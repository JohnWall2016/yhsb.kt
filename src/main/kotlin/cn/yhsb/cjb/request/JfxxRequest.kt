package cn.yhsb.cjb.request

import cn.yhsb.cjb.service.JsonField
import cn.yhsb.cjb.service.Jsonable
import cn.yhsb.cjb.service.PageRequest
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * 省内参保信息查询 - 缴费信息
 */
class JfxxRequest(idcard: String) : PageRequest("executeSncbqkcxjfxxQ", 1, 500) {
    @SerializedName("aac002")
    val idcard = idcard

    class Jfxx : Jsonable() {
        /** 缴费年度  */
        @SerializedName("aae003")
        var year: Int? = null

        /** 备注  */
        @SerializedName("aae013")
        var memo: String? = null

        /** 金额  */
        @SerializedName("aae022")
        var amount: BigDecimal? = null

        /** 缴费类型  */
        class Type : JsonField() {
            override val name: String get() {
                return when (value) {
                    "10" -> "正常应缴"
                    "31" -> "补缴"
                    else -> "未知值: $value"
                }
            }
        }

        @SerializedName("aaa115")
        var type: Type? = null

        /** 缴费项目  */
        class Item : JsonField() {
            override val name: String get() {
                return when (value) {
                    "1" -> "个人缴费"
                    "3" -> "省级财政补贴"
                    "4" -> "市级财政补贴"
                    "5" -> "县级财政补贴"
                    "11" -> "政府代缴"
                    else -> "未知值: $value"
                }
            }
        }

        @SerializedName("aae341")
        var item: Item? = null

        /** 缴费方式  */
        class Method : JsonField() {
            override val name: String get() {
                return when (value) {
                    "2" -> "银行代收"
                    "3" -> "经办机构自收"
                    else -> "未知值: $value"
                }
            }
        }

        @SerializedName("aab033")
        var method: Method? = null

        /** 划拨日期  */
        @SerializedName("aae006")
        var paidOffDay: String? = null

        /** 是否已划拨  */
        fun paidOff(): Boolean {
            return paidOffDay != null
        }

        /** 社保机构  */
        @SerializedName("aaa027")
        var agency: String? = null

        /** 行政区划代码  */
        @SerializedName("aaf101")
        var xzqh: String? = null
    }
}