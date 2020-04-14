package cn.yhsb.qb.service

import org.junit.Test

class Sncbry {
    @Property("aac003")
    var name = ""

    @Property("aac002")
    var idcard = ""

    var rown = ""
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

class Header : Result() {
    var sessionID = ""
    var message = ""
}

const val NSOut = "http://www.molss.gov.cn/"
const val NSSoapEnvelope = "http://schemas.xmlsoap.org/soap/envelope/"

class Body {
    @Tag(NSOut, "business")
    var business = Business()
}

class Envelope {
    @Tag(NSSoapEnvelope, "Header")
    var header = Header()

    @Tag(NSSoapEnvelope, "Body")
    var body = Body()
}

class XmlTest {
    @Test
    fun testXml() {
        val xml = """<?xml version="1.0" encoding="GBK"?>
<out:business xmlns:out="http://www.molss.gov.cn/">
    <result result="" />
    <resultset name="querylist">
        <row aac003="徐X" rown="1" aac008="2" aab300="XXXXXXX服务局" sac007="101" aac031="3" aac002="43030219XXXXXXXXXX" />
    </resultset>
    <result row_count="1" />
    <result querysql="select * from 
  from ac01_css a, ac02_css b
 where a.aac001 = b.aac001) where ( aac002 = &apos;43030219XXXXXXXX&apos;) and 1=1) row_ where rownum &lt;(501)) where rown &gt;=(1) " />
</out:business>"""

        val rs = Result.fromXmlElement<Business>(XmlUtil.rootElement(xml))
        println(rs.rowCount)
        println(rs.querySql)
        rs.queryList.forEach {
            println("${it.name} ${it.idcard} ${it.rown}")
        }
    }

    @Test
    fun testXml2() {
        val xml = """<?xml version="1.0" encoding="GBK"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" soap:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
  <soap:Header>
    <result sessionID="DpPZb8mZ0qgv08kN26LyKmm1yDz4nn7QvXxh2VD32vDvgvQ2zw14!-23337339!1530701497001"/>
    <result message=""/>
  </soap:Header>
  <soap:Body>
    <out:business xmlns:out="http://www.molss.gov.cn/">
      <result result="" />
      <resultset name="querylist">
        <row aac003="徐X" rown="1" aac008="2" aab300="XXXXXXX服务局" sac007="101" aac031="3" aac002="43030219XXXXXXXXXX" />
      </resultset>
      <result row_count="1" />
      <result querysql="select * from
        from ac01_css a, ac02_css b
        where a.aac001 = b.aac001) where ( aac002 = &apos;43030219XXXXXXXX&apos;) and 1=1) row_ where rownum &lt;(501)) where rown &gt;=(1) " />
    </out:business>
  </soap:Body>
</soap:Envelope>"""

        val env = XmlUtil.fromXml<Envelope>(xml)
        println(env)
        println(env.header.sessionID)
        println(env.header.message)
        println(env.body.business.rowCount)
        env.body.business.queryList.forEach {
            println("${it.name} ${it.idcard} ${it.rown}")
        }
    }
}
