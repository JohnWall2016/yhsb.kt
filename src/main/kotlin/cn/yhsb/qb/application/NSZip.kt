package cn.yhsb.qb.application

import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun main(args: Array<String>) {
    val nszl1 = "E:\\企保年审\\2020年年审资料\\430302024322单位年审资料\\2020年度雨湖区企业职工基本养老保险缴费基数申报表.xls"
    val nszl2 = "E:\\企保年审\\2020年年审资料\\430302024322单位年审资料\\填报说明.docx"
    val dwdir = "E:\\企保年审\\2020年单位数据"
    val outdir = "E:\\企保年审\\2020年年审打包数据"

    val reg = Regex("""((\d+)-(.+?))-""")
    Files.list(Paths.get(dwdir)).forEach { file: Path? ->
        println(file)
        val m = reg.find(file?.fileName.toString())
        if (m != null) {
            val zipFile = Paths.get(outdir, "${m.groupValues[1]}-2020年年审资料.zip")
            println("生成压缩包: $zipFile")
            ZipOutputStream(Files.newOutputStream(zipFile)).use { zip ->
                for (f in listOf(nszl1, nszl2, file.toString())) {
                    val p = Paths.get(f)
                    Files.newInputStream(p).use { inStream ->
                        val entry = ZipEntry(p.fileName.toString())
                        zip.putNextEntry(entry)
                        inStream.copyTo(zip)
                    }
                }
            }
        } else {
            throw Exception("文件名有误: $file")
        }
    }
}