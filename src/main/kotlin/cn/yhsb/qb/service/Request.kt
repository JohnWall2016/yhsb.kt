package cn.yhsb.qb.service

import cn.yhsb.base.CustomField
import cn.yhsb.cjb.service.Jsonable

const val NSOut = "http://www.molss.gov.cn/"
const val NSSoapEnvelope = "http://schemas.xmlsoap.org/soap/envelope/"
const val NSIn = "http://www.molss.gov.cn/"

/////////////////////////////////////////////////////////////////

class InEnvelop(header: Parameter, body: Parameter) {
    @Property("soap:encodingStyle", NSSoapEnvelope)
    val encodingStyle = "http://schemas.xmlsoap.org/soap/encoding/"

    @Tag("soap:Header", NSSoapEnvelope)
    var header = InHeader(header)

    @Tag("soap:Body", NSSoapEnvelope)
    var body = InBody(body)
}

class InHeader(system: Parameter) {
    @Tag("in:system", NSIn)
    var system = system
}

class InBody(business: Parameter) {
    @Tag("in:business", NSIn)
    var business = business
}

//////////////////////////////////////////////////////////////

class OutEnvelope<T : Result, S : Result>(
        headerInit: () -> T,
        businessInit: () -> S) {
    @Tag("soap:Header", NSSoapEnvelope)
    var header = headerInit()

    @Tag("soap:Body", NSSoapEnvelope)
    var body = OutBody(businessInit)

    val result: S get() = body.business
}

class OutBody<T : Result>(businessInit: () -> T) {
    @Tag("out:business", NSOut)
    var business = businessInit()
}

//////////////////////////////////////////////////////////////

class InSystem : Parameter() {
    @Property("usr")
    var user = ""

    @Property("pwd")
    var password = ""

    @Property("funid")
    var funID = ""
}

open class ParameterWithFunID(funID: String) : Parameter() {
    @Ignore
    val funID: String = funID
}

open class CustomResult : Result()

class Login : ParameterWithFunID(
        "F00.00.00.00|192.168.1.110|PC-20170427DGON|00-05-0F-08-1A-34") {
    class Result : CustomResult() {
        @Property("operator_name")
        var name = ""

        @Property("usr")
        var user = ""

        @Property("login_name")
        var loginName = ""

        @Property("sab090")
        var agencyName = ""

        @Property("grbhqz")
        var agencyCode = ""

        class ID {
            var id = ""
        }

        var acl: ResultSet<ID> = ResultSet()
    }
}

class OutHeader : Result() {
    var sessionID = ""
    var message = ""
}

open class ClientSql(funID: String, functionID: String, sql: String = "")
    : ParameterWithFunID(funID) {
    @Property("startrow")
    var startRow = "1"

    @Property("row_count")
    var rowCount = "-1"

    @Property("pagesize")
    var pageSize = "500"

    @Property("clientsql")
    var clientSql = sql

    @Property("functionid")
    var functionID = functionID

    open class Result : CustomResult() {
        var result = ""

        @Property("row_count")
        var rowCount = ""

        @Property("querysql")
        var querySql = ""
    }

    open class QueryList<T> : Result() {
        @Property("querylist")
        var queryList: ResultSet<T> = ResultSet()
    }
}

class SncbryQuery(idcard: String)
    : ClientSql(
        "F00.01.03",
        "F27.06",
        "( aac002 = &apos;$idcard&apos;)") {
    class Result : ClientSql.QueryList<Sncbry>()
}

/** 社会保险状态 */
class SBState : CustomField() {
    override val name: String
        get() = when (value) {
            "1" -> "在职"
            "2" -> "退休"
            "4" -> "终止"
            else -> "未知值: $value"
        }
}

/** 参保状态 */
class CBState : CustomField() {
    override val name: String
        get() = when (value) {
            "1" -> "参保缴费"
            "2" -> "暂停缴费"
            "3" -> "终止缴费"
            else -> "未知值: $value"
        }
}

/** 缴费人员类别 */
class JFKind : CustomField() {
    override val name: String
        get() = when (value) {
            "102" -> "个体缴费"
            "101" -> "单位在业人员"
            else -> "未知值: $value"
        }
}

class Sncbry : Jsonable() {
    var rown = ""

    /** 个人编号 */
    @Property("sac100")
    var grbh = ""

    @Property("aac003")
    var name = ""

    @Property("aac002")
    var idcard = ""

    @Property("aac008")
    var sbState = SBState()

    @Property("sac007")
    var jfKind = JFKind()

    @Property("aac031")
    var cbState = CBState()

    /** 单位编号 */
    @Property("sab100")
    var dwbh = ""

    @Property("aab300")
    var agency = ""
}