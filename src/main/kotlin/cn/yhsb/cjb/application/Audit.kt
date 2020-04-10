package cn.yhsb.cjb.application

import cn.yhsb.base.*
import cn.yhsb.cjb.database.FPHistoryData as FP
import cn.yhsb.cjb.database.transaction
import cn.yhsb.cjb.request.CbshRequest
import cn.yhsb.cjb.request.jbKindMap
import cn.yhsb.cjb.service.Session
import org.jetbrains.exposed.sql.select
import picocli.CommandLine
import java.lang.String.format
import java.nio.file.Paths

// typealias FP = FPHistoryData

@CommandLine.Command(description = ["特殊参保人员身份信息变更导出程序"])
class Audit : CommandWithHelp() {
    @CommandLine.Option(names = ["-e", "--export"], description = ["导出信息表"])
    var export = false

    @CommandLine.Parameters(index = "0", description = ["审核开始时间, 例如: 20200301"])
    var startDate: String? = null

    @CommandLine.Parameters(index = "1", defaultValue = "", description = ["审核结束时间, 例如: 20200310"])
    var endDate: String? = null

    val dir = """D:\精准扶贫\"""
    val xlsx = "批量信息变更模板.xls"

    override fun run() {
        val start = if (startDate.isNullOrEmpty()) "" else DateTime.toDashedDate(startDate!!)
        val end = if (endDate.isNullOrEmpty()) "" else DateTime.toDashedDate(endDate!!)
        var span = ""
        if (start.isNotEmpty())
            span += start
        if (end.isNotEmpty())
            span += "_$end"
        println(span)

        val result = Session.autoLogin { session ->
            session.sendService(CbshRequest(start, end))
            return@autoLogin session.getResult<CbshRequest.Cbsh>()
        }

        println("共计 ${result.size} 条")
        if (result.isNotEmpty()) {
            val workbook = if (export) Excels.load(Paths.get(dir, xlsx)) else null
            val sheet = workbook?.getSheetAt(0)
            var index = 1
            val copyIndex = 1
            var save = false
            transaction {
                for ((i, d) in result.withIndex()) {
                    val rs = FP.select { FP.idcard eq d.idcard!! }.firstOrNull()
                    if (rs != null) {
                        println(format("%4d %s %s %s", i+1, d.idcard, d.name?.padRight(6), d.birthDay) +
                                " ${rs[FP.jbrdsf]} ${if (d.name != rs[FP.name]) rs[FP.name] else ""}")
                        if (export) {
                            val row = sheet?.getOrCopyRowFrom(index++, copyIndex, false)
                            row?.cell("B")?.setValue(d.idcard)
                            row?.cell("E")?.setValue(d.name)
                            row?.cell("J")?.setValue(jbKindMap[rs[FP.jbrdsf]])
                            save = true
                        }
                    } else {
                        println(format("%4d %s %s %s", i+1, d.idcard, d.name?.padRight(6), d.birthDay))
                    }
                }
            }
            if (save) {
                println("导出 批量信息变更$span.xls")
                workbook?.save(Paths.get(dir, "批量信息变更$span.xls"))
            }
            println("Done")
        }
    }
}

fun main(args: Array<String>) {
    CommandLine(Audit()).execute(*args)
}