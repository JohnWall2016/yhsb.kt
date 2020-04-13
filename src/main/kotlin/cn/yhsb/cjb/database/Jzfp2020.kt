package cn.yhsb.cjb.database

import cn.yhsb.cjb.Config
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

/** 扶贫数据表 */
open class FPData(name: String) : Table(name) {
    /** 序号 */
    val id = integer("NO").primaryKey().autoIncrement()

    /** 乡镇街 */
    val xzj = text("Xzj")

    /** 村社区 */
    val csq = text("Csq")

    /** 地址 */
    val address = text("Address")

    /** 姓名 */
    val name = text("Name")

    /** 身份证号码 */
    val idcard = text("Idcard")

    /** 出生日期 */
    val birthDay = text("BirthDay")

    /** 贫困人口 */
    val pkrk = text("Pkrk")

    /** 贫困人口日期 */
    val pkrkDate = text("PkrkDate")

    /** 特困人员 */
    val tkry = text("Tkry")

    /** 特困人员日期 */
    val tkryDate = text("TkryDate")

    /** 全额低保人员 */
    val qedb = text("Qedb")

    /** 全额低保人员日期 */
    val qedbDate = text("QedbDate")

    /** 差额低保人员 */
    val cedb = text("Cedb")

    /** 差额低保人员日期 */
    val cedbDate = text("CedbDate")

    /** 一二级残疾人员 */
    val yejc = text("Yejc")

    /** 一二级残疾人员日期 */
    val yejcDate = text("YejcDate")

    /** 三四级残疾人员 */
    val ssjc = text("Ssjc")

    /** 三四级残疾人员日期 */
    val ssjcDate = text("SsjcDate")

    /** 属于贫困人员 */
    val sypkry = text("Sypkry")

    /** 居保认定身份 */
    val jbrdsf = text("Jbrdsf")

    /** 居保认定身份最初日期 */
    val jbrdsfFirstDate = text("JbrdsfFirstDate")

    /** 居保认定身份最后日期 */
    val jbrdsfLastDate = text("JbrdsfLastDate")

    /** 居保参保情况 */
    val jbcbqk = text("Jbcbqk")

    /** 居保参保情况日期 */
    val jbcbqkDate = text("JbcbqkDate")
}

/** 扶贫历史数据 */
object FPHistoryData : FPData("FpHistoryData")

/** 扶贫月度数据 */
object FPMonthData : FPData("FpMonthData") {
    /** 年月 201912 */
    val month = text("Month")
}

/** 扶贫原始数据表 */
object FPRawData : Table("FpRawData") {
    /** 序号 */
    val id = integer("NO").entityId().autoIncrement()

    /** 乡镇街 */
    val xzj = text("Xzj")

    /** 村社区 */
    val csq = text("Csq")

    /** 地址 */
    val address = text("Address")

    /** 姓名 */
    val name = text("Name")

    /** 身份证号码 */
    val idcard = text("Idcard")

    /** 出生日期 */
    val birthDay = text("BirthDay")

    /** 人员类型 */
    val type = text("Type")

    /** 类型细节 */
    val detail = text("Detail")

    /** 数据月份 */
    val date = text("Date")
}

/** 居保参保人员明细表 */
object Jbrymx : Table("Jbrymx") {
    /** 身份证号码 */
    val idcard = varchar("Idcard", 18).entityId()

    /** 行政区划 */
    val xzqh = text("Xzqh")

    /** 户籍性质 */
    val hjxz = text("Hjxz")

    /** 姓名 */
    val name = text("Name")

    /** 性别 */
    val sex = text("Sex")

    /** 出生日期 */
    val birthDay = text("BirthDay")

    /** 参保身份 */
    val cbsf = text("Cbsf")

    /** 参保状态 */
    val cbzt = text("Cbzt")

    /** 缴费状态 */
    val jfzt = text("Jfzt")

    /** 参保时间 */
    val cbsj = text("Cbsj")
}

private val jzfp2020 by lazy {
    Database.connect(Config.Database.url,
            driver = Config.Database.driver,
            user = Config.Database.user.id,
            password = Config.Database.user.password)
}

fun <T> jzfp2020Transaction(statement: Transaction.()->T): T {
    return transaction(jzfp2020, statement)
}
