package cn.yhsb.qb.service

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
        headerInit: () -> T, businessInit: () -> T) {
    @Tag("soap:Header", NSSoapEnvelope)
    var header = headerInit()

    @Tag("soap:Body", NSSoapEnvelope)
    var body = OutBody(businessInit)
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

open class ParameterWithFunID(@Ignore val funID: String) : Parameter()

class Login : ParameterWithFunID("F00.00.00.00|192.168.1.110|PC-20170427DGON|00-05-0F-08-1A-34")

class InBusiness : Parameter() {
    @Property("startrow")
    var startRow = ""

    @Property("row_count")
    var rowCount = ""

    @Property("pagesize")
    var pageSize = ""

    @Property("clientsql")
    var clientSql = ""

    @Property("functionid")
    var functionID = ""
}

class OutHeader : Result() {
    var sessionID = ""
    var message = ""
}

class Business : Result() {
    var result = ""

    @Property("querylist")
    var queryList: ResultSet<Sncbry> = ResultSet()

    @Property("row_count")
    var rowCount = ""

    @Property("querysql")
    var querySql = ""
}

class Sncbry {
    @Property("aac003")
    var name = ""

    @Property("aac002")
    var idcard = ""

    var rown = ""
}