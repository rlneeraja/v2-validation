package hl7.v2.validation.vs

import java.io.InputStream
import java.lang.Boolean

import nist.xml.util.XOMDocumentBuilder

import scala.util.Try

case class ValueSetLibraryImpl(
    noValidation: Seq[String],
    library: Map[String, ValueSet]
  ) extends ValueSetLibrary {

  override def isExcludedFromTheValidation(id: String): Boolean =
    noValidation contains id

  override def get(id: String): ValueSet =
    library get id match {
      case Some(x) => x
      case None    => throw new ValueSetNotFoundException(id)
    }
}

object ValueSetLibraryImpl {

  import nist.xml.util.XOMExtensions._

  private val xsd = getClass.getResourceAsStream("/vs/ValueSets.xsd")

  /**
   * Builds and returns the value set map from the XML file
   */
  def apply(vsXML: InputStream): Try[ValueSetLibrary] =
    XOMDocumentBuilder.build( vsXML, xsd ) map { doc =>
      val root = doc.getRootElement
      val noValDef = root.getFirstChildElement("NoValidation")
      //val tblSet   = root.getFirstChildElement("ValueSetDefinitions")
      val tbls = root.getChildElements("ValueSetDefinitions")
      val noVal    = noValidation( noValDef )
      val lib      = tables(tbls)
      ValueSetLibraryImpl(noVal, lib)
    }

  private def noValidation(e: nu.xom.Element): Seq[String] =
    if(e == null) Nil else e.getChildElements("BindingIdentifier").map(_.getValue.trim).toList

  private def tableSet(e: nu.xom.Element): Map[String, ValueSet] =
    if( e == null ) Map()
    else {
      val tableDefs = e.getChildElements("ValueSetDefinition")
      tableDefs.foldLeft(Map[String, ValueSet]()) { (acc, x) =>
        val vs = valueSet(x)
        acc + (vs.id -> vs)
      }
    }
  
  private def tables(e: nu.xom.Elements): Map[String, ValueSet] =
    if( e == null ) Map()
    else {
      e.foldLeft(Map[String, ValueSet]()){ (acc, x) =>
        acc ++ tableSet(x)
      }
    }

  private def valueSet(e: nu.xom.Element): ValueSet = {
    val id = e.attribute("BindingIdentifier")
    val _stability = stability( e.attribute("Stability") )
    val _extensibility = extensibility( e.attribute("Extensibility") )
    val codes = (e.getChildElements("ValueElement") map code).toList
    ValueSet(id, _extensibility, _stability, codes )
  }

  private def stability(s: String): Option[Stability] =
    s match {
      case ""        => None
      case "Static"  => Some(Stability.Static)
      case "Dynamic" => Some(Stability.Dynamic)
      case x => throw new Exception(s"Invalid value set stability '$x'")
    }

  private def extensibility(s: String): Option[Extensibility] =
    s match {
      case ""      => None
      case "Open"  => Some(Extensibility.Open)
      case "Closed" => Some(Extensibility.Closed)
      case x => throw new Exception(s"Invalid value set extensibility '$x'")
    }

  private def code(e: nu.xom.Element): Code = {
    val value   = e.attribute("Value")
    val desc    = e.attribute("DisplayName")
    val usage   = codeUsage( e.attribute("Usage") )
    val codeSys = e.attribute("CodeSystem")
    Code(value, desc, usage, codeSys)
  }

  private def codeUsage(s: String): CodeUsage = s match {
    case "R" => CodeUsage.R
    case "E" => CodeUsage.E
    case "P" => CodeUsage.P
    case ""  => CodeUsage.R //FIXME This is not needed if the xml is validated against the XSD
    case  x  => throw new Exception(s"Invalid code usage '$x'")
  }
}
