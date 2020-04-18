package cn.yhsb.qb.service

import cn.yhsb.base.GenericClass
import cn.yhsb.cjb.service.Jsonable
import org.junit.Test

class XmlTest {

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

    class OutHeader : Result() {
        var sessionID = ""
        var message = ""
    }

    val nsOut = "http://www.molss.gov.cn/"
    val nsSoapEnvelope = "http://schemas.xmlsoap.org/soap/envelope/"

    class OutBody {
        @Tag("out:business", NSOut)
        var business = Business()
    }

    class OutEnvelope {
        @Tag("soap:Header", NSSoapEnvelope)
        var header = OutHeader()

        @Tag("soap:Body", NSSoapEnvelope)
        var body = OutBody()
    }

    class OutBody2<B> {
        @Tag("out:business", NSOut)
        var business: B? = null
    }

    class OutEnvelope2<H, B> : Jsonable() {
        @Tag("soap:Header", NSSoapEnvelope)
        var header: H? = null

        @Tag("soap:Body", NSSoapEnvelope)
        var body: OutBody2<B>? = null
    }

    val nsIn = "http://www.molss.gov.cn/"

    class InSystem : Parameter() {
        @Property("usr")
        var user = ""

        @Property("pwd")
        var password = ""

        @Property("funid")
        var funID = ""
    }

    class InHeader {
        @Tag("in:system", NSIn)
        var system = InSystem()
    }

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

    class InBody {
        @Tag("in:business", NSIn)
        var business = InBusiness()
    }

    class InEnvelop {
        @Property("soap:encodingStyle", NSSoapEnvelope)
        val encodingStyle = "http://schemas.xmlsoap.org/soap/encoding/"

        @Tag("soap:Header", NSSoapEnvelope)
        var header = InHeader()

        @Tag("soap:Body", NSSoapEnvelope)
        var body = InBody()
    }

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

        val rs = Result.fromXmlElement(XmlUtil.rootElement(xml),
                GenericClass(Business::class))
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

        val env = XmlUtil.fromXml<OutEnvelope>(xml)
        println(env)
        println(env.header.sessionID)
        println(env.header.message)
        println(env.body.business.rowCount)
        env.body.business.queryList.forEach {
            println("${it.name} ${it.idcard} ${it.rown}")
        }

        val env2 = XmlUtil.fromXml<OutEnvelope2<OutHeader, Business>>(
                xml, OutHeader::class, Business::class)
        println(env2)
    }

    @Test
    fun testXml3() {
        /*
        val xml = """<?xml version="1.0" encoding="GBK"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" soap:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
  <soap:Header>
    <in:system xmlns:in="http://www.molss.gov.cn/">
      <para usr="abc"/>
      <para pwd="YLZ_A2ASSDFDFDSS"/>
      <para funid="F00.01.03"/>
    </in:system>
  </soap:Header>
  <soap:Body>
    <in:business xmlns:in="http://www.molss.gov.cn/">
      <para startrow="1"/>
      <para row_count="-1"/>
      <para pagesize="500"/>
      <para clientsql="( aac002 = &apos;430302195806251012&apos;)"/>
      <para functionid="F27.06"/>
    </in:business>
  </soap:Body>
</soap:Envelope>"""
         */

        val env = InEnvelop()
        val doc = XmlUtil.xmlDocument(env, "soap:Envelope", nsSoapEnvelope)
        println(doc.transfromToString())
    }
}
