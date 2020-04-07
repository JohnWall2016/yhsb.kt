package cn.yhsb.cjb.service

import com.google.gson.annotations.SerializedName

open class Request(@field:Transient val id: String) : Jsonable()

open class PageRequest(id: String, page: Int = 1, pageSize: Int = 15) : Request(id) {
    private val page = page
    private val pagesize = pageSize
    private val filtering = mutableListOf<Any>()
    private val sorting = mutableListOf<Any>()
    private val totals = mutableListOf<Any>()

    fun setFiltering(filtering: Map<String, String>) = this.filtering.add(filtering)

    fun setSorting(sorting: Map<String, String>) = this.sorting.add(sorting)

    fun setTotals(totals: Map<String, String>) = this.totals.add(totals)
}


class SysLogin(
        @field:SerializedName("username") val userName: String,
        @field:SerializedName("passwd") val password: String
) : Request("syslogin")