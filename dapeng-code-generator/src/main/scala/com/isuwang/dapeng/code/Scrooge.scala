package com.isuwang.dapeng.code

import java.io.{File, FileNotFoundException, FilenameFilter}
import javax.annotation.processing.FilerException

import com.isuwang.dapeng.code.generator.ScalaGenerator

//import com.isuwang.dapeng.code.generator.scala.ScalaGenerator
import com.isuwang.dapeng.code.generator.{JavaGenerator, JavascriptGenerator, JsonGenerator, MetadataGenerator}
import com.isuwang.dapeng.code.parser.ThriftCodeParser

/**
  * @author craneding
  * @date 15/5/11
  */
object Scrooge {

  val help =
    """-----------------------------------------------------------------------
      | args: -gen metadata,js,json file
      | Scrooge [options] file
      | Options:
      |   -out dir    Set the output location for generated files.
      |   -gen STR    Generate code with a dynamically-registered generator.
      |               STR has the form language[val1,val2,val3].
      |               Keys and values are options passed to the generator.
      |   -v version  Set the version of the Service generated.
      |   -in dir     Set input location of all Thrift files.
      |   -all        Generate all structs and enums
      |
      | Available generators (and options):
      |   metadata
      |   scala
      |   java
      |-----------------------------------------------------------------------
    """.stripMargin

  def main(args: Array[String]) {

    println(s"scrooge:${args.mkString(" ")}")

    if (args.length <= 0) {
      println(help);
      return
    }

    var outDir: String = null
    var inDir: String = null
    var resources: Array[String] = null
    var language: String = ""
    var version: String = null
    var generateAll: Boolean = false

    try {
      for (index <- 0 until args.length) {

        args(index) match {
          case "-gen" =>
            //获取languages
            if (index + 1 < args.length) language = args(index + 1)
          case "-out" =>
            //获取到outDir
            if (index + 1 < args.length) outDir = args(index + 1)
            val file = new File(outDir)
            if (!file.exists())
              file.createNewFile()

            if (file.isFile) {
              file.delete()
              throw new FilerException(s"File[${outDir}] is not a directory")
            }
          case "-in" =>
            if (index + 1 < args.length) inDir = args(index + 1)
            val file = new File(inDir)
            if (!file.exists())
              file.createNewFile()

            if (file.isFile) {
              file.delete()
              throw new FilerException(s"File[${inDir}] is not a directory")
            }
          case "-help" => println(help); return
          case "-v" => if (index + 1 < args.length) version = args(index + 1)
          case "-all" => generateAll = true
          case _ =>
        }
      }


      if (inDir != null) {

        resources = new File(inDir).listFiles(new FilenameFilter {
          override def accept(dir: File, name: String): Boolean = name.endsWith(".thrift")
        }).map(file => file.getAbsolutePath)

      } else {
        //获取到resource
        resources = args(args.length - 1).split(",")
        resources.foreach { str =>
          val file = new File(str)
          if (!file.exists())
            throw new FileNotFoundException(s"File[${str}] is not found")
          else if (new File(str).isDirectory || !str.endsWith(".thrift"))
            throw new FilerException(s"File[${str}] is not a *.thrift")
        }
      }

      var isUpdated = false
      var scalaXmlCount = 0

      val file = new File(resources(0))
      val parent = file.getParentFile.getParentFile

      parent.listFiles().foreach {
        xmlFile =>
          if (xmlFile.getName.endsWith(".xml") && xmlFile.lastModified() < file.lastModified()) {
            isUpdated = true
          }
          if (xmlFile.getName.contains("scala")) {
            scalaXmlCount += 1
          }
      }
      if (parent.listFiles().length <= 1 || (language.equals("scala") && scalaXmlCount == 0)) {
        isUpdated = true
      }

      if (outDir == null) // 如果输出路径为空,则默认为当前目录
        outDir = System.getProperty("user.dir")

      if (resources != null && language != "" && isUpdated) {

        val parserLanguage = if (language == "scala") "scala" else "java"
        val services = new ThriftCodeParser(parserLanguage).toServices(resources, version)
        val structs = if (generateAll) new ThriftCodeParser(parserLanguage).getAllStructs(resources) else null
        val enums = if (generateAll) new ThriftCodeParser(parserLanguage).getAllEnums(resources) else null

        language match {
          case "metadata" => new MetadataGenerator().generate(services, outDir)
          case "js" => new JavascriptGenerator().generate(services, outDir)
          case "json" => new JsonGenerator().generate(services, outDir)
          case "java" => new JavaGenerator().generate(services, outDir, generateAll, structs, enums)
          case "scala" => new ScalaGenerator().generate(services, outDir, generateAll, structs, enums)
        }

      } else if (resources == null || language == "") {
        throw new RuntimeException("resources is null or language is null")
      }
    }
    catch {
      case e: Exception =>
        e.printStackTrace()

        println(s"Error: ${e.getMessage}")
    }
  }

  def failed(): Unit = {

  }

}
