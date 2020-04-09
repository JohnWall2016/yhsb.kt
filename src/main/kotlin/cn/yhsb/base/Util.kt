package cn.yhsb.base

import picocli.CommandLine


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