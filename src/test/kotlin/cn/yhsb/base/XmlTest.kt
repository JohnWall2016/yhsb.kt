package cn.yhsb.base

import org.junit.Test
import org.w3c.dom.Attr
import org.w3c.dom.Element
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

annotation class Property(val name: String)
annotation class Container(val itemClass: KClass<*>)

open class Result {
    companion object {
        fun <T : Result> fromXmlElement(e: Element, type: KClass<T>): T {
            val result = mutableMapOf<String, String>()
            val resultSet = mutableMapOf<String, Element>()
            val elems = e.childNodes
            for (i in 0 until elems.length) {
                (elems.item(i) as? Element)?.let {
                    when (it.localName) {
                        "result" -> {
                            val attrs = it.attributes
                            for (ii in 0 until attrs.length) {
                                val attr = attrs.item(ii) as? Attr
                                if (attr != null)
                                    result[attr.localName] = attr.value
                            }
                        }
                        "resultset" -> {
                            val name = it.getAttribute("name")
                            if (name != null) {
                                resultSet[name] = it
                            }
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
                                    prop.returnType.arguments.firstOrNull()?.type?.jvmErasure?.let {
                                        prop.setter.call(inst, XmlUtil.elementToObject(elem, it))
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

class ResultSet<T> : ArrayList<T>() {
    companion object {
        fun <T : Any> fromXmlElement(e: Element, subtype: KClass<T>): ResultSet<T> {
            val rs = ResultSet<T>()
            val elems = e.getElementsByTagName("row")
            for (i in 0 until elems.length) {
                val n = elems.item(i) as? Element
                if (n != null) {
                    rs.add(XmlUtil.elementToObject(n, subtype))
                }
            }
            return rs
        }

        inline fun <reified T : Any> fromXmlElement(e: Element): ResultSet<T> = fromXmlElement(e, T::class)
    }
}

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


class XmlTest {
    @Test
    fun testXml() {
        /*
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
         */
        val xml = """<?xml version="1.0" encoding="GBK"?>
<business>
    <result result="" />
    <resultset name="querylist">
        <row aac003="徐X" rown="1" aac008="2" aab300="XXXXXXX服务局" sac007="101" aac031="3" aac002="43030219XXXXXXXXXX" />
    </resultset>
    <result row_count="1" />
    <result querysql="select * from 
  from ac01_css a, ac02_css b
 where a.aac001 = b.aac001) where ( aac002 = &apos;43030219XXXXXXXX&apos;) and 1=1) row_ where rownum &lt;(501)) where rown &gt;=(1) " />
</business>"""

        class Sncbry

        class Business : Result() {
            var result = ""

            @Property("querylist")
            var queryList: ResultSet<Sncbry> = ResultSet()

            @Property("row_count")
            var rowCount = ""

            @Property("querysql")
            var querySql = ""
        }

        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(InputSource(StringReader(xml)))

        //val elem = doc.getElementsByTagNameNS("http://www.molss.gov.cn/", "business").item(0) as? Element
        val elem = doc.documentElement
        println(elem)

        val rs = Result.fromXmlElement<Business>(elem!!)
        println(rs)
        println(rs.rowCount)
    }
}