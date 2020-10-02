package com.github.thstock.djp

import org.junit.jupiter.api.{Assertions, Test}

class ScalaParserTest {
  @Test
  def testParseEnv_1(): Unit = {
    val out = ScalaParser.parseAll(ScalaParser.ENV, "ENV a i")
    Assertions.assertTrue(out.successful)
    Assertions.assertEquals(Env(Seq(Assign("a", "i"))), out.get)
  }

  @Test
  def testParseEnv_2(): Unit = {
    val out = ScalaParser.parseAll(ScalaParser.ENV, "ENV a=i")
    Assertions.assertTrue(out.successful)
    Assertions.assertEquals(Env(Seq(Assign("a", "i"))), out.get)
  }

  @Test
  def testParseEnv_3(): Unit = {
    val out = ScalaParser.parseAll(ScalaParser.ENV, "ENV a=i \\\nb=c")
    Assertions.assertTrue(out.successful)
    Assertions.assertEquals(Env(Seq(Assign("a", "i"), Assign("b", "c"))), out.get)
  }
}
