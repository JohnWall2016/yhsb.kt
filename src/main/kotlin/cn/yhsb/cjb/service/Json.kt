package cn.yhsb.cjb.service

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

interface JsonAdapter<T> : JsonSerializer<T>, JsonDeserializer<T>

open class JsonField {
    var value: String? = null
        protected set

    open val name: String
        get() = "未知值: $value"

    override fun toString(): String {
        return name
    }

    class Adapter : JsonAdapter<JsonField> {
        override fun serialize(src: JsonField, typeOfSrc: Type,
                               context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.value)
        }

        @Throws(JsonParseException::class)
        override fun deserialize(json: JsonElement, typeOfT: Type,
                                 context: JsonDeserializationContext): JsonField {
            try {
                val clazz = typeOfT as Class<*>
                val field = clazz.getConstructor().newInstance() as JsonField
                field.value = json.asString
                return field
            } catch (e: Exception) {
                throw JsonParseException(e)
            }
        }
    }
}

object GsonInstance {
    val gson: Gson = GsonBuilder().apply {
        serializeNulls()
        registerTypeHierarchyAdapter(JsonField::class.java, JsonField.Adapter())
    }.create()

    inline fun <reified T> fromJson(json: String): T {
        val type = object : TypeToken<T>() {}.type
        return gson.fromJson(json, type)
    }

    fun <T> toJson(obj: T): String = gson.toJson(obj)
}

open class Jsonable {
    fun toJson() = GsonInstance.toJson(this)

    override fun toString() = toJson()
}

class JsonService<T : Request>(params: T) : Jsonable() {
    @SerializedName("serviceid")
    private var serviceID = params.id
    private var target = ""

    @SerializedName("sessionid")
    var sessionID: String? = null

    @SerializedName("loginname")
    var loginName: String? = null
    var password: String? = null

    private var params: T = params
    private var datas: List<T> = listOf(params)
}

class Result<T : Jsonable> : Jsonable(), Iterable<T> {
    var rowcount = 0
        private set
    var page = 0
        private set
    var pagesize = 0
        private set
    var serviceid: String? = null
        private set
    var type: String? = null
        private set
    var vcode: String? = null
        private set
    var message: String? = null
        private set
    var messagedetail: String? = null
        private set

    @SerializedName("datas")
    private var data: MutableList<T>? = mutableListOf()

    fun add(d: T): Boolean {
        return if (data == null) {
            data = mutableListOf(d)
            true
        } else {
            data!!.add(d)
        }
    }

    operator fun get(index: Int): T = data?.get(index) ?: throw Exception("datas is null")

    fun size(): Int = data?.size ?: 0

    fun empty(): Boolean = size() == 0

    override fun iterator(): Iterator<T> = iterator {
        data?.forEach {
            yield(it)
        }
    }

    companion object {
        inline fun <reified T : Jsonable> fromJson(json: String): Result<T> {
            val type = TypeToken.getParameterized(Result::class.java, T::class.java).type
            return GsonInstance.gson.fromJson(json, type)
        }
    }
}