package cn.yhsb.base

import org.junit.Test
import org.w3c.dom.Element
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties

annotation class Property(val name: String)

class Result {
    companion object {

    }
}

class ResultSet<T> : ArrayList<T>() {

    companion object {
        fun <T> fromXmlElement(e: Element, subtype: KClass<*>): ResultSet<T> {
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

        inline fun <reified T> fromXmlElement(e: Element): List<T> = fromXmlElement(e, T::class)
    }

}

object XmlUtil {
    fun <T> elementToObject(elem: Element, type: KClass<*>): T {
        val inst = type.createInstance()
        type.memberProperties
                .filter { it.visibility == KVisibility.PUBLIC }
                .filterIsInstance<KMutableProperty<*>>()
                .forEach { prop ->
                    val an = prop.annotations.find { it is Property } as? Property
                    val name = an?.name ?: prop.name
                    val value = elem.getAttribute(name)
                    if (value != null) {
                        prop.setter.call(inst, value)
                    }
                }
        return inst as T
    }

    inline fun <reified T> elementToObject(elem: Element): T = elementToObject(elem, T::class)
}


class XmlTest {
    @Test
    fun testXml() {
        val xml = """
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

        class Business {

        }
    }
}