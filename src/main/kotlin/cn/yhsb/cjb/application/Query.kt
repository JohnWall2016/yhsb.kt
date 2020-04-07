package cn.yhsb.cjb.application

import cn.yhsb.base.CommandWithHelp
import cn.yhsb.base.Excels
import cn.yhsb.base.getOrCopyRowFrom
import cn.yhsb.base.save
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
                it.sendService(GrinfoRequest(idcard))
                val res = it.getResult<Grinfo>()
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
        val results = records.values.sortedBy { it.year }
        val list = mutableListOf<JfxxRecord>()
        val total = JfxxTotalRecord()
        results.forEach { r: JfxxRecord ->
            list.add(r)
            total.grjf += r.grjf
            total.sjbt += r.sjbt
            total.sqbt += r.sqbt
            total.xjbt += r.xjbt
            total.zfdj += r.zfdj
            total.jtbz += r.jtbz
        }
        total.total = total.grjf + total.sjbt + total.sqbt +
                total.xjbt + total.zfdj + total.jtbz
        list.add(total)
        return list
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

        var info: Cbxx? = null
        var jfxx: Result<Jfxx>? = null

        Session.autoLogin { session ->
            session.sendService(CbxxRequest(idcard))
            val infoRes = session.getResult<Cbxx>()
            if (infoRes.empty() || !infoRes[0].valid())
                return@autoLogin
            info = infoRes[0]

            session.sendService(JfxxRequest(idcard))
            val jfxxRes = session.getResult<Jfxx>()
            if (!jfxxRes.empty() && jfxxRes[0].year != null)
                jfxx = jfxxRes
        }

        if (info == null) {
            println("未查到参保记录")
            return
        }

        printInfo(info!!)

        var records: List<JfxxRecord>? = null
        val unrecords: List<JfxxRecord>?

        if (jfxx == null) {
            println("未查询到缴费信息")
        } else {
            val paidRecords = mutableMapOf<Int, JfxxRecord>()
            val unpaidRecords = mutableMapOf<Int, JfxxRecord>()
            getJfxxRecords(jfxx!!, paidRecords, unpaidRecords)
            records = orderAndSum(paidRecords)
            unrecords = orderAndSum(unpaidRecords)
            printJfxxRecords(records, "已拨付缴费历史记录:")
            if (unpaidRecords.isNotEmpty()) {
                printJfxxRecords(unrecords, "\n未拨付补录入记录:")
            }
        }

        if (export) {
            val dir = "D:\\征缴管理"
            val xlsx = Paths.get(dir, "雨湖区城乡居民基本养老保险缴费查询单模板.xlsx")
            val workbook = Excels.load(xlsx.toString())
            val sheet = workbook.getSheetAt(0)
            sheet.getRow(4).getCell(0).setCellValue(info?.name)
            sheet.getRow(4).getCell(2).setCellValue(info?.idcard)
            sheet.getRow(4).getCell(4).setCellValue(info?.agency)
            sheet.getRow(4).getCell(6).setCellValue(info?.czName)
            sheet.getRow(4).getCell(10).setCellValue(info?.dealDate)

            if (records != null) {
                var index = 8
                val copyIndex = index
                for (r in records) {
                    val row = sheet.getOrCopyRowFrom(index++, copyIndex, true)
                    if (r is JfxxTotalRecord) {
                        row.getCell(0).setCellValue("")
                        row.getCell(1).setCellValue("合计")
                    } else {
                        row.getCell(0).setCellValue("${index - copyIndex}")
                        row.getCell(1).setCellValue(r.year?.toString() ?: "")
                    }
                    row.getCell(2).setCellValue(r.grjf.toString())
                    row.getCell(3).setCellValue(r.sjbt.toString())
                    row.getCell(4).setCellValue(r.sqbt.toString())
                    row.getCell(5).setCellValue(r.xjbt.toString())
                    row.getCell(6).setCellValue(r.zfdj.toString())
                    row.getCell(7).setCellValue(r.jtbz.toString())
                    if (r is JfxxTotalRecord) {
                        row.getCell(8).setCellValue("总计")
                        row.getCell(10).setCellValue(r.total.toString())
                    } else {
                        row.getCell(8)
                                .setCellValue(r.sbjg.joinToString("|"))
                        row.getCell(10)
                                .setCellValue(r.hbrq.joinToString("|"))
                    }
                }
            }
            workbook.save(Paths.get(dir, info?.name + "缴费查询单.xlsx").toString())
        }
    }

}

fun main(args: Array<String>) {
    CommandLine(Query())
            .addSubcommand(GrinfoQuery())
            .addSubcommand(JfxxQuery())
            .execute(*args)
}