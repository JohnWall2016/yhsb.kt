package cn.yhsb.qb.service

import cn.yhsb.base.HttpRequest
import cn.yhsb.base.HttpSocket
import cn.yhsb.qb.Config
import java.nio.charset.Charset

class Session(host: String, port: Int, private val userID: String, private val password: String)
    : HttpSocket(host, port, Charset.forName("GBK")) {

    private val cookies = mutableMapOf<String, String>()

    private fun createRequest(): HttpRequest {
        val request = HttpRequest("/sbzhpt/MainServlet", "POST", charset).apply {
            addHeader("SOAPAction", "mainservlet")
            addHeader("Content-Type", "text/html;charset=GBK")
            addHeader("Host", url)
            addHeader("Connection", "keep-alive")
            addHeader("Cache-Control", "no-cache")
        }
        if (cookies.isNotEmpty()) {
            val cookie = cookies.map { "${it.key}=${it.value}" }.joinToString(";")
            request.addHeader("Cookie", cookie)
        }
        return request
    }

    private fun buildRequest(content: String): HttpRequest {
        val request = createRequest()
        request.addBody(content)
        return request
    }

    private fun request(content: String) {
        val request = buildRequest(content)
        write(request.toByteArray())
    }

    fun toService(env: InEnvelop): String {
        val doc = XmlUtil.xmlDocument(env, "soap:Envelope", NSSoapEnvelope)
        return doc.transfromToString("<?xml version=\"1.0\" encoding=\"GBK\"?>")
    }

    fun toService(param: ParameterWithFunID): String {
        return toService(InEnvelop(InSystem().apply {
            this.user = userID
            this.password = this@Session.password
            funID = param.funID
        }, param))
    }

    fun sendService(env: InEnvelop) = request(toService(env))

    fun sendService(param: ParameterWithFunID) = request(toService(param))

    inline fun <reified T : Result, reified S : Result> responseFrom(xml: String) = OutEnvelope.fromXml<T, S>(xml)

    inline fun <reified T : Result, reified S : Result> receiveResponse() = OutEnvelope.fromXml<T, S>(readBody())

    inline fun <reified T : Result> resultFrom(xml: String): T? = responseFrom<OutHeader, T>(xml).result

    inline fun <reified T : Result> getResult() = resultFrom<T>(readBody())


    fun login(): String {
        sendService(Login())
        val header = readHeader()
        if (header.contains("set-cookie")) {
            header["set-cookie"]?.forEach { cookie ->
                val reg = Regex("([^=]+?)=(.+?);")
                val m = reg.find(cookie)
                if (m != null) {
                    cookies[m.groupValues[1]] = m.groupValues[2]
                }
            }
        }
        return readBody(header)
    }

    fun logout() {
    }

    companion object {
        fun new(userID: String = "sqb"): Session =
                Session(Config.Service.host, Config.Service.port,
                        (Config.Service.users[userID] ?: error("invalid user")).id,
                        (Config.Service.users[userID] ?: error("invalid user")).password)

        fun <T> autoLogin(userID: String = "sqb", action: Session.() -> T): T {
            new(userID).use {
                it.login()
                try {
                    return it.action()
                } finally {
                    it.logout()
                }
            }
        }
    }
}