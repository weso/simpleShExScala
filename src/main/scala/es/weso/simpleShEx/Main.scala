package es.weso.simpleShEx

import org.rogach.scallop._
import org.rogach.scallop.exceptions._
import com.typesafe.scalalogging._
import es.weso.rdf.nodes._
import es.weso.rdf.RDFReader
import es.weso.rdf.jena.RDFAsJenaModel
import java.nio.file._
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
      _ <- { println(s"RDF prefix: ${rdf.getPrefixMap()}\nShEx prefix: ${shex.prefixes}"); Right(()) }
      shapeMap <- getShapeMap(opts.shapeMap(), opts.shapeMapFormat(), rdf.getPrefixMap(), shex.prefixMap).leftMap(e => s"Error reading ShapeMap: $e")
      fixedShapeMap <- ShapeMap.fixShapeMap(shapeMap, rdf, rdf.getPrefixMap, shex.prefixMap)
      result <- Validator.validate(shex, fixedShapeMap, rdf)
    } yield {
      result.toJson.spaces2
    }
    either.fold(e => println(s"Error: $e"), v => println(s"Result: $v"))

    println(s"Bye!")
  }

  private def errorDriver(e: Throwable, scallop: Scallop) = e match {
    case Help(s) => {
      println("Help: " + s)
      scallop.printHelp
      sys.exit(0)
    }
    case _ => {
      println("Error: %s".format(e.getMessage))
      scallop.printHelp
      sys.exit(1)
    }
  }

  private def getData(dataFile: String, dataFormat: String): Either[String, RDFReader] = {
    RDFAsJenaModel.fromFile(Paths.get(dataFile).toFile(),dataFormat,None)
  }

  private def getShEx(shexFile: String, shExFormat: String): Either[String, Schema] = for {
    contents <- FileUtils.getContents(shexFile)
    schema <- Schema.fromString(contents, shExFormat, None)
  } yield schema

  private def getShapeMap(shapeMapFile: String, shapeMapFormat: String, 
                          rdfPrefixMap: PrefixMap, shexPrefixMap: PrefixMap): Either[String, ShapeMap] = for {
    contents <- FileUtils.getContents(shapeMapFile)
    shapeMap <- ShapeMap.fromString(contents.toString,shapeMapFormat,None, rdfPrefixMap,shexPrefixMap)
  } yield shapeMap
  

}

