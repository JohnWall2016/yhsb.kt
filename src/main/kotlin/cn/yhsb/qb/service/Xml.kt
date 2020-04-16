package cn.yhsb.qb.service

import cn.yhsb.base.CustomField
import cn.yhsb.cjb.service.Jsonable
import cn.yhsb.qb.service.Result.Companion.populateResult
import org.w3c.dom.*
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import java.lang.reflect.Field
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.reflect.*
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

annotation class Property(val name: String = "", val namespace: String = "")
annotation class Container(val itemClass: KClass<*>)
annotation class Tag(val name: String, val namespace: String = "")
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

open class Result : Jsonable() {
    companion object {
        val type = Result::class.createType()

        fun <T : Result> fromXmlElement(elem: Element, type: KClass<T>): T {
            val result = mutableMapOf<String, String>()
            val resultSet = mutableMapOf<String, Element>()
            elem.childNodes.asSequence()
                    .filterIsInstance<Element>().forEach { child ->
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
            type.publicMemberProperties
                    .filterIsInstance<KMutableProperty<*>>()
                    .forEach { prop ->
                        var name = prop.findAnnotation<Property>()?.name ?: ""
                        if (name == "") name = prop.name
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

        fun <T : Result> Element.populateResult(obj: T) {
            val result = mutableMapOf<String, String>()
            val resultSet = mutableMapOf<String, Element>()
            childNodes.asSequence()
                    .filterIsInstance<Element>().forEach { child ->
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
            obj::class.publicMemberProperties
                    .filterIsInstance<KMutableProperty<*>>()
                    .forEach { prop ->
                        var name = prop.findAnnotation<Property>()?.name ?: ""
                        if (name == "") name = prop.name
                        when (prop.returnType.jvmErasure) {
                            String::class -> result[name]?.let { prop.setter.call(obj, it) }
                            ResultSet::class -> {
                                resultSet[name]?.let { elem ->
                                    prop.returnType.arguments
                                            .firstOrNull()?.type?.jvmErasure?.let {
                                                prop.setter.call(obj, ResultSet.fromXmlElement(elem, it))
                                            }
                                }
                            }
                        }
                    }
        }
    }
}

object XmlUtil {
    fun <T : Any> elementToObject(elem: Element, type: KClass<T>): T {
        val inst = type.createInstance()
        type.publicMemberProperties
                .filterIsInstance<KMutableProperty1<Any, Any>>()
                .forEach { prop ->
                    var name = prop.findAnnotation<Property>()?.name ?: ""
                    if (name == "") name = prop.name
                    val value = elem.getAttribute(name)
                    if (value != null) {
                        if (prop.returnType.isSubtypeOf(CustomField.type)) {
                            val f = prop.get(inst) as? CustomField
                            if (f != null) {
                                f.value = value
                            } else {
                                (prop.returnType.jvmErasure
                                        .createInstance() as? CustomField)?.let {
                                    it.value = value
                                    prop.set(inst, it)
                                }
                            }
                        } else if (prop.returnType.jvmErasure == String::class) {
                            prop.set(inst, value)
                        }
                    }
                }
        return inst
    }

    inline fun <reified T : Any> elementToObject(elem: Element): T = elementToObject(elem, T::class)

    fun <T : Any> fromXmlElement(elem: Element, type: KClass<T>): T {
        val inst = type.createInstance()
        type.publicMemberProperties
                .filterIsInstance<KMutableProperty<*>>()
                .forEach { prop ->
                    if (prop.findAnnotation<Ignore>() != null)
                        return@forEach

                    val an = prop.findAnnotation<Tag>()
                    var namespace = an?.namespace ?: ""
                    if (namespace == "") namespace = "*"
                    var localName = an?.name ?: ""
                    val index = localName.indexOf(":")
                    if (index >= 0) localName = localName.substring(index + 1)
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

    fun <T : Any> Element.populateObject(obj: T): T {
        obj::class.publicMemberProperties
                .filterIsInstance<KMutableProperty<*>>()
                .forEach { prop ->
                    if (prop.findAnnotation<Ignore>() != null)
                        return@forEach

                    val an = prop.findAnnotation<Tag>()
                    var namespace = an?.namespace ?: ""
                    if (namespace == "") namespace = "*"
                    var localName = an?.name ?: ""
                    val index = localName.indexOf(":")
                    if (index >= 0) localName = localName.substring(index + 1)
                    if (localName == "") localName = prop.name

                    getElementsByTagNameNS(namespace, localName)
                            .asSequence()
                            .filterIsInstance<Element>()
                            .firstOrNull()
                            ?.let {
                                if (prop.returnType.isSubtypeOf(Result.type)) {
                                    val o = prop.getter.call(obj) as? Result
                                    @Suppress("UNCHECKED_CAST")
                                    val c = prop.returnType.jvmErasure as KClass<Result>
                                    if (o != null)
                                        it.populateResult(o)
                                    else
                                        prop.setter.call(obj, Result.fromXmlElement(it, c))
                                } else {
                                    val o = prop.getter.call(obj)
                                    if (o != null)
                                        it.populateObject(o)
                                    else
                                        prop.setter.call(obj,
                                                fromXmlElement(it, prop.returnType.jvmErasure))
                                }
                            }
                }
        return obj
    }

    fun rootElement(xml: String): Element = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(InputSource(StringReader(xml)))
            .documentElement

    fun xmlDocument(obj: Any, tagName: String, namespace: String?): Document {
        return DocumentBuilderFactory.newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .newDocument().apply {
                    appendChild(toXmlElement(obj, tagName, namespace))
                }
    }
}

open class Parameter : Jsonable() {
    companion object {
        val type = Parameter::class.createType()

        fun <T : Parameter> toXmlElement(doc: Document, param: T, tagName: String, namespace: String? = null): Element {
            val elem = if (namespace.isNullOrEmpty())
                doc.createElement(tagName)
            else
                doc.createElementNS(namespace, tagName)
            param::class.publicMemberProperties
                    .filterIsInstance<KProperty1<Any, Any>>()
                    .forEach { prop ->
                        if (prop.findAnnotation<Ignore>() != null)
                            return@forEach
                        val ppt = prop.findAnnotation<Property>()
                        var name = ppt?.name ?: ""
                        if (name == "") name = prop.name
                        val ns = ppt?.namespace
                        val value = prop.get(param).toString()
                        elem.appendChild(doc.createElement("para").apply {
                            if (ns.isNullOrEmpty())
                                setAttribute(name, value)
                            else
                                setAttributeNS(ns, name, value)
                        })
                    }
            return elem
        }
    }
}

fun <T : Any> Document.toXmlElement(obj: T, tagName: String, namespace: String? = null): Element {
    val elem = if (namespace == null)
        this.createElement(tagName)
    else {
        this.createElementNS(namespace, tagName)
    }
    obj::class.publicMemberProperties
            .filterIsInstance<KProperty1<Any, Any?>>()
            .forEach { prop ->
                if (prop.findAnnotation<Ignore>() != null)
                    return@forEach
                prop.get(obj)?.let {
                    val ppt = prop.findAnnotation<Property>()
                    if (ppt != null && prop.returnType.jvmErasure == String::class) {
                        val ns = ppt.namespace
                        val name = if (ppt.name == "") prop.name else ppt.name
                        if (ns == "")
                            elem.setAttribute(name, prop.get(obj) as String)
                        else
                            elem.setAttributeNS(ns, name, prop.get(obj) as String)
                    } else {
                        val tag = prop.findAnnotation<Tag>()
                        val tnamespace = tag?.namespace
                        val tname = tag?.name ?: prop.name
                        elem.appendChild(
                                if (prop.returnType.isSubtypeOf(Parameter.type)) {
                                    Parameter.toXmlElement(this, it as Parameter, tname, tnamespace)
                                } else {
                                    this@toXmlElement.toXmlElement(it, tname, tnamespace)
                                }
                        )
                    }
                }
            }
    return elem
}

fun Document.transfromToString(declare: String? = null): String {
    return StringWriter().let {
        if (declare != null) it.write(declare)
        TransformerFactory.newInstance().newTransformer()
                .apply {
                    if (declare != null) {
                        setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
                    }
                }.transform(DOMSource(this), StreamResult(it))
        it.buffer.toString()
    }
}

private val javaDeclaredPropertiesMap = mutableMapOf<KClass<*>, List<KProperty1<*, *>>>()

val <T : Any> KClass<T>.javaDeclaredProperties: List<KProperty1<*, *>>
    get() {
        val ret = javaDeclaredPropertiesMap[this]
        if (ret != null) return ret

        var jclass = java as Class<*>
        val fields = mutableListOf<Field>()
        while (jclass != Object::class.java) {
            fields.addAll(jclass.declaredFields)
            jclass = jclass.superclass as Class<*>
        }
        val properties = Array<KProperty1<*, *>?>(fields.size) { null }
        memberProperties.forEach { prop ->
            val index = fields.indexOfFirst {
                it.name == prop.name
            }
            if (index >= 0) properties[index] = prop
        }
        return properties.filterNotNull().apply {
            javaDeclaredPropertiesMap[this@javaDeclaredProperties] = this
        }
    }

val <T : Any> KClass<T>.publicMemberProperties: Sequence<KProperty1<*, *>>
    get() = this.javaDeclaredProperties.filter { it.visibility == KVisibility.PUBLIC }.asSequence()
