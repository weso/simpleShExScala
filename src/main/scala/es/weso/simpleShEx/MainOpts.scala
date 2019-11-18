package es.weso.simpleShEx
import org.rogach.scallop._
import es.weso.rdf.rdf4j.RDFAsRDF4jModel
import es.weso.shapeMaps.ShapeMap

class MainOpts(arguments: Array[String],
               onError: (Throwable, Scallop) => Nothing
              ) extends ScallopConf(arguments) {

  lazy val shExCFormat = "ShExC"
  lazy val shExJFormat = "ShExJ"
  lazy val shExFormats = List(shExCFormat,shExJFormat).map(_.toUpperCase).distinct

  private lazy val defaultDataFormat = "TURTLE"
  private lazy val dataFormats = RDFAsRDF4jModel.availableFormats.map(_.toUpperCase).distinct
  private lazy val defaultShExFormat = shExCFormat
  private lazy val defaultShapeMapFormat = ShapeMap.defaultFormat
  private lazy val shapeMapFormats = ShapeMap.formats
              
  banner("""| simpleShExScala: 
            |""".stripMargin)

  footer("Enjoy!")

  val shex: ScallopOption[String] = opt[String](
    "shex",
    short = 's',
    default = None,
    descr = "shex file")

  val shexFormat: ScallopOption[String] = opt[String](
      "schemaFormat",
      noshort = true,
      default = Some(defaultShExFormat),
      descr = s"Schema format. Default ($defaultDataFormat) Possible values: ${shExFormats.mkString(",")}",
      validate = isMemberOf(shExFormats))

  val data: ScallopOption[String] = opt[String](
      "data",
      default = None,
      descr = "Data file(s) to validate",
      short = 'd')
  
  val dataFormat: ScallopOption[String] = opt[String](
        "dataFormat",
        default = Some(defaultDataFormat),
        descr = s"Data format. Default ($defaultDataFormat). Possible values = ${dataFormats.mkString(",")}",
        validate = isMemberOf(dataFormats),
        noshort = true)
    
  val shapeMap: ScallopOption[String] = opt[String](
      "shapeMap",
      default = None,
      short='m',
      descr = s"Shape map file")
    
  val shapeMapFormat: ScallopOption[String] = opt[String](
        "shapeMapFormat",
        default = Some(defaultShapeMapFormat),
        descr = s"Shape Map format. Default ($defaultShapeMapFormat). Possible values = ${shapeMapFormats.mkString(",")}",
        validate = isMemberOf(shapeMapFormats),
        noshort = true)
    
    override protected def onError(e: Throwable): Unit = onError(e, builder)

    private def isMemberOf(ls: List[String])(x: String): Boolean =
      ls contains x.toUpperCase

}
