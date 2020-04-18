package cn.yhsb.base

import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.memberProperties

private val declaredPropertiesMap = mutableMapOf<KClass<*>, List<KProperty1<*,*>>>()

@Suppress("UNCHECKED_CAST")
class GenericClass<T : Any>(val kClass: KClass<*>,
                            val typeArguments: List<GenericClass<*>> = listOf()
) :KClass<T> by kClass as KClass<T> {

    init {
        if (kClass.typeParameters.size != typeArguments.size)
            throw IllegalArgumentException(
                    "the size is mismatched, expect ${kClass.typeParameters.size} got ${typeArguments.size}")
    }

    constructor(kClass: KClass<*>, vararg typeArguments: KClass<*>)
            : this(kClass, typeArguments.map { if (it is GenericClass<*>) it else GenericClass<Any>(it) })

    fun resolveTypeParameter(param: KTypeParameter): GenericClass<*>? {
        val index = kClass.typeParameters.indexOf(param)
        if (index >= 0) return typeArguments[index]
        return null
    }

    val declaredProperties: List<KProperty1<T, *>> get() {
        @Suppress("UNCHECKED_CAST")
        val result = declaredPropertiesMap[kClass] as? List<KProperty1<T, *>>
        if (result != null) return result

        val fields = mutableListOf<Field>()
        var jclass = kClass.java as Class<*>
        while (jclass != Object::class.java) {
            fields.addAll(jclass.declaredFields)
            jclass = jclass.superclass as Class<*>
        }
        val properties = Array<KProperty1<T, *>?>(fields.size) { null }
        val propertiesNotMatch = mutableListOf<KProperty1<T, *>>()
        kClass.memberProperties.forEach { prop ->
            val index = fields.indexOfFirst {
                it.name == prop.name
            }
            if (index >= 0) properties[index] = prop as KProperty1<T, *>
            else propertiesNotMatch.add(prop as KProperty1<T, *>)
        }

        return if (propertiesNotMatch.isEmpty()) {
            properties.filterNotNull()
        } else {
            properties.filterNotNull().toMutableList().apply {
                addAll(propertiesNotMatch)
            }
        }.apply { declaredPropertiesMap[kClass] = this }
    }
}

