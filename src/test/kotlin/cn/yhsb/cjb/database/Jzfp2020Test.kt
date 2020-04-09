package cn.yhsb.cjb.database

import org.jetbrains.exposed.sql.select
import org.junit.Test

typealias tb = FPHistoryData

class Jzfp2020Test {
    @Test
    fun testJzfp2020() {
        transaction {
            FPHistoryData.select {
                FPHistoryData.idcard eq "430321200107031259"
            }.forEach {
                println("${it[tb.name]} ${it[tb.idcard]} ${it[tb.jbrdsf]}")
            }
        }
    }
}