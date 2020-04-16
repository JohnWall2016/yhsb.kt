package cn.yhsb.qb.service

import org.junit.Test

class SessionTest {
    @Test
    fun testSession() {
        Session.new().use {
            val ret = it.login()
            println(ret)
            it.populateResult(ret, Login.Result()).let(::println)
        }
    }

    @Test
    fun testSncbry() {
        Session.autoLogin {
            sendService(SncbryQuery("430302195806251012"))
            val result = fetchResult(SncbryQuery.Result())
            println(result)
            for (info in result.queryList)
                println("${info.idcard} ${info.name} ${info.sbState} ${info.cbState} " +
                        "${info.jfKind} ${info.agency} ${info.dwbh}")
        }
    }
}