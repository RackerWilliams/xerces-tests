package com.rackspace.com.papi.components.checker.xsd

import java.io.File

import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import org.xml.sax.SAXException

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.scalatest.exceptions.TestFailedException

import org.scalatest.FunSuite

@RunWith(classOf[JUnitRunner])
class XSDTestSuite extends FunSuite {
  def isXMLFile (f : File) = f.getName().endsWith(".xml")
  def isXSDFile (f : File) = f.getName().endsWith(".xsd")

  val useSaxon = (System.getProperty("test.xsd.impl","Xerces") == "Saxon")
  val factory = {
    if (useSaxon) {
      val inst = Class.forName("com.saxonica.jaxp.SchemaFactoryImpl").newInstance.asInstanceOf[SchemaFactory]
      inst.setProperty("http://saxon.sf.net/feature/xsd-version","1.1")
      inst
    } else {
      val inst =  SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1")
      //
      //  Enable CTA full XPath2.0 checking in XSD 1.1
      //
      inst.setFeature ("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
      inst
    }
  }

  val parentTestDir = new File("xsd_tests")
  val testDirs = parentTestDir.listFiles().toList.filter(f => f.isDirectory)

  testDirs.foreach(d => {
    val xsdFiles = d.listFiles().toList.filter(isXSDFile)
    val xsdSources = xsdFiles.map(f => new StreamSource(f).asInstanceOf[Source]).toArray
    val xsd = factory.newSchema(xsdSources)
    val goodSamples = new File (d, "good").listFiles().toList.filter(isXMLFile)
    val badSamples  = new File (d, "bad").listFiles().toList.filter(isXMLFile)

    //
    //  All good samples should validate
    //
    goodSamples.foreach (gs => {
      test (s"Instance $gs should validate against $xsdFiles") {
        try {
          xsd.newValidator().validate(new StreamSource(gs))
        } catch {
          case se : SAXException => throw new TestFailedException(s"Validation Error on instance document: $gs", se, 4)
          case unknown : Throwable => throw new TestFailedException ("Unkown validation error! ", unknown, 4)
        }
      }
    })

    //
    //  All bad samples should fail to validate
    //
    badSamples.foreach (bs => {
      test (s"Instance $bs should *not* validate against $xsdFiles") {
        try {
          xsd.newValidator().validate(new StreamSource(bs))
          throw new TestFailedException (s"Expecting $bs to fail validation against $xsdFiles but it did not!", 4)
        } catch {
          case se : SAXException => // Ignore we are good!
          case unknown : Throwable => throw new TestFailedException ("Unkown validation error! ", unknown, 4)
        }
      }
    })
  })
}
