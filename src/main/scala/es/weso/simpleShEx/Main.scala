package es.weso.simpleShEx

import es.weso.rdf.rdf4j.RDFAsRDF4jModel
import es.weso.shapeMaps.ShapeMap
import es.weso.shex.Schema
import es.weso.shex.validator.Validator
import org.eclipse.rdf4j.rio.{RDFFormat, Rio}

import scala.io.Source

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
    shapeMap <- ShapeMap.fromString(mappingsDeclaration.getLines().mkString,
                                    ShapeMap.defaultFormat,
                                    None,
                                    rdf.getPrefixMap(),
                                    shex.prefixMap)
    fixedShapeMap <- ShapeMap.fixShapeMap(shapeMap, rdf, rdf.getPrefixMap, shex.prefixMap)
    result        <- Validator.validate(shex, fixedShapeMap, rdf)
  } yield result

  validation match {
    case Right(result) =>
      println(result.toJson.spaces2)

    case Left(e) =>
      println(s"Error: $e")
  }
}
