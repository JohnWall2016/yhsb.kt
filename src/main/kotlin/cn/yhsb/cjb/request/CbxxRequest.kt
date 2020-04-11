package cn.yhsb.cjb.request

import cn.yhsb.cjb.service.Jsonable
import cn.yhsb.cjb.service.Request
import com.google.gson.annotations.SerializedName

/**
 * 省内参保信息查询
 */
class CbxxRequest(idcard: String) : Request("executeSncbxxConQ") {
    @SerializedName("aac002")
    val idcard = idcard

    class Cbxx : Jsonable(), JBState {
        /** 个人编号  */
        @SerializedName("aac001")
        var pid = 0

        /** 身份证号码 */
        @SerializedName("aac002")
        var idcard: String? = null

        @SerializedName("aac003")
        var name: String? = null

        @SerializedName("aac006")
        var birthDay: String? = null

        @SerializedName("aac008")
        override var cbState: CBState? = null

        @SerializedName("aac031")
        override var jfState: JFState? = null

        /** 参保时间 */
        @SerializedName("aac049")
        var cbDate = 0

        /** 参保身份编码 */
        @SerializedName("aac066")
        var jbKind: JBKind? = null

        /** 社保机构 */
        @SerializedName("aaa129")
        var agency: String? = null

        /** 经办时间 */
        @SerializedName("aae036")
        var dealDate: String? = null

        /** 行政区划编码 */
        @SerializedName("aaf101")
        var xzqhCode: String? = null

        /** 村组名称 */
        @SerializedName("aaf102")
        var czName: String? = null

        /** 村社区名称 */
        @SerializedName("aaf103")
        var csName: String? = null

        fun invalid(): Boolean = idcard.isNullOrEmpty()

        fun valid(): Boolean = !invalid()
    }
}