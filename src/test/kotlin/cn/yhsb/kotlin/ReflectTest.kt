package cn.yhsb.kotlin

import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties

annotation class Property(val name: String)

class Person {
    @Property("abc001")
    var name: String = ""

    @Property("efg002")
    var idcard: String = ""

    var age: String = ""
}

object Util {
    fun <T> mapToObject(map: Map<String, String>, type: KClass<*>): T {
        val inst = type.createInstance()
        type.memberProperties
                .filter { it.visibility == KVisibility.PUBLIC }
                .filterIsInstance<KMutableProperty<*>>()
                .forEach { prop ->
                    var name = prop.name
                    val an = prop.annotations.find { it is Property } as? Property
                    if (an != null) name = an.name
                    val value = map[name]
                    if (value != null) {
                        prop.setter.call(inst, value)
                    }
                }
        return inst as T
    }

    inline fun <reified T> mapToObject(map: Map<String, String>): T = mapToObject(map, T::class)
}

class ReflectTest {
    @Test
    fun testReflect() {
        val map = mapOf(
                "abc001" to "John",
                "efg002" to "ABC123",
                "age" to "4"
        )

        val obj = Util.mapToObject<Person>(map)

        println("${obj.name} ${obj.idcard} ${obj.age}")
    }
}