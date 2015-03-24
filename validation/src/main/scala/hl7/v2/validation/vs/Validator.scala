package hl7.v2.validation.vs

import hl7.v2.instance.{Complex, Element, Simple, Message}
import hl7.v2.validation.report.VSEntry

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Trait defining the value set validator
  * @author Salifou Sidi M. Malick <salifou.sidi@gmail.com>
  */

trait Validator { this: SimpleElementValidator with ComplexElementValidator =>

  /**
    * The value set library used by this validator
    */
  def valueSetLibrary: Map[String, ValueSet]

  /**
    * Checks the message and returns the list of problems.
    */
  def checkValueSet(m: Message): Future[Seq[VSEntry]] = Future { check(m.asGroup) }

  /**
    * Recursively the element and its children if any
    */
  private def check(e: Element): List[VSEntry] = e match {
    case s: Simple  => check(s, valueSetLibrary)
    case c: Complex =>
      val r = check(c, valueSetLibrary)
      c.children.foldLeft(r) { (acc, x) => check(x) ::: acc  }
    case _ => Nil //No op //FIXME Make something specific to HL7 Data Elem for e.g.
  }

}
