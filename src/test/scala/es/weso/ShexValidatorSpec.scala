package es.weso

import es.weso.rdf.rdf4j.RDFAsRDF4jModel
import es.weso.shapeMaps.ShapeMap
import es.weso.shex.Schema
import es.weso.shex.validator.Validator
import org.eclipse.rdf4j.rio.{RDFFormat, Rio}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

class ShexValidatorSpec extends AnyFlatSpec with Matchers {
  "The Shex Validator" should "validate triples, given the mappings and the shex definition" in {
    val rdfContent          = Source.fromFile("examples/user.ttl")
    val shexDefinition      = Source.fromFile("examples/user.shex")
    val mappingsDeclaration = Source.fromFile("examples/user.sm")

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
        result.noSolutions should be(false)

      case Left(e) =>
        fail(e)
    }
  }
}
