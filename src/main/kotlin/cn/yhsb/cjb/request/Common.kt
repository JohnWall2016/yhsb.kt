package cn.yhsb.cjb.request

import cn.yhsb.cjb.service.JsonField

/**
 * 参保状态
 */
class CBState : JsonField() {
    override val name: String
        get() = when (value) {
            "0" -> "未参保"
            "1" -> "正常参保"
            "2" -> "暂停参保"
            "4" -> "终止参保"
            else -> "未知值: $value"
        }
}

/**
 * 缴费状态
 */
class JFState : JsonField() {
    override val name: String
        get() = when (value) {
            "1" -> "参保缴费"
            "2" -> "暂停缴费"
            "3" -> "终止缴费"
            else -> "未知值: $value"
        }
}

/**
 * 居保状态
 */
interface JBState {
    val cbState: CBState?
    val jfState: JFState?
    val jbState: String
        get() {
            val jfState = jfState?.value
            val cbState = cbState?.value
            return when (jfState) {
                "1" -> when (cbState) {
                    "1" -> "正常缴费人员"
                    else -> "未知类型参保缴费人员: $cbState"
                }
                "2" -> when (cbState) {
                    "2" -> "暂停缴费人员"
                    else -> "未知类型暂停缴费人员: $cbState"
                }
                "3" -> when (cbState) {
                    "1" -> "正常待遇人员"
                    "2" -> "暂停待遇人员"
                    "4" -> "终止参保人员"
                    else -> "未知类型终止缴费人员: $cbState"
                }
                "0", null -> "未参保"
                else -> "未知类型人员: $jfState, $cbState"
            }
        }
}


/**
 * 参保身份
 */
class JBKind : JsonField() {
    override val name: String
        get() = when (value) {
            "011" -> "普通参保人员"
            "021" -> "残一级"
            "022" -> "残二级"
            "031" -> "特困一级"
            "051" -> "贫困人口一级"
            "061" -> "低保对象一级"
            "062" -> "低保对象二级"
            else -> "未知身份类型: $value"
        }
}

val xzqhMap = mapOf(
        "43030200" to "代发虚拟乡镇",
        "43030201" to "长城乡",
        "43030202" to "昭潭街道",
        "43030203" to "先锋街道",
        "43030204" to "万楼街道",
        "43030205" to "（原）鹤岭镇",
        "43030206" to "楠竹山镇",
        "43030207" to "姜畲镇",
        "43030208" to "鹤岭镇",
        "43030209" to "城正街街道",
        "43030210" to "雨湖路街道",
        "43030211" to "（原）平政路街道",
        "43030212" to "云塘街道",
        "43030213" to "窑湾街道",
        "43030214" to "（原）窑湾街道",
        "43030215" to "广场街道",
        "43030216" to "（原）羊牯塘街道)"
)

val jbKindMap = mapOf(
        "贫困人口一级" to "051",
        "特困一级" to "031",
        "低保对象一级" to "061",
        "低保对象二级" to "062",
        "残一级" to "021",
        "残二级" to "022"
)