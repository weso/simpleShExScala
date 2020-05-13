package es.weso.simpleshex

import es.weso.rdf.rdf4j.RDFAsRDF4jModel
import es.weso.shapeMaps.ShapeMap
import es.weso.shex.Schema
import es.weso.shex.validator.Validator
import org.eclipse.rdf4j.rio.{RDFFormat, Rio}
import Either2IO._
import cats.effect._
import scala.io.Source
import es.weso.shex.ResolvedSchema

object Main extends App {
  if (args.length != 3) {
    println("Usage is: sbt \"run <rdfFile> <shexFile> <mappingsFile>\"")
    sys.exit(1)
  }
  val rdfContent          = Source.fromFile(args(0))
  val shexDefinition      = Source.fromFile(args(1))
  val mappingsDeclaration = Source.fromFile(args(2))

  val model = Rio.parse(rdfContent.bufferedReader(), "", RDFFormat.TRIG)
  val rdf   = RDFAsRDF4jModel(model)

  val validation = for {
    shex <- Schema.fromString(shexDefinition.getLines.mkString, "ShexC")
    shapeMap <- either2IO(ShapeMap.fromString(mappingsDeclaration.getLines().mkString,
                                    ShapeMap.defaultFormat,
                                    None,
                                    rdf.getPrefixMap(),
                                    shex.prefixMap))
    fixedShapeMap <- ShapeMap.fixShapeMap(shapeMap, rdf, rdf.getPrefixMap, shex.prefixMap)
    resolvedSchema <- ResolvedSchema.resolve(shex, None)
    result        <- Validator.validate(resolvedSchema, fixedShapeMap, rdf)
    resultMap <- result.toResultShapeMap
  } yield resultMap

  validation.attempt.unsafeRunSync match {
    case Right(result) =>
      println(result.toJson.spaces2)

    case Left(e) =>
      println(s"Error: ${e.getMessage()}")
  }

}
