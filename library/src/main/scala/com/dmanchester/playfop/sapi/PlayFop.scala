package com.dmanchester.playfop.sapi

import java.io.OutputStream

import scala.xml.Node

import org.apache.fop.apps.FOUserAgent
import org.apache.fop.apps.Fop

import play.twirl.api.Xml

/** The primary entry point into PlayFOP for Scala applications.
  */
trait PlayFop {

  /** Processes XSL-FO generated from a [[https://www.playframework.com/documentation/2.6.x/ScalaTemplates Twirl]]
    * XML template. Optionally auto-detects fonts (for PDF output) and/or
    * applies a code block to the `FOUserAgent`. Generates output in the
    * specified format.
    *
    * @tparam U the return type of `foUserAgentBlock` (typically inferred, as
    *           opposed to explicitly specified)
    * @param xslfo the XSL-FO to process
    * @param outputFormat the format to generate
    * @param autoDetectFontsForPDF whether to auto-detect fonts
    * @param foUserAgentBlock the code block for the `FOUserAgent`
    * @return the Apache FOP output
    */
  def processTwirlXml[U](xslfo: Xml, outputFormat: String,
      autoDetectFontsForPDF: Boolean = false,
      foUserAgentBlock: (FOUserAgent => U) = {_: FOUserAgent => }): Array[Byte]

  /** Processes XSL-FO provided as a [[https://github.com/scala/scala-xml scala-xml]]
    * `Node`. Optionally auto-detects fonts (for PDF output) and/or applies a
    * code block to the `FOUserAgent`. Generates output in the specified format.
    *
    * @tparam U the return type of `foUserAgentBlock` (typically inferred, as
    *           opposed to explicitly specified)
    * @param xslfo the XSL-FO to process
    * @param outputFormat the format to generate
    * @param autoDetectFontsForPDF whether to auto-detect fonts
    * @param foUserAgentBlock the code block for the `FOUserAgent`
    * @return the Apache FOP output
    */
  def processScalaXml[U](xslfo: Node, outputFormat: String,
      autoDetectFontsForPDF: Boolean = false,
      foUserAgentBlock: (FOUserAgent => U) = {_: FOUserAgent => }): Array[Byte]

  /** Processes XSL-FO provided as a `String` of XML. Optionally auto-detects
    * fonts (for PDF output) and/or applies a code block to the `FOUserAgent`.
    * Generates output in the specified format.
    *
    * @tparam U the return type of `foUserAgentBlock` (typically inferred, as
    *           opposed to explicitly specified)
    * @param xslfo the XSL-FO to process
    * @param outputFormat the format to generate
    * @param autoDetectFontsForPDF whether to auto-detect fonts
    * @param foUserAgentBlock the code block for the `FOUserAgent`
    * @return the Apache FOP output
    */
  def processStringXml[U](xslfo: String, outputFormat: String,
      autoDetectFontsForPDF: Boolean = false,
      foUserAgentBlock: (FOUserAgent => U) = {_: FOUserAgent => }): Array[Byte]

  /** Creates a new `Fop` instance, optionally auto-detecting fonts (for PDF
    * output) and/or applying a code block to the `FOUserAgent`. Sets up the
    * `Fop` to save output to the supplied `OutputStream` in the supplied format.
    *
    * '''Note:''' This method is offered primarily for client code to
    * interrogate the Apache FOP environment (for example, to determine
    * available fonts). Code wishing to process XSL-FO with Apache FOP should
    * rely on a `process...` method instead.
    *
    * @tparam U the return type of `foUserAgentBlock` (typically inferred, as
    *           opposed to explicitly specified)
    * @param outputFormat the format the `Fop` should generate
    * @param output the `OutputStream` to which the `Fop` should save output
    * @param autoDetectFontsForPDF whether to auto-detect fonts
    * @param foUserAgentBlock the code block for the `Fop`'s `FOUserAgent`
    * @return the `Fop`
    */
  def newFop[U](outputFormat: String, output: OutputStream,
      autoDetectFontsForPDF: Boolean = false,
      foUserAgentBlock: (FOUserAgent => U) = {_: FOUserAgent => }): Fop
}