package cn.yhsb.cjb.application

import cn.yhsb.base.*
import cn.yhsb.cjb.request.CbxxRequest
import cn.yhsb.cjb.request.CbxxRequest.Cbxx
import cn.yhsb.cjb.request.GrinfoRequest
import cn.yhsb.cjb.request.GrinfoRequest.Grinfo
import cn.yhsb.cjb.request.JfxxRequest
import cn.yhsb.cjb.request.JfxxRequest.Jfxx
import cn.yhsb.cjb.service.Result
import cn.yhsb.cjb.service.Session
import picocli.CommandLine
import java.lang.String.format
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.nio.file.Paths


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
                sendService(GrinfoRequest(idcard))
                val res = getResult<Grinfo>()
                if (res.isEmpty()) {
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

    /** 缴费记录  */
    open class JfxxRecord {
        /** 年度  */
        var year: Int? = null

        /** 个人缴费  */
        var grjf: BigDecimal = ZERO

        /** 省级补贴  */
        var sjbt: BigDecimal = ZERO

        /** 市级补贴  */
        var sqbt: BigDecimal = ZERO

        /** 县级补贴  */
        var xjbt: BigDecimal = ZERO

        /** 政府代缴  */
        var zfdj: BigDecimal = ZERO

        /** 集体补助  */
        var jtbz: BigDecimal = ZERO

        /** 划拨日期  */
        var hbrq = mutableSetOf<String>()

        /** 社保机构  */
        var sbjg = mutableSetOf<String>()

        constructor(year: Int) {
            this.year = year
        }

        constructor()
    }

    /** 缴费合计记录  */
    class JfxxTotalRecord : JfxxRecord() {
        /** 合计  */
        var total: BigDecimal = ZERO
    }

    fun getJfxxRecords(result: Result<Jfxx>,
                       paidRecords: MutableMap<Int, JfxxRecord>,
                       unpaidRecords: MutableMap<Int, JfxxRecord>) {
        for (data in result) {
            val year = data.year
            if (year != null) {
                val records = if (data.paidOff()) paidRecords else unpaidRecords
                var record = records[year]
                if (record == null) {
                    record = JfxxRecord(year)
                    records[year] = record
                }
                val amount = data.amount ?: ZERO
                when (val type = data.item?.value) {
                    "1" -> record.grjf += amount
                    "3" -> record.sjbt += amount
                    "4" -> record.sqbt += amount
                    "5" -> record.xjbt += amount
                    "6" -> record.jtbz += amount
                    "11" -> record.zfdj += amount
                    else -> throw Exception("未知缴费类型$type, 金额$amount")
                }
                record.sbjg.add(data.agency ?: "")
                record.hbrq.add(data.paidOffDay ?: "")
            }
        }
    }

    fun orderAndSum(records: Map<Int, JfxxRecord>): List<JfxxRecord> {
        val results = records.values.sortedBy { it.year }.toMutableList()
        val total = results.fold(JfxxTotalRecord()) { acc, n ->
            acc.grjf += n.grjf
            acc.sjbt += n.sjbt
            acc.sqbt += n.sqbt
            acc.xjbt += n.xjbt
            acc.zfdj += n.zfdj
            acc.jtbz += n.jtbz
            acc
        }
        total.total = total.grjf + total.sjbt + total.sqbt +
                total.xjbt + total.zfdj + total.jtbz
        results.add(total)
        return results
    }

    fun printInfo(info: Cbxx) {
        println("个人信息:")
        println(format("%s %s %s %s %s %s %s", info.name, info.idcard,
                info.jbState, info.jbKind, info.agency,
                info.czName, info.dealDate))
    }

    fun formatRecord(r: JfxxRecord): String? {
        return if (r !is JfxxTotalRecord) {
            format("%5s%9s%9s%9s%9s%9s%9s  %s %s", r.year,
                    r.grjf, r.sjbt, r.sqbt, r.xjbt, r.zfdj, r.jtbz,
                    r.sbjg.joinToString("|"), r.hbrq.joinToString("|"))
        } else {
            " 合计" + format("%9s%9s%9s%9s%9s%9s", r.grjf, r.sjbt,
                    r.sqbt, r.xjbt, r.zfdj, r.jtbz) + "  总计: " + r.total
        }
    }

    fun printJfxxRecords(records: List<JfxxRecord>, message: String) {
        println(message)
        println(format("%2s%3s%6s%5s%5s%5s%5s%5s%7s %s", "序号", "年度", "个人缴费",
                "省级补贴", "市级补贴", "县级补贴", "政府代缴", "集体补助", "社保经办机构", "划拨时间"))
        var i = 1
        for (r in records) {
            val t = if (r is JfxxTotalRecord) "" else "${i++}"
            println(format("%3s %s", t, formatRecord(r)))
        }
    }

    override fun run() {
        val idcard = idcard ?: return

        val (info, jfxx) = Session.autoLogin {
            var info: Cbxx? = null
            var jfxx: Result<Jfxx>? = null

            sendService(CbxxRequest(idcard))
            val infoRes = getResult<Cbxx>()
            if (infoRes.isEmpty() || infoRes[0].invalid())
                return@autoLogin Pair(info, jfxx)
            info = infoRes[0]

            sendService(JfxxRequest(idcard))
            val jfxxRes = getResult<Jfxx>()
            if (jfxxRes.isNotEmpty() && jfxxRes[0].year != null)
                jfxx = jfxxRes
            Pair(info, jfxx)
        }

        if (info == null) {
            println("未查到参保记录")
            return
        }

        printInfo(info)

        var records: List<JfxxRecord>? = null
        val unrecords: List<JfxxRecord>?

        if (jfxx == null) {
            println("未查询到缴费信息")
        } else {
            val paidRecords = mutableMapOf<Int, JfxxRecord>()
            val unpaidRecords = mutableMapOf<Int, JfxxRecord>()
            getJfxxRecords(jfxx, paidRecords, unpaidRecords)
            records = orderAndSum(paidRecords)
            unrecords = orderAndSum(unpaidRecords)
            printJfxxRecords(records, "\n已拨付缴费历史记录:")
            if (unpaidRecords.isNotEmpty()) {
                printJfxxRecords(unrecords, "\n未拨付补录入记录:")
            }
        }

        if (export) {
            val dir = "D:\\征缴管理"
            val xlsx = Paths.get(dir, "雨湖区城乡居民基本养老保险缴费查询单模板.xlsx")
            val workbook = Excels.load(xlsx.toString())
            val sheet = workbook.getSheetAt(0).apply {
                cell("A5").setValue(info.name)
                cell("C5").setValue(info.idcard)
                cell("E5").setValue(info.agency)
                cell("G5").setValue(info.czName)
                cell("K5").setValue(info.dealDate)
            }

            if (records != null) {
                var index = 8
                val copyIndex = index
                for (r in records) {
                    sheet.getOrCopyRowFrom(index++, copyIndex, true).apply {
                        if (r is JfxxTotalRecord) {
                            cell("A").setValue("")
                            cell("B").setValue("合计")
                        } else {
                            cell("A").setValue("${index - copyIndex}")
                            cell("B").setValue(r.year)
                        }
                        cell("C").setValue(r.grjf)
                        cell("D").setValue(r.sjbt)
                        cell("E").setValue(r.sqbt)
                        cell("F").setValue(r.xjbt)
                        cell("G").setValue(r.zfdj)
                        cell("H").setValue(r.jtbz)
                        if (r is JfxxTotalRecord) {
                            cell("I").setValue("总计")
                            cell("K").setValue(r.total)
                        } else {
                            cell("I").setValue(r.sbjg.joinToString("|"))
                            cell("K").setValue(r.hbrq.joinToString("|"))
                        }
                    }
                }
            }
            workbook.save(Paths.get(dir, info.name + "缴费查询单.xlsx"))
        }
    }

}

fun main(args: Array<String>) {
    CommandLine(Query())
            .addSubcommand(GrinfoQuery())
            .addSubcommand(JfxxQuery())
            .execute(*args)
}