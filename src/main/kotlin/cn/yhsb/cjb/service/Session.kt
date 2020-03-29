package cn.yhsb.cjb.service

import cn.yhsb.base.HttpRequest
import cn.yhsb.base.HttpSocket
import cn.yhsb.cjb.Config

class Session(host: String, port: Int, private val userID: String, private val password: String) : HttpSocket(host, port) {
    private val cookies = mutableMapOf<String, String>()

    private fun createRequest(): HttpRequest {
        val request = HttpRequest("/hncjb/reports/crud", "POST").apply {
            addHeader("Host", url)
            addHeader("Connection", "keep-alive")
            addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            addHeader("Origin", "http://$url")
            addHeader("X-Requested-With", "XMLHttpRequest")
            addHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36")
            addHeader("Content-Type", "multipart/form-data;charset=UTF-8")
            addHeader("Referer", "http://$url/hncjb/pages/html/index.html")
            addHeader("Accept-Encoding", "gzip, deflate")
            addHeader("Accept-Language", "zh-CN,zh;q=0.8")
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

    fun toService(request: Request): String {
        val service = JsonService(request).apply {
            loginName = userID
            password = this@Session.password
        }
        return service.toJson()
    }

    fun sendService(request: Request) = request(toService(request))

    fun toService(id: String) = toService(Request(id))

    fun sendService(id: String) = request(toService(id))

    inline fun <reified T : Jsonable> fromJson(json: String): Result<T> = Result.fromJson(json)

    inline fun <reified T : Jsonable> getResult(): Result<T> {
        val result = readBody()
        return fromJson(result)
    }

    fun login(): String {
        sendService("loadCurrentUser")
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
        readBody(header)

        sendService(SysLogin(userID, password))
        return readBody()
    }

    fun logout(): String {
        sendService("syslogout")
        return readBody()
    }

    companion object {
        fun new(userID: String = "002"): Session =
                Session(Config.Service.host, Config.Service.port,
                        (Config.Service.users[userID] ?: error("invalid user")).id,
                        (Config.Service.users[userID] ?: error("invalid user")).password)

        fun autoLogin(userID: String = "002", action: (Session) -> Unit) {
            new(userID).use {
                it.login()
                action(it)
                it.logout()
            }
        }
    }
}
