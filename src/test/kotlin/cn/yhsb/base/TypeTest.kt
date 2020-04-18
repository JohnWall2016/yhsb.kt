package cn.yhsb.base

import cn.yhsb.cjb.service.Jsonable
import org.junit.Test
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.createInstance
import kotlin.reflect.jvm.jvmErasure

class TypeTest {
    class Email<H, B> : Jsonable() {
        var header: H? = null
        var body: B? = null
    }

    class Header {
        val title: String = "A greeting"
        val from: String = "wj@mail.com"
    }

    class Body {
        val message: String = "hello, Peter."
    }

    fun <T : Any> createObject(gClass: GenericClass<T>): T {
        println(gClass)
        val inst = gClass.createInstance()
        gClass.declaredProperties
                .filterIsInstance<KMutableProperty1<Any, Any>>()
                .forEach { prop ->
                    println("prop: $prop")
                    val type = prop.returnType
                    val classifier = type.classifier
                    val propInst = if (classifier is KTypeParameter) {
                        gClass.resolveTypeParameter(classifier)?.let {
                            createObject(it)
                        }
                    } else {
                        createObject(GenericClass(type.jvmErasure))
                    }
                    if (propInst != null) {
                        prop.set(inst, propInst)
                    }
        }
        return inst
    }

    @Test
    fun testType() {
        val obj = createObject(GenericClass(Email::class, Header::class, Body::class))
        println(obj)
    }
}