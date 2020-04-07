package cn.yhsb.cjb.application

import cn.yhsb.base.CommandWithHelp
import cn.yhsb.cjb.request.GrinfoRequest
import cn.yhsb.cjb.service.Session
import picocli.CommandLine

@CommandLine.Command(description = ["城居保信息查询程序"])
class Query : CommandWithHelp() {
    override fun run() {
        CommandLine.usage(Query(), System.out)
    }
}

@CommandLine.Command(name = "grinfo", description = ["个人综合查询"])
class GrinfoQuery : CommandWithHelp() {
    @CommandLine.Parameters(description = ["身份证号码"])
    var idcards: Array<String> = arrayOf()

    override fun run() {
        Session.autoLogin {
            for (idcard in idcards) {
                it.sendService(GrinfoRequest(idcard))
                val res = it.getResult<GrinfoRequest.Grinfo>()
                if (res.empty()) {
                    println("$idcard 未在我区参保")
                } else {
                    val info = res[0]
                    println("${info.idcard} ${info.name} ${info.jbState} " +
                            "${info.dwmc} ${info.czmc}")
                }
            }
        }
    }
}

@CommandLine.Command(name = "jfxx", description = ["缴费信息查询"])
class JfxxQuery : CommandWithHelp() {
    @CommandLine.Option(names = ["-e", "--export"], description = ["导出信息表"])
    var export = false

    @CommandLine.Parameters(description = ["身份证号码"])
    var idcard: String? = null

    override fun run() {
        TODO("Not yet implemented")
    }

}

fun main(args: Array<String>) {
    CommandLine(Query())
            .addSubcommand(GrinfoQuery())
            .addSubcommand(JfxxQuery())
            .execute(*args)
}