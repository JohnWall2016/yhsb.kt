package cn.yhsb.qb.service


import org.w3c.dom.*
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure


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

annotation class Property(val name: String)
annotation class Container(val itemClass: KClass<*>)
annotation class Tag(val namespace: String, val localName: String)
annotation class Ignore

class ResultSet<T> : ArrayList<T>() {
    companion object {
        fun <T : Any> fromXmlElement(e: Element, subtype: KClass<T>): ResultSet<T> {
            val rs = ResultSet<T>()
            e.getElementsByTagName("row").asSequence()
                    .filterIsInstance<Element>().forEach {
                        rs.add(XmlUtil.elementToObject(it, subtype))
                    }
            return rs
        }

        inline fun <reified T : Any> fromXmlElement(e: Element): ResultSet<T> = fromXmlElement(e, T::class)
    }
}

open class Result {
    companion object {
        val type = Result::class.createType()

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

object XmlUtil {
    fun <T : Any> elementToObject(elem: Element, type: KClass<T>): T {
        val inst = type.createInstance()
        type.memberProperties
                .filter { it.visibility == KVisibility.PUBLIC }
                .filterIsInstance<KMutableProperty1<Any, String>>()
                .forEach { prop ->
                    val name = prop.findAnnotation<Property>()?.name ?: prop.name
                    val value = elem.getAttribute(name)
                    if (value != null) {
                        prop.set(inst, value)
                    }
                }
        return inst
    }

    inline fun <reified T : Any> elementToObject(elem: Element): T = elementToObject(elem, T::class)

    fun <T : Any> fromXmlElement(elem: Element, type: KClass<T>): T {
        val inst = type.createInstance()
        type.memberProperties
                .filter { it.visibility == KVisibility.PUBLIC }
                .filterIsInstance<KMutableProperty<*>>()
                .forEach { prop ->
                    if (prop.findAnnotation<Ignore>() != null)
                        return@forEach

                    val an = prop.findAnnotation<Tag>()
                    var namespace = an?.namespace ?: ""
                    if (namespace == "") namespace = "*"
                    var localName = an?.localName ?: ""
                    if (localName == "") localName = prop.name

                    elem.getElementsByTagNameNS(namespace, localName)
                            .asSequence()
                            .filterIsInstance<Element>()
                            .firstOrNull()
                            ?.let {
                                prop.setter.call(inst,
                                        if (prop.returnType.isSubtypeOf(Result.type))
                                            @Suppress("UNCHECKED_CAST")
                                            Result.fromXmlElement(it,
                                                    prop.returnType.jvmErasure as KClass<Result>)
                                        else
                                            fromXmlElement(it, prop.returnType.jvmErasure)
                                )
                            }
                }
        return inst
    }

    inline fun <reified T : Any> fromXmlElement(elem: Element): T = fromXmlElement(elem, T::class)

    inline fun <reified T : Any> fromXml(xml: String): T = fromXmlElement(rootElement(xml), T::class)

    fun rootElement(xml: String): Element = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(InputSource(StringReader(xml)))
            .documentElement
}