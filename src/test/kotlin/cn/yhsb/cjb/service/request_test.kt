package cn.yhsb.cjb.service

import org.junit.Test

data class Person(val name: String, val age: Int) : Jsonable()

class JsonTest {
    @Test
    fun testResult() {
        val result = Result<Person>()
        result.add(Person("John", 40))
        val json = result.toJson()
        println(json)

        val result2 = Result.fromJson<Person>(json)
        println("${result.javaClass}: $result2")
        println(result2.size())
        println(result2[0])
        for (d in result2) {
            println(d)
        }

        val page = PageRequest("abc")
        println(page)
    }
}