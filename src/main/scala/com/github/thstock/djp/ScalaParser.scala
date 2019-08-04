
package com.github.thstock.djp

import com.google.common.collect.ImmutableMap

import scala.util.parsing.combinator.RegexParsers

case class Assign(key: String, value: String)

case class Label(assigns: Seq[Assign])

case class Env(assigns: Seq[Assign])

case class From(from: String)

case class ContNl()

object ScalaParser extends RegexParsers {

  case class PResult(pr: ParseResult[List[Any]])

  override val skipWhitespace = false

  def doParse(s: String, strict: Boolean): (String,
    ImmutableMap[String, String],
    ImmutableMap[String, String],
    ImmutableMap[String, String]) = {
    val eol = sys.props("line.separator")
    val eoi = """\z""".r // end of input
    val nl = eoi | eol
    val indent = """[ \t]*""".r
    val spaceOpt = "[ ]*".r
    val space = "[ ]+".r
    val cont = """\"""

    def contNl = spaceOpt ~ cont ~ nl ~ indent ^^ (_ => ContNl())

    def noQuoteEscapedQuote = "([^\\\\\"]|\\\\\"|\\\\n)".r

    val word = """\w+""".r
    val noQuote = """[^"]+""".r
    val wordEq = """[\w=]+""".r

    def quotedWord =
      """"""" ~> rep1(noQuoteEscapedQuote | contNl) <~ """"""" ^^ {
        ((x: List[String]) => {
          x.mkString("").replace("\\\"", "\"")
        })
        ((x: List[Any]) => {
          x.map {
            case s: String => s.replace("\\\"", "\"")
            case _: ContNl => "\n"
          }.mkString("")
        })
      }

    val noSpace = """\S+""".r

    def assignErr = word ~ "=" ~ word ~ space ~> word <~ space ~ word >> {
      (x => {
        err("can't find = in \"" + x + "\". Must be of the form: name=value")
      })
    }

    def assignQuote: Parser[Assign] = quotedWord ~ "=" ~ quotedWord ^^ {
      terms => {
        Assign(terms._1._1, terms._2)
      }
    }

    def assignQright: Parser[Assign] =
      """[\.\w]+""".r ~ "=" ~ quotedWord ^^ {
        terms => {
          Assign(terms._1._1, terms._2)
        }
      }

    def assign1: Parser[Assign] = word ~ "=" ~ wordEq ^^ {
      terms: (String ~ String ~ String) => {
        Assign(terms._1._1, terms._2)
      }
    }

    def assignStrange: Parser[Assign] = (word ~ space ~ "=" ~ space ~ word) ^^ {
      terms: (String ~ String ~ String ~ String ~ String) => {
        Assign(terms._1._1._1._1, "= " + terms._2) // strange
      }
    }

    def assign: Parser[Assign] = assignErr | assignQright | assignStrange | assign1 | assignQuote

    def from =
      """FROM """ ~> noSpace ^^ {
        terms ⇒ From(terms)
      }

    def label1: Parser[Label] = (("LABEL ") ~> rep1(assign <~ spaceOpt)) ^^ {
      terms => {
        Label(terms)
      }
    }

    def label2: Parser[Label] = (("LABEL ") ~> (rep1(assign <~ contNl) ~ assign)) ^^ {
      terms => {
        val aT: Seq[Assign] = terms._1
        Label(Seq(terms._2) ++ aT)
      }
    }

    def run = "RUN .*".r

    def CMD = "CMD .*".r

    def MAINTAINER = "MAINTAINER .*".r

    def EXPOSE = "EXPOSE .*".r

    def ENV = "ENV .*".r

    def ADD = "ADD .*".r

    def COPY = "COPY .*".r

    def ENTRYPOINT = "ENTRYPOINT .*".r

    def VOLUME = "VOLUME .*".r

    def USER = "USER .*".r

    def WORKDIR = "WORKDIR .*".r

    def ARG = "ARG .*".r

    def ONBUILD = "ONBUILD .*".r

    def STOPSIGNAL = "STOPSIGNAL .*".r

    def HEALTHCHECK = "HEALTHCHECK .*".r

    def SHELL = "SHELL .*".r

    val list: Parser[List[Any]] = rep(from | label2 | label1 |
      run | CMD | MAINTAINER | EXPOSE | ENV | ADD | COPY | ENTRYPOINT | VOLUME | USER |
      WORKDIR | ARG | ONBUILD | STOPSIGNAL | HEALTHCHECK | SHELL)
    val lists: Parser[List[List[Any]]] = repsep(list, rep1(eol))

    val value = parseAll(lists, s)
    val r = ScalaParser.PResult(value.map(_.flatten))

    r.pr match {
      case Success(vp, v) => {

        val labels: Seq[Assign] = vp.filter(_.isInstanceOf[Label]).map(_.asInstanceOf[Label]).flatMap(_.assigns)
        val b = ImmutableMap.builder[String, String]()
        for (line <- labels) {
          b.put(line.key, line.value)
        }
        val labelsO = b.build()

        val froms: Seq[String] = vp.filter(_.isInstanceOf[From]).map(_.asInstanceOf[From]).map(_.from)

        (froms.head, labelsO,
          ImmutableMap.of[String, String],
          ImmutableMap.of[String, String])
      }
      case f: Failure => {
        throw new IllegalStateException("failure: " + f.toString())
      }
      case Error(msg, next) => {
        throw new IllegalStateException("Syntax error - " + msg)
      }
    }
  }
}




