package com.github.thstock.djp

import com.google.common.collect.ImmutableMap

import scala.util.parsing.combinator.RegexParsers

private class ScalaParser extends RegexParsers {

  override val skipWhitespace = false

  case class Assign(key: String, value: String)

  case class Label(assigns: Seq[Assign])

  def doParse(s: String): ParseResult[List[Any]] = {
    val eol = sys.props("line.separator")
    val eoi = """\z""".r // end of input
    val nl = eoi | eol
    val indent = """[ \t]*""".r
    val spaceOpt = "[ ]*".r
    val space = "[ ]+".r
    val cont = """\"""
    val contNl = spaceOpt ~ cont ~ nl ~ indent
    val word = """\w+""".r
    val noSpace = """[\w=]+""".r

    def assignErr = word ~ "=" ~ word ~ space ~> word <~ space ~ word >> {
      (x => {
        err("can't find = in \"" + x + "\". Must be of the form: name=value")
      })
    }

    def assign: Parser[Assign] = assignErr | word ~ "=" ~ noSpace ^^ {
      case terms => {
        Assign(terms._1._1, terms._2)
      }
    }

    val from = """FROM """ ~ word <~ nl

    def label1: Parser[Label] = ((Dockerfile.LABEL + " ") ~> rep1(assign <~ spaceOpt)) ^^ {
      case terms => {
        Label(terms)
      }
    }

    def label2: Parser[Label] = ((Dockerfile.LABEL + " ") ~> (rep1(assign ~ contNl) ~ assign)) ^^ {
      case terms => {
        val aT: Seq[Assign] = terms._1.map(_._1)
        Label(Seq(terms._2) ++ aT)
      }
    }

    val list: Parser[List[Any]] = rep(from | label2 | label1)
    val lists: Parser[List[List[Any]]] = repsep(list, rep1(eol))

    val value = parseAll(lists, s)
    value.map(_.flatten)
  }

  def parseLabels(in: String): ImmutableMap[String, String] = {
    val value = doParse(in)
    value match {
      case Success(vp, v) => {
        val labels: Seq[Assign] = vp.filter(_.isInstanceOf[Label]).map(_.asInstanceOf[Label]).flatMap(_.assigns)
        val b = ImmutableMap.builder[String, String]()
        for (line <- labels) {
          b.put(line.key, line.value)
        }
        b.build()
      }
      case Failure(msg, next) => {
        throw new IllegalStateException("failure " + msg)
      }
      case Error(msg, next) => {
        throw new IllegalStateException("Syntax error - " + msg)
      }
    }

  }
}
