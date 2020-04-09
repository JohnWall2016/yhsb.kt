package cn.yhsb.cjb.application

import cn.yhsb.base.CommandWithHelp
import cn.yhsb.base.DateTime
import cn.yhsb.cjb.request.CbshRequest
import cn.yhsb.cjb.service.Session
import picocli.CommandLine

@CommandLine.Command(description = ["特殊参保人员身份信息变更导出程序"])
class Audit : CommandWithHelp() {
    @CommandLine.Parameters(index = "0", description = ["审核开始时间, 例如: 20200301"])
    var startDate: String? = null

    @CommandLine.Parameters(index = "1", defaultValue = "", description = ["审核结束时间, 例如: 20200310"])
    var endDate: String? = null

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
        for ((i, d) in result.withIndex()) {
            println("$i ${d.idcard} ${d.name} ${d.birthDay}")
        }
    }
}

fun main(args: Array<String>) {
    CommandLine(Audit()).execute(*args)
}