package cn.yhsb.cjb.request

import cn.yhsb.cjb.service.Jsonable
import cn.yhsb.cjb.service.PageRequest
import com.google.gson.annotations.SerializedName

/**
 * 参保审核查询
 */
class CbshRequest(startDate: String = "", endDate: String = "", state: String = "1")
    : PageRequest("cbshQuery", 1, 500) {
    var aaf013 = ""
    var aaf030 = ""
    var aae011 = ""
    var aae036 = ""
    var aae036s = ""
    var aae014 = ""
    var aac009 = ""
    var aac002 = ""
    var aac003 = ""
    var sfccb = ""

    @SerializedName("aae015")
    var startDate = startDate // "2020-03-27"

    @SerializedName("aae015s")
    var endDate = endDate

    /** 审核状态  */
    @SerializedName("aae016")
    var state = state

    class Cbsh : Jsonable() {
        @SerializedName("aac002")
        var idcard: String? = null

        @SerializedName("aac003")
        var name: String? = null

        @SerializedName("aac006")
        var birthDay: String? = null
    }
}