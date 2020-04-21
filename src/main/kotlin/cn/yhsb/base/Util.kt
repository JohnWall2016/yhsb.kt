package cn.yhsb.base

import picocli.CommandLine
import java.lang.StringBuilder
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.reflect.full.createType


@CommandLine.Command(mixinStandardHelpOptions = true)
abstract class CommandWithHelp : Runnable

object DateTime {
    fun toDashedDate(date: String,
                     format: String = """^(\d\d\d\d)(\d\d)(\d\d)$"""): String {
        val m = Regex(format).find(date)
        if (m != null) {
            return m.groupValues.drop(1).joinToString("-")
        }
        throw IllegalArgumentException("invalid date format")
    }
}

data class SpecialChars(val range: CharRange, val width: Int)

val chineseChars = SpecialChars(CharRange('\u4e00', '\u9fa5'), 2)

fun padCount(s: String, width: Int, specialChars: Array<out SpecialChars>): Int {
    var n = 0
    out@for (c in s) {
        for ((r, w) in specialChars) {
            if (c in r) {
                n += w
                continue@out
            }
        }
        n += 1
    }
    return width - n
}

private fun Char.times(n: Int, b: StringBuilder) {
    for (i in 1..n) {
        b.append(this)
    }
}

fun String.padLeft(width: Int, padChar: Char = ' ',
                   vararg specialChars: SpecialChars = arrayOf(chineseChars)): String {
    val count = padCount(this, width, specialChars)
    return if (count > 0) {
        val b = StringBuilder()
        padChar.times(count, b)
        b.append(this)
        b.toString()
    } else {
        this
    }
}

fun String.padRight(width: Int, padChar: Char = ' ',
                  vararg specialChars: SpecialChars = arrayOf(chineseChars)): String {
    val count = padCount(this, width, specialChars)
    return if (count > 0) {
        val b = StringBuilder()
        b.append(this)
        padChar.times(count, b)
        b.toString()
    } else {
        this
    }
}

open class CustomField {
    var value: String? = null

    open val name: String
        get() = "未知值: $value"

    override fun toString(): String {
        return name
    }

    companion object {
        val type = CustomField::class.createType()
    }
}

object ChineseNumber {
    private val numbers = arrayOf("零", "壹", "贰", "叁", "肆",
            "伍", "陆", "柒", "捌", "玖")
    private val places = arrayOf("", "拾", "佰", "仟", "万", "亿")
    private val units = arrayOf("元", "角", "分")
    private const val whole = "整"

    private val TEN = BigInteger("10")
    private val HUNDRED = BigInteger("100")
    private val ZERO = BigInteger.ZERO

    fun BigDecimal.toChineseMoney(): String {
        val value = (this.setScale(2, RoundingMode.HALF_UP)
                * BigDecimal("100")).toBigIntegerExact()
        var integer = value / HUNDRED
        val fraction = value % HUNDRED

        val length = integer.toString().length
        var result = ""
        var zero = false
        for (i in length downTo 0) {
            val base = TEN.pow(i)
            val quotient = integer / base
            if (quotient > ZERO) {
                if (zero) result += numbers[0]
                result += numbers[quotient.toInt()] + places[i % 4]
                zero = false
            } else if (quotient == ZERO && result != "") {
                zero = true
            }
            if (i >= 4) {
                if (i % 8 == 0 && result != "") {
                    result += places[5]
                } else if (i % 4 == 0 && result != "") {
                    result += places[4]
                }
            }
            integer %= base
            if (integer == ZERO && i != 0) {
                zero = true
                break
            }
        }
        result += units[0]

        if (fraction == ZERO) {
            result += whole
        } else {
            val quotient = fraction / TEN
            val remainder = fraction % TEN
            if (remainder == ZERO) {
                if (zero) result += numbers[0]
                result += numbers[quotient.toInt()] + units[1] + whole
            } else {
                if (zero || quotient == ZERO) {
                    result += numbers[0]
                }
                if (quotient != ZERO) {
                    result += numbers[quotient.toInt()] + units[1]
                }
                result += numbers[remainder.toInt()] + units[2]
            }
        }
        return result
    }
}