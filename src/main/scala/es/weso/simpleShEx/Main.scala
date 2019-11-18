package es.weso.simpleShEx

import org.rogach.scallop._
import org.rogach.scallop.exceptions._
import com.typesafe.scalalogging._
import es.weso.rdf.nodes._
import es.weso.rdf.RDFReader
import es.weso.rdf.rdf4j.RDFAsRDF4jModel
import es.weso.utils.FileUtils
import es.weso.shex.Schema
import es.weso.shapeMaps.ShapeMap
import es.weso.shex.validator.Validator
import es.weso.rdf.PrefixMap
import cats.implicits._

object Main extends App with LazyLogging {
  val relativeBase: Option[IRI] = Some(IRI("internal://base/"))
  try {
      run(args)
    } catch {
      case (e: Exception) => {
        println(s"Error: ${e.getMessage}")
      }
    }

  private def run(args: Array[String]): Unit = {
    val opts = new MainOpts(args, errorDriver)
    opts.verify()
    if (args.length==0) return opts.printHelp()

    val either = for {
      rdf <- getData(opts.data(), opts.dataFormat()).leftMap(e => s"Error reading RDF: $e")
      shex <- getShEx(opts.shex(), opts.shexFormat()).leftMap(e => s"Error reading ShEx: $e")
      shapeMap <- getShapeMap(opts.shapeMap(), opts.shapeMapFormat(), rdf.getPrefixMap(), shex.prefixMap).leftMap(e => s"Error reading ShapeMap: $e")
      fixedShapeMap <- ShapeMap.fixShapeMap(shapeMap, rdf, rdf.getPrefixMap, shex.prefixMap)
      result <- Validator.validate(shex, fixedShapeMap, rdf)
    } yield {
      result.toJson.spaces2
    }
    either.fold(
      e => println(s"Error: $e"), 
      v => println(s"Result: $v")
    )
  }

  private def errorDriver(e: Throwable, scallop: Scallop) = e match {
    case Help(s) => {
      println(s"Help: $s")
      scallop.printHelp
      sys.exit(0)
    }
    case _ => {
      println(s"Error: ${e.getMessage}")
      scallop.printHelp
      sys.exit(1)
    }
  }

  private def getData(dataFile: String, dataFormat: String): Either[String, RDFReader] = for {
    contents <- FileUtils.getContents(dataFile)
    rdf <- RDFAsRDF4jModel.fromChars(contents,dataFormat,None)
  } yield rdf 

  private def getShEx(shexFile: String, shExFormat: String): Either[String, Schema] = for {
    contents <- FileUtils.getContents(shexFile)
    schema <- Schema.fromString(contents, shExFormat, None)
  } yield schema

  private def getShapeMap(shapeMapFile: String, shapeMapFormat: String, 
                          rdfPrefixMap: PrefixMap, shexPrefixMap: PrefixMap
                          ): Either[String, ShapeMap] = for {
    contents <- FileUtils.getContents(shapeMapFile)
    shapeMap <- ShapeMap.fromString(contents.toString,shapeMapFormat,None, rdfPrefixMap,shexPrefixMap)
  } yield shapeMap
  

}

