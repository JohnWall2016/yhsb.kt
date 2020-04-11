package cn.yhsb.cjb.request

import cn.yhsb.cjb.service.Session
import org.junit.Test

class RequestTest {
    @Test
    fun testCbxxRequest() {
        Session.autoLogin {
            sendService(CbxxRequest("430321197502110542"))
            val res = getResult<CbxxRequest.Cbxx>()
            for (cbxx in res) {
                println("${cbxx.name} ${cbxx.idcard} ${cbxx.jbState} ${cbxx.jbKind}")
            }
        }
    }

    @Test
    fun testGrinfoRequest() {
        Session.autoLogin {
            sendService(GrinfoRequest("430321197502110542"))
            val res = getResult<GrinfoRequest.Grinfo>()
            for (cbxx in res) {
                println("${cbxx.name} ${cbxx.idcard} ${cbxx.jbState}")
            }
        }
    }

    @Test
    fun testJfxxRequest() {
        Session.autoLogin {
            sendService(JfxxRequest("430321197502110542"))
            val res = getResult<JfxxRequest.Jfxx>()
            res.sortedByDescending { it.year }.forEach {
                println("${it.year} ${it.type} ${it.item} ${it.amount}")
            }
        }
    }
}