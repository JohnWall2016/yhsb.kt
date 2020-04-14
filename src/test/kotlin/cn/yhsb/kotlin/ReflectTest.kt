package cn.yhsb.kotlin

import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

annotation class Property(val name: String)
annotation class Container(val itemClass: KClass<*>)

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
                .filterIsInstance<KMutableProperty1<Any, String>>()
                .forEach { prop ->
                    val name = prop.findAnnotation<Property>()?.name ?: prop.name
                    val value = map[name]
                    if (value != null) {
                        prop.set(inst, value)
                    }
                }
        @Suppress("UNCHECKED_CAST")
        return inst as T
    }

    inline fun <reified T> mapToObject(map: Map<String, String>): T = mapToObject(map, T::class)
}


class ResultSet<T> : ArrayList<T>()

class Result {
    @Container(itemClass = Person::class)
    var queryList: ResultSet<Person> = ResultSet()

    var name: String = ""
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

    @Test
    fun testTypeParameter() {
        val t = Result::class
        t.memberProperties
                .filterIsInstance<KMutableProperty1<*, *>>()
                .forEach {
                    println(it)
                    println(it.returnType)
                    println(it.returnType.javaType)
                    println(it.returnType.jvmErasure)
                    println(it.returnType.arguments)

                    if (it.returnType.jvmErasure == String::class) {
                        println("String")
                    } else if (it.returnType.jvmErasure == ResultSet::class) {
                        println("ResultSet")
                        if (it.returnType.arguments.isNotEmpty()) {
                            val p = it.returnType.arguments[0]
                            println(p.type?.jvmErasure?.java?.getConstructor()?.newInstance())
                            //println(p.type.)
                            //val obj = p.type?.javaClass?.getConstructor()?.newInstance()
                            //println(obj)
                        }
                    }

                    /*val c = it.findAnnotation<Container>()
                    println(c?.itemClass)*/
                }
    }
}
