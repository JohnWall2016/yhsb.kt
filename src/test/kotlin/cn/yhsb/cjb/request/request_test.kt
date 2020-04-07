package cn.yhsb.cjb.request

import cn.yhsb.cjb.service.Session
import org.junit.Test

class RequestTest {
    @Test
    fun testCbxxRequest() {
        Session.autoLogin {
            it.sendService(CbxxRequest("430321197502110542"))
            val res = it.getResult<CbxxRequest.Cbxx>()
            for (cbxx in res) {
                println("${cbxx.name} ${cbxx.idcard} ${cbxx.jbState} ${cbxx.jbKind}")
            }
        }
    }

    @Test
    fun testJfxxRequest() {
        Session.autoLogin { session ->
            session.sendService(JfxxRequest("430321197502110542"))
            val res = session.getResult<JfxxRequest.Jfxx>()
            res.sortedByDescending { it.year }.forEach {
                println("${it.year} ${it.type} ${it.item} ${it.amount}")
            }
        }
    }
}