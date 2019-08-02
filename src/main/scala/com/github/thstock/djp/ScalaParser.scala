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
    val cont = "[ ]*".r ~ """\""" ~ nl ~ indent
    val word = """\w+""".r

    def assign: Parser[Assign] = word ~ "=".r ~ word ^^ {
      case terms => {
        Assign(terms._1._1, terms._2)
      }
    }

    val assignCont = assign ~ cont

    val from = """FROM """ ~ word <~ nl

    def label1: Parser[Label] = ("""LABEL """ ~> assign) ^^ {
      case terms => {
        Label(Seq(terms))
      }
    }

    def label2: Parser[Label] = ("""LABEL """ ~> (rep1(assignCont) ~ assign)) ^^ {
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
    val labels: Seq[Assign] = value.get.filter(_.isInstanceOf[Label]).map(_.asInstanceOf[Label]).flatMap(_.assigns)
    val b = ImmutableMap.builder[String, String]()
    for (line <- labels) {
      b.put(line.key, line.value)
    }
    b.build()
  }
}
