package cn.yhsb.cjb.request

import cn.yhsb.cjb.service.Jsonable
import cn.yhsb.cjb.service.PageRequest
import com.google.gson.annotations.SerializedName

/**
 * 个人综合查询
 */
class GrinfoRequest(idcard: String) : PageRequest("zhcxgrinfoQuery") {
    /** 行政区划编码  */
    @SerializedName("aaf013")
    var xzqh = ""

    /** 村级编码  */
    @SerializedName("aaz070")
    var cjbm = ""

    var aaf101 = ""
    var aac009 = ""

    /** 参保状态: "1"-正常参保 "2"-暂停参保 "4"-终止参保 "0"-未参保  */
    @SerializedName("aac008")
    var cbzt = ""

    /** 缴费状态: "1"-参保缴费 "2"-暂停缴费 "3"-终止缴费  */
    @SerializedName("aac031")
    var jfzt = ""

    var aac006str = ""
    var aac006end = ""
    var aac066 = ""
    var aae030str = ""
    var aae030end = ""
    var aae476 = ""
    var aac058 = ""

    /** 身份证号码  */
    @SerializedName("aac002")
    var idcard = idcard

    var aae478 = ""

    @SerializedName("aac003")
    var name = ""

    class Grinfo : Jsonable(), JBState {
        /** 个人编号  */
        @SerializedName("aac001")
        var grbh = 0

        /** 身份证号码  */
        @SerializedName("aac002")
        var idcard: String? = null

        @SerializedName("aac003")
        var name: String? = null

        @SerializedName("aac006")
        var birthday = 0

        /** 参保状态  */
        @SerializedName("aac008")
        override var cbState: CBState? = null

        /** 户口所在地  */
        @SerializedName("aac010")
        var hkszd: String? = null

        /** 缴费状态  */
        @SerializedName("aac031")
        override var jfState: JFState? = null

        @SerializedName("aae005")
        var phone: String? = null

        @SerializedName("aae006")
        var address: String? = null

        @SerializedName("aae010")
        var bankcard: String? = null

        /** 村组行政区划编码  */
        @SerializedName("aaf101")
        var czqh: String? = null

        /** 村组名称  */
        @SerializedName("aaf102")
        var czmc: String? = null

        /** 村社区名称  */
        @SerializedName("aaf103")
        var csmc: String? = null

        /**
         * 所属单位名称
         */
        val dwmc: String?
            get()  = xzqhMap[czqh?.substring(0, 8)]
    }

}
