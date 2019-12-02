package es.weso.simpleShEx

import org.rogach.scallop._
import org.rogach.scallop.exceptions._
import es.weso.rdf.nodes._
import cats.data.EitherT
import cats.effect._
import es.weso.rdf.RDFReader
import es.weso.rdf.rdf4j.RDFAsRDF4jModel
import es.weso.utils.FileUtils
import es.weso.shex.Schema
import es.weso.shapeMaps.ShapeMap
import es.weso.shex.validator.Validator
import es.weso.rdf.PrefixMap
// import cats.implicits._

object Main extends IOApp {
  val relativeBase: Option[IRI] = Some(IRI("internal://base/"))

  def run(args: List[String]): IO[ExitCode] = {
    val opts = new MainOpts(args, errorDriver)
    opts.verify()

    if (args.length==0) for {
      _ <- IO { opts.printHelp() }
    } yield ExitCode.Error
    else {
     val either = for {
     rdf <- getData(opts.data(), opts.dataFormat()).leftMap(e => s"Error reading RDF: $e")
     shex <- getShEx(opts.shex(), opts.shexFormat()).leftMap(e => s"Error reading ShEx: $e")
     shapeMap <- getShapeMap(opts.shapeMap(), opts.shapeMapFormat(), rdf.getPrefixMap(), shex.prefixMap).leftMap(e => s"Error reading ShapeMap: $e")
     fixedShapeMap <- EitherT.fromEither[IO](ShapeMap.fixShapeMap(shapeMap, rdf, rdf.getPrefixMap, shex.prefixMap))
     result <- EitherT.fromEither[IO](Validator.validate(shex, fixedShapeMap, rdf))
     } yield {
      result.toJson.spaces2
     }

    for {
      e <- either.value 
      _ <- e.fold(e => IO(println(s"Error: $e")), v => IO(println(s"Result: $v")))
    } yield ExitCode.Success 
  }
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

  private def getData(dataFile: String, dataFormat: String): EitherT[IO,String, RDFReader] = for {
    contents <- FileUtils.getContents(dataFile)
    rdf <- RDFAsRDF4jModel.fromChars(contents,dataFormat,None)
  } yield rdf 

  private def getShEx(shexFile: String, shExFormat: String): EitherT[IO, String, Schema] = for {
    contents <- FileUtils.getContents(shexFile)
    schema <- EitherT.fromEither[IO](Schema.fromString(contents, shExFormat, None))
  } yield schema

  private def getShapeMap(shapeMapFile: String, shapeMapFormat: String, 
                          rdfPrefixMap: PrefixMap, shexPrefixMap: PrefixMap
                          ): EitherT[IO,String, ShapeMap] = for {
    contents <- FileUtils.getContents(shapeMapFile)
    shapeMap <- EitherT.fromEither[IO](ShapeMap.fromString(contents.toString,shapeMapFormat,None, rdfPrefixMap,shexPrefixMap))
  } yield shapeMap
  

}

