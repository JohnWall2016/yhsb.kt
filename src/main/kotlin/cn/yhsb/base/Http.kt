package cn.yhsb.base

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.net.Socket
import java.nio.charset.Charset
import kotlin.sequences.iterator

open class HttpSocket(
        host: String,
        port: Int,
        val charset: Charset = Charsets.UTF_8
) : Closeable {
    private val socket = Socket(host, port)
    private val ostream = socket.getOutputStream()
    private val istream = socket.getInputStream()

    val url = "$host:$port"

    protected fun write(bytes: ByteArray) = ostream.write(bytes)

    fun write(string: String) = write(string.toByteArray(charset))

    private fun read() = istream.read()

    private fun read(len: Int): ByteArray? {
        var r = read()
        if (r == -1) return null
        return ByteArrayOutputStream(len).use {
            it.write(r)
            for (i in 2..len) {
                r = read()
                if (r == -1) break
                it.write(r)
            }
            it.toByteArray()
        }
    }

    private fun readLine(): String {
        return ByteArrayOutputStream(128).use {
            end@ while (true) {
                when (val c = read()) {
                    -1 -> break@end
                    0x0D -> {// \r
                        when (val n = read()) {
                            -1 -> {
                                it.write(c)
                                break@end
                            }
                            0x0A -> // \n
                                break@end
                            else -> {
                                it.write(c)
                                it.write(n)
                            }
                        }
                    }
                    else -> it.write(c)
                }
            }
            String(it.toByteArray(), charset)
        }
    }

    fun readHeader(): HttpHeader {
        val header = HttpHeader()
        while (true) {
            val line = readLine()
            if (line == "") break
            val i = line.indexOf(':')
            if (i > 0) {
                val key = line.substring(0, i).trim()
                val value = line.substring(i + 1).trim()
                header.add(key, value)
            }
        }
        return header
    }

    fun readBody(header: HttpHeader? = null): String {
        return ByteArrayOutputStream().use {
            val hd = header ?: readHeader()
            // header.forEach(::println)
            if (hd["Transfer-Encoding"]?.contains("chunked") == true) {
                while (true) {
                    val len = readLine().toInt(16)
                    if (len <= 0) {
                        readLine()
                        break
                    }
                    it.write(read(len) ?: throw Exception("The read length is short"))
                    readLine()
                }
            } else {
                val length = hd["Content-Length"]?.get(0)
                if (length != null) {
                    val len = length.toInt(10)
                    it.write(read(len) ?: throw Exception("The read length is short"))
                } else {
                    throw Exception("Unsupported transfer mode")
                }
            }
            String(it.toByteArray(), charset)
        }
    }

    override fun close() {
        socket.use {
            ostream.use {
                istream.use { }
            }
        }
    }

    fun getHttp(path: String, charset: Charset = Charsets.UTF_8): String {
        val request = HttpRequest(path, method = "GET", charset = charset).apply {
            addHeader("Host", url)
            addHeader("Connection", "keep-alive")
            addHeader("Cache-Control", "max-age=0")
            addHeader("Upgrade-Insecure-Requests", "1")
            addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
            addHeader("Accept", "text/html,application/xhtml+xml,application/xml;" +
                    "q=0.9,image/webp,image/apng,*/*;q=0.8")
            addHeader("Accept-Encoding", "gzip, deflate")
            addHeader("Accept-Language", "zh-CN,zh;q=0.9")
        }
        write(request.toByteArray())
        return readBody()
    }
}

class HttpHeader : Iterable<Map.Entry<String, List<String>>> {
    private val headers = mutableMapOf<String, MutableList<String>>()

    fun add(name: String, value: String) {
        val key = name.toLowerCase()
        if (!headers.containsKey(key)) {
            headers[key] = mutableListOf()
        }
        headers[key]?.add(value)
    }

    fun addAll(header: HttpHeader) = headers.putAll(header.headers)

    operator fun set(key: String, values: MutableList<String>) {
        headers[key.toLowerCase()] = values
    }

    operator fun get(key: String) = headers[key.toLowerCase()]

    fun contains(key: String) = headers.containsKey(key)

    override fun iterator() = iterator {
        headers.forEach {
            yield(it)
        }
    }
}

class HttpRequest(
        private val path: String,
        private val method: String = "GET",
        private val charset: Charset = Charsets.UTF_8,
        header: HttpHeader? = null
) {
    private val header = HttpHeader().apply {
        if (header != null) addAll(header)
    }
    private val body = ByteArrayOutputStream()

    fun addHeader(key: String, value: String) = header.add(key, value)

    fun addBody(str: String) = body.write(str.toByteArray(charset))

    fun toByteArray(): ByteArray {
        val buf = ByteArrayOutputStream(512)

        fun ByteArrayOutputStream.write(s: String, cs: Charset = charset) = write(s.toByteArray(cs))

        buf.write("$method $path HTTP/1.1\r\n")
        header.forEach { entry ->
            entry.value.forEach { value ->
                buf.write("${entry.key}: $value\r\n")
            }
        }
        if (body.size() > 0) {
            buf.write("content-length: ${body.size()}\r\n")
        }
        buf.write("\r\n")
        if (body.size() > 0) {
            body.writeTo(buf)
        }
        return buf.toByteArray()
    }
}