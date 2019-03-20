package net.flyingff.stockdisplayer

import org.json.JSONObject
import java.lang.AssertionError
import java.util.*
import java.util.regex.Pattern

data class StockData (
    val date : Date,
    val open : Float,
    val close : Float,
    val max : Float,
    val min : Float,
    val volume : Float,
    var prev : StockData? = null
) {
    companion object {
        fun fromJSON(json : JSONObject) : StockData {
            val day = json.getString("day").toDate() ?:
                throw AssertionError("Date malformed: ${json.getString("day")}")
            val open = json.getString("open").toFloat()
            val high = json.getString("high").toFloat()
            val low = json.getString("low").toFloat()
            val close = json.getString("close").toFloat()
            val vol = json.getString("volume").toFloat()
            return StockData(day, open, close, high, low, vol)
        }
        private fun String.toDate() : Date? {
            val s = trim()
            val matcher = PATTERN.matcher(s)
            if (!matcher.matches()) {
                return null
            }
            val c = Calendar.getInstance()
            c.set(
                matcher.group(1).toInt(),
                matcher.group(2).toInt(),
                matcher.group(3).toInt(),
                matcher.group(4).toInt(),
                matcher.group(5).toInt(),
                matcher.group(6).toInt()
            )
            c.set(Calendar.MILLISECOND, 0)
            return c.time
        }
        private val PATTERN = Pattern.compile("([0-9]{4})-([0-9]{1,2})-([0-9]{1,2}) ([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2})")
    }
    override fun toString() =
        "$date : [$open -> $max -> $min -> $close] $volume"
}