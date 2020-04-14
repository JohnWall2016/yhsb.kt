package cn.yhsb.base

import org.junit.Test
import org.w3c.dom.*
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

object XmlUtil {
    fun <T> elementToObject(elem: Element, type: KClass<*>): T {
        val inst = type.createInstance()
        type.memberProperties
                .filter { it.visibility == KVisibility.PUBLIC }
                .filterIsInstance<KMutableProperty1<Any, String>>()
                .forEach { prop ->
                    val an = prop.annotations.find { it is Property } as? Property
                    val name = an?.name ?: prop.name
                    val value = elem.getAttribute(name)
                    if (value != null) {
                        prop.set(inst, value)
                    }
                }
        @Suppress("UNCHECKED_CAST")
        return inst as T
    }

    inline fun <reified T> elementToObject(elem: Element): T = elementToObject(elem, T::class)
}

annotation class Property(val name: String)
annotation class Container(val itemClass: KClass<*>)
annotation class Tag(val namespace: String, val localName: String)

class ResultSet<T> : ArrayList<T>() {
    companion object {
        fun <T : Any> fromXmlElement(e: Element, subtype: KClass<T>): ResultSet<T> {
            val rs = ResultSet<T>()
            val elems = e.getElementsByTagName("row")
            for (i in 0 until elems.length) {
                (elems.item(i) as? Element)?.let {
                    rs.add(XmlUtil.elementToObject(it, subtype))
                }
            }
            return rs
        }

        inline fun <reified T : Any> fromXmlElement(e: Element): ResultSet<T> = fromXmlElement(e, T::class)
    }
}

open class Result {
    companion object {
        fun <T : Result> fromXmlElement(elem: Element, type: KClass<T>): T {
            val result = mutableMapOf<String, String>()
            val resultSet = mutableMapOf<String, Element>()
            elem.childNodes.asSequence()
                    .filterIsInstance<Element>().forEach { child ->
                // println("${child.tagName}|${child.localName}")
                when (child.tagName) {
                    "result" -> {
                        child.attributes.asSequence()
                                .filterIsInstance<Attr>()
                                .forEach {
                                    result[it.name] = it.value
                                }
                    }
                    "resultset" -> {
                        val name = child.getAttribute("name")
                        if (name != null) {
                            resultSet[name] = child
                        }
                    }
                }
            }
            val inst = type.createInstance()
            type.memberProperties
                    .filter { it.visibility == KVisibility.PUBLIC }
                    .filterIsInstance<KMutableProperty<*>>()
                    .forEach { prop ->
                        val name = prop.findAnnotation<Property>()?.name ?: prop.name
                        when (prop.returnType.jvmErasure) {
                            String::class -> result[name]?.let { prop.setter.call(inst, it) }
                            ResultSet::class -> {
                                resultSet[name]?.let { elem ->
                                    prop.returnType.arguments
                                            .firstOrNull()?.type?.jvmErasure?.let {
                                        prop.setter.call(inst, ResultSet.fromXmlElement(elem, it))
                                    }
                                }
                            }
                        }
                    }
            return inst
        }

        inline fun <reified T : Result> fromXmlElement(e: Element): T = fromXmlElement(e, T::class)
    }
}

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
    val business = Business()
}

class Envelope {
    @Tag(NSSoapEnvelope, "Header")
    val header = Header()

    @Tag(NSSoapEnvelope, "Body")
    val body = Body()
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

        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(InputSource(StringReader(xml)))

        //val elem = doc.getElementsByTagNameNS("http://www.molss.gov.cn/", "business").item(0) as? Element
        val elem = doc.documentElement
        println(elem)

        val rs = Result.fromXmlElement<Business>(elem!!)
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

        val doc = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        val env = doc.documentElement
        println(env)
        println(env.namespaceURI)
        println(env.baseURI)
        val nodes = env.childNodes

        println(env.getElementsByTagNameNS("*", "Header").length)
        println(env.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Header").length)
        println(env.getElementsByTagNameNS("soap", "Header").length) // 0

        nodes.asSequence().filterIsInstance<Element>().forEach {
            println("${it.namespaceURI} ${it.nodeName} ${it.tagName} ${it.localName}")

        }
        println(nodes.length)
    }
}

fun NodeList.asSequence(): Sequence<Node> = sequence {
    for (i in 0 until this@asSequence.length) {
        yield(this@asSequence.item(i))
    }
}

fun NamedNodeMap.asSequence(): Sequence<Node> = sequence {
    for (i in 0 until this@asSequence.length) {
        yield(this@asSequence.item(i))
    }
}

