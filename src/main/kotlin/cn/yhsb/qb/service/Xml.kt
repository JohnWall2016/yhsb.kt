package cn.yhsb.qb.service

import cn.yhsb.base.CustomField
import cn.yhsb.base.GenericClass
import cn.yhsb.cjb.service.Jsonable
import org.w3c.dom.*
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
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
        fun <T : Any> fromXmlElement(e: Element, gClass: GenericClass<T>): ResultSet<T> {
            val rs = ResultSet<T>()
            e.getElementsByTagName("row").asSequence()
                    .filterIsInstance<Element>().forEach {
                        rs.add(XmlUtil.elementToObject(it, gClass))
                    }
            return rs
        }
    }
}

open class Result : Jsonable() {
    companion object {
        val type = Result::class.createType()

        fun <T : Result> fromXmlElement(elem: Element, gClass: GenericClass<T>): T {
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
            val inst = gClass.createInstance()
            gClass.kClass.memberProperties
                    .filter { it.visibility == KVisibility.PUBLIC }
                    .filterIsInstance<KMutableProperty1<T, Any>>()
                    .forEach { prop ->
                        var name = prop.findAnnotation<Property>()?.name ?: ""
                        if (name == "") name = prop.name
                        when (prop.returnType.jvmErasure) {
                            String::class -> result[name]?.let { prop.set(inst, it) }
                            ResultSet::class -> {
                                resultSet[name]?.let { elem ->
                                    prop.returnType.arguments
                                            .firstOrNull()?.type?.let {
                                                // println(it)
                                                val classifier = it.classifier
                                                val subGClass = if (classifier is KTypeParameter) {
                                                    gClass.resolveTypeParameter(classifier)
                                                } else {
                                                    // println(it.jvmErasure)
                                                    GenericClass(it.jvmErasure)
                                                }
                                                if (subGClass != null)
                                                    prop.set(inst,
                                                            ResultSet.fromXmlElement(elem, subGClass))
                                            }
                                }
                            }
                        }
                    }
            return inst
        }
    }
}

object XmlUtil {
    fun <T : Any> elementToObject(elem: Element, gClass: GenericClass<T>): T {
        val inst = gClass.createInstance()
        // println(inst::class)
        gClass.kClass.memberProperties
                .filter { it.visibility == KVisibility.PUBLIC }
                .filterIsInstance<KMutableProperty1<T, Any>>()
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

    fun <T : Any> fromXmlElement(elem: Element, gClass: GenericClass<T>): T {
        val inst = gClass.createInstance()
        gClass.kClass.memberProperties
                .filter { it.visibility == KVisibility.PUBLIC }
                .filterIsInstance<KMutableProperty1<T, Any>>()
                .forEach { prop ->
                    // println("FXE: $prop")
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
                            ?.let { elem ->
                                val type = prop.returnType
                                if (type.isSubtypeOf(Result.type)) {
                                    @Suppress("UNCHECKED_CAST")
                                    prop.set(inst, Result.fromXmlElement(elem,
                                            GenericClass(type.jvmErasure as KClass<Result>)))
                                } else if (type.arguments.isNotEmpty()) {
                                    val args = type.arguments.map {
                                        // println("TA: $it")
                                        val classifier = it.type?.classifier
                                        if (classifier is KTypeParameter) {
                                            val resolvedClass = gClass.resolveTypeParameter(classifier)
                                            // println("RSLV: $resolvedClass|${resolvedClass?.kClass}")
                                            resolvedClass ?: GenericClass(Object::class)
                                        } else {
                                            GenericClass(it.type?.jvmErasure ?: Object::class)
                                        }
                                    }
                                    prop.set(inst, fromXmlElement(elem, GenericClass(type.jvmErasure, args)))
                                } else {
                                    val classifier = type.classifier
                                    if (classifier is KTypeParameter) {
                                        val subGClass = gClass.resolveTypeParameter(classifier)
                                        if (subGClass != null) {
                                            if (subGClass.isSubclassOf(Result::class)) {
                                                @Suppress("UNCHECKED_CAST")
                                                prop.set(inst, Result.fromXmlElement(elem,
                                                        subGClass as GenericClass<Result>))
                                            } else {
                                                prop.set(inst, fromXmlElement(elem, subGClass))
                                            }
                                        }
                                    } else {
                                        prop.set(inst, fromXmlElement(elem,
                                                GenericClass(type.jvmErasure)))
                                    }
                                }
                            }
                }
        return inst
    }

    fun <T : Any> fromXml(gClass: GenericClass<T>, xml: String): T = fromXmlElement(rootElement(xml), gClass)

    inline fun <reified T : Any> fromXml(xml: String, vararg typeArguments: KClass<*>): T = fromXml(GenericClass(T::class, *typeArguments), xml)

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
            GenericClass(param::class).declaredProperties
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
    GenericClass(obj::class).declaredProperties
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
