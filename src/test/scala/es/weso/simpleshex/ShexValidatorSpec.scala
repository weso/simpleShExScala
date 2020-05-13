package es.weso.simpleshex

import es.weso.rdf.rdf4j.RDFAsRDF4jModel
import es.weso.shapeMaps.ShapeMap
import es.weso.shex.Schema
import es.weso.shex.validator.Validator
import org.eclipse.rdf4j.rio.{RDFFormat, Rio}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import Either2IO._

import scala.io.Source
import es.weso.shex.ResolvedSchema

class ShexValidatorSpec extends AnyFlatSpec with Matchers {
  "The Shex Validator" should "validate triples, given the mappings and the shex definition" in {
    val rdfContent          = Source.fromFile("examples/user.ttl")
    val shexDefinition      = Source.fromFile("examples/user.shex")
    val mappingsDeclaration = Source.fromFile("examples/user.sm")

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
      resolvedSchema <- ResolvedSchema.resolve(shex,None)
      result        <- Validator.validate(resolvedSchema, fixedShapeMap, rdf)
      resultMap <- result.toResultShapeMap
    } yield resultMap

    validation.attempt.unsafeRunSync match {
      case Right(result) =>
        result.noSolutions should be(false)

      case Left(e) =>
        fail(e)
    }
  }


}
