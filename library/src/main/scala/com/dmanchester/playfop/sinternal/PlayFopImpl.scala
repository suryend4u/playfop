package com.dmanchester.playfop.sinternal

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.StringReader
import java.io.StringWriter

import scala.xml.Elem
import scala.xml.Node
import scala.xml.XML

import org.apache.fop.apps.FOUserAgent
import org.apache.fop.apps.Fop
import org.slf4j.LoggerFactory

import com.dmanchester.playfop.sapi.PlayFop

import javax.inject.Singleton
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.stream.StreamSource
import play.twirl.api.Xml

/** The standard implementation of `[[com.dmanchester.playfop.sapi.PlayFop PlayFop]]`.
  *
  * While the code within this class is thread-safe, it is PlayFOP's primary
  * integration point with Apache FOP, and there is an open question around the
  * thread safety of Apache FOP itself. For more information, see the "Thread
  * Safety" discussion in the PlayFOP User Guide.
  */
@Singleton
class PlayFopImpl extends PlayFop {

  private val fopFactorySource = new FopFactorySource()

  private val logger = LoggerFactory.getLogger(this.getClass())

  def processTwirlXml[U](xslfo: Xml, outputFormat: String,
      autoDetectFontsForPDF: Boolean = false,
      foUserAgentBlock: (FOUserAgent => U) = {_: FOUserAgent => }): Array[Byte] = {

    processStringXml(xslfo.body, outputFormat, autoDetectFontsForPDF, foUserAgentBlock)
  }

  def processScalaXml[U](xslfo: Node, outputFormat: String,
      autoDetectFontsForPDF: Boolean = false,
      foUserAgentBlock: (FOUserAgent => U) = {_: FOUserAgent => }): Array[Byte] = {

    val stringWriter = new StringWriter()
    XML.write(stringWriter, xslfo, "utf-8", true /* xmlDecl */, null /* doctype */)

    processStringXml(stringWriter.toString(), outputFormat, autoDetectFontsForPDF, foUserAgentBlock)
  }

  def processStringXml[U](xslfo: String, outputFormat: String,
      autoDetectFontsForPDF: Boolean = false,
      foUserAgentBlock: (FOUserAgent => U) = {_: FOUserAgent => }): Array[Byte] = {

    logger.info("Rendering XSL-FO...")
    if (logger.isTraceEnabled()) {
      logger.trace(s"XSL-FO:\n$xslfo")
    }

    val output = new ByteArrayOutputStream()
    val fop = newFop(outputFormat, output, autoDetectFontsForPDF, foUserAgentBlock)

    val transformer = TransformerFactory.newInstance().newTransformer()

    val source = new StreamSource(new StringReader(xslfo))

    val result = new SAXResult(fop.getDefaultHandler())

    transformer.transform(source, result)

    val byteArray = output.toByteArray()

    logger.info(s"...XSL-FO rendered. ${byteArray.length} bytes produced.")

    byteArray
  }

  def newFop[U](outputFormat: String, output: OutputStream,
      autoDetectFontsForPDF: Boolean = false,
      foUserAgentBlock: (FOUserAgent => U) = {_: FOUserAgent => }): Fop = {

    val fopConfigXml: Option[Elem] = if (autoDetectFontsForPDF) {
      Some(
          <fop version="1.0">
            <renderers>
              <renderer mime="application/pdf">
                <fonts>
                  <auto-detect/>
                </fonts>
              </renderer>
            </renderers>
          </fop>
      )
    } else {
      None
    }

    val fopFactory = fopFactorySource.get(fopConfigXml)

    val foUserAgent = fopFactory.newFOUserAgent()
    foUserAgentBlock(foUserAgent)

    fopFactory.newFop(outputFormat, foUserAgent, output)
  }
}
