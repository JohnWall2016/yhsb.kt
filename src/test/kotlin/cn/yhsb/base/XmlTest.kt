package cn.yhsb.base

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import org.junit.Test

class XmlTest {
    @Test
    fun testXml() {
        val xml = """
<out:business xmlns:out=""http://www.molss.gov.cn/"">
    <result result=""${'"'}${'"'} />
    <resultset name=""querylist"">
        <row aac003=""徐X"" rown=""1"" aac008=""2"" aab300=""XXXXXXX服务局"" sac007=""101"" aac031=""3"" aac002=""43030219XXXXXXXXXX"" />
    </resultset>
    <result row_count=""1"" />
    <result querysql=""select * from 
  from ac01_css a, ac02_css b
 where a.aac001 = b.aac001) where ( aac002 = &apos;43030219XXXXXXXX&apos;) and 1=1) row_ where rownum &lt;(501)) where rown &gt;=(1) "" />
</out:business>"""

        @JacksonXmlRootElement(localName = "business")
        class Business {

        }
    }
}