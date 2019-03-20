package net.flyingff.stockdisplayer

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.os.Build
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class DataService : IntentService("DataService") {

    override fun onHandleIntent(intent: Intent?) {

        when (intent?.action) {
            ACTION_UPDATE -> {
                createNotification()
                handleUpdate()
                stopForeground(true)
            }
            ACTION_CANCEL -> {
                stopSelf()
                stopForeground(true)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun createNotification() {
        val notificationIntent = Intent(this, DataService::class.java)
        notificationIntent.action = ACTION_CANCEL
        val pendingIntent = PendingIntent.getService(this, 255,
            notificationIntent, PendingIntent.FLAG_ONE_SHOT)

        val notify = (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            if (null == manager.getNotificationChannel(CH_ID)) {
                val notificationChannel = NotificationChannel(CH_ID,
                    "Ch0", NotificationManager.IMPORTANCE_HIGH)
                notificationChannel.enableLights(false)
                notificationChannel.setShowBadge(true)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                manager.createNotificationChannel(notificationChannel)
            }
            Notification.Builder(this, CH_ID)
        } else {
            Notification.Builder(this)
        }).setSmallIcon(Icon.createWithResource(this, R.drawable.sync))
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_CONTENT)
            .setContentIntent(pendingIntent)
            .build()
        notify.flags = notify.flags.or(Notification.FLAG_NO_CLEAR)
        startForeground(1, notify)
    }

    private fun handleUpdate() {
        val client = OkHttpClient()
        val req = Request.Builder()
            .url(SZ_URL)
            .build()
        val retText = client.newCall(req).execute().use {
            it.body()?.string()
        }
        if (retText != null) {
            val array = JSONArray(retText)
            val dataList = (0 until array.length())
                .map { StockData.fromJSON(array.getJSONObject(it)) }
                .toMutableList()
            for (i in 1 until dataList.size) {
                dataList[i].prev = dataList[i - 1]
            }
            dataList.removeAt(0)
            // keep only 4 day
            val sortedDate = dataList.map { cmpFormat.format(it.date) }.toSortedSet()
            if (sortedDate.size > 4) {
                val min = sortedDate.min()
                val itr = dataList.iterator()
                while (itr.hasNext()) {
                    if (cmpFormat.format(itr.next().date) == min) {
                        itr.remove()
                    }
                }
            }
            updateWidgets(paint(dataList))
        }
    }

    private val paint = Paint().apply {
        color = 0x1f3f51b5
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val paintVol = Paint().apply {
        color = 0x3f3f51b5
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val paintUp = Paint().apply {
        color = 0xafdb5425.toInt()
        style = Paint.Style.FILL
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val paintDown = Paint().apply {
        color = 0xaf13bf2b.toInt()
        style = Paint.Style.FILL
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val paintText = Paint().apply {
        color = 0xaf0318af.toInt()
        style = Paint.Style.FILL
        textSize = 26f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val paintTextSmall = Paint().apply {
        color = 0x7f0318af
        style = Paint.Style.FILL
        textSize = 16f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }
    private val textSmallColorUp = 0xafa8401c.toInt()
    private val textSmallColorDown = 0xaf0e881f.toInt()

    private val cmpFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    private val format = SimpleDateFormat("MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("MM-dd hh:mm", Locale.getDefault())
    private fun paint(dataList : List<StockData>) : Bitmap {
        val w = 640
        val h = 380
        val marginTop = 32f
        val marginBottom = 48f
        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)

        // background
        canvas.drawRoundRect(0f, 0f, w - 1f, h - 1f, 8f, 8f, paint)

        // k graph
        val min = dataList.map { it.min }.min()!!
        val max = dataList.map { it.max }.max()!!
        val maxV = dataList.map { it.volume }.max()!!

        val toY = { v : Float ->
            (h - marginBottom) - ((v - min) * (h - marginTop - marginBottom) / (max - min))
        }

        val len = DATA_COUNT + 2
        val blockW = w / len

        val dayMin = HashMap<String, Pair<StockData, Float>>()
        val dayMax = HashMap<String, Pair<StockData, Float>>()
        dataList.forEachIndexed { index, stockData ->
            val x = (index + 1f) * w / len
            val halfX = x + (blockW - 0.5f) / 2f
            val minY = toY(stockData.min)
            val maxY = toY(stockData.max)
            val openY = toY(stockData.open)
            val closeY = toY(stockData.close)

            val day = cmpFormat.format(stockData.date)
            if ((dayMin[day]?.second ?: Float.MAX_VALUE) > x) {
                dayMin[day] = Pair(stockData, x)
            }
            if ((dayMax[day]?.second ?: Float.MIN_VALUE) < x + blockW) {
                dayMax[day] = Pair(stockData, x + blockW)
            }

            // volume
            canvas.drawRect(x - 1, h - 1f, x + blockW,
                h - 1f - (stockData.volume * h * 0.3f / maxV), paintVol)

            val paint = if (stockData.open > stockData.close) paintDown else paintUp
            canvas.drawRect(x, Math.min(openY, closeY), x + blockW - 1, Math.max(openY, closeY), paint)
            canvas.drawLine(halfX, maxY, halfX, minY, paint)
        }
        // upper text
        val current = dataList.last()
        canvas.drawText("上证指数 [${current.close}] @ ${timeFormat.format(current.date)}", w / 2f, 26f, paintText)
        // lower text, date and percent
        for (day in dayMin.keys.sorted()) {
            val (minStock, minX) = dayMin[day]!!
            val maxStock = dayMax[day]!!.first
            val maxX = minX + 16 * w / len
            // splitter
            canvas.drawRect(minX - 2, marginTop, minX, h - 1f, paint)
            canvas.drawRect(maxX - 2, marginTop, maxX, h - 1f, paint)
            // texts
            val base = minStock.prev!!.close
            val abs = maxStock.close - base
            val percent = String.format("%+.02f", (abs * 100 / base))

            val originColor = paintTextSmall.color
            paintTextSmall.color = if (abs > 0) textSmallColorUp else textSmallColorDown
            canvas.drawText("$day $percent%", (minX + maxX) / 2, h - 4f, paintTextSmall)
            canvas.drawText(String.format("%+.02f", abs), (minX + maxX) / 2, h - 20f, paintTextSmall)
            paintTextSmall.color = originColor
        }
        // maximum and minimum text
        paintTextSmall.textAlign = Paint.Align.LEFT
        canvas.drawText(String.format("%.2f", max), 2f, 32f, paintTextSmall)
        canvas.drawText(String.format("%.2f", min), 2f, h - 32f, paintTextSmall)
        paintTextSmall.textAlign = Paint.Align.CENTER

        return bm
    }
    private fun updateWidgets(bitmap: Bitmap) {
        val wManager = getSystemService(AppWidgetManager::class.java)
        for (id in wManager.getAppWidgetIds(ComponentName.createRelative(
            this, HomeWidget::class.java.name))) {
            HomeWidget.updateAppWidget(this, wManager, id, bitmap)
        }
    }
    companion object {
        private const val DATA_COUNT = 64
        private const val ACTION_UPDATE = "net.flyingff.stockdisplayer.action.UPDATE"
        private const val ACTION_CANCEL = "net.flyingff.stockdisplayer.action.CANCEL"

        private const val SZ_URL = "https://money.finance.sina.com.cn/quotes_service/api/json_v2.php/" +
                "CN_MarketData.getKLineData?symbol=sh000001&scale=15&ma=no&datalen=${DATA_COUNT + 1}"

        internal const val CH_ID = "net.flyingff.stockdisplayer.channel"
        internal const val NOTIFICATION_TITLE = "Stock Display"
        internal const val NOTIFICATION_CONTENT = "Sync in progress..."

        @JvmStatic
        fun getUpdate(context: Context) : PendingIntent {
            val intent = Intent(context, DataService::class.java).apply {
                action = ACTION_UPDATE
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(context, 0, intent, 0)
            } else {
                PendingIntent.getService(context, 0, intent, 0)
            }
        }
    }
}
