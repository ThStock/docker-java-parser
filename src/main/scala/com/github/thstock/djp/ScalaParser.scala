
package com.github.thstock.djp

import com.google.common.collect.ImmutableMap

import scala.util.parsing.combinator.RegexParsers

case class Assign(key: String, value: String)

trait Assignable {
  val assigns: Seq[Assign]
}

case class Label(assigns: Seq[Assign]) extends Assignable

case class Env(assigns: Seq[Assign]) extends Assignable

case class From(from: String)

case class ContNl(indent: String)

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

    def contNl = spaceOpt ~ cont ~ nl ~> indent ^^ (term => ContNl(term))

    def noQuoteEscapedQuote = "([^\\\\\"]|\\\\\"|\\\\#|\\\\n)".r

    val word = """\w+""".r
    val noQuote = """[^"]+""".r
    val wordEq = """[\w=]+""".r

    def quotedWord = {
      def re(in: String) = in.replace("\\\"", "\"")
        .replace("\\#", "#").replace("\\n", "\n")
      """"""" ~> rep1(noQuoteEscapedQuote | contNl) <~ """"""" ^^ {
        ((x: List[String]) => {
          re(x.mkString(""))
        })
        ((x: List[Any]) => {
          x.map {
            case s: String => re(s)
            case cl: ContNl => cl.indent
          }.mkString("")
        })
      }
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

    def assignP1(string: String): Parser[Seq[Assign]] = (string ~> rep1(assign <~ spaceOpt)) ^^ {
      terms => {
        terms
      }
    }

    def assignP2(string: String): Parser[Seq[Assign]] = (string ~> (rep1(assign <~ contNl) ~ assign)) ^^ {
      terms => {
        val aT: Seq[Assign] = terms._1
        Seq(terms._2) ++ aT
      }
    }

    def assignP(key: String): Parser[Seq[Assign]] = (assignP2(key) | assignP1(key))

    def label: Parser[Label] = {
      assignP("LABEL ") ^^ { assigns => Label(assigns) }
    }

    def run = "RUN .*".r

    def CMD = "CMD .*".r

    def MAINTAINER = "MAINTAINER .*".r

    def EXPOSE = "EXPOSE .*".r

    def ENV: Parser[Env] = {
      def envAssign = assignP("ENV ") ^^ { assigns => Env(assigns) }

      def envE = "ENV " ~> word ~ rep1(space ~ word) ^^ {
        (terms) => Env(Seq(Assign(terms._1, terms._2.map(_._2).mkString(" "))))
      }

      envAssign | envE
    }

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

    val list: Parser[List[Any]] = rep(from | label |
      run | CMD | MAINTAINER | EXPOSE | ENV | ADD | COPY | ENTRYPOINT | VOLUME | USER |
      WORKDIR | ARG | ONBUILD | STOPSIGNAL | HEALTHCHECK | SHELL)
    val lists: Parser[List[List[Any]]] = repsep(list, rep1(eol))

    val value = parseAll(lists, s)
    val r = ScalaParser.PResult(value.map(_.flatten))

    r.pr match {
      case Success(vp, v) => {

        def toImmutableMap[T <: Assignable](): ImmutableMap[String, String] = {
          val labels: Seq[Assign] = vp.filter(_.isInstanceOf[T]).map(_.asInstanceOf[T]).flatMap(_.assigns)
          val b = ImmutableMap.builder[String, String]()
          for (line <- labels) {
            b.put(line.key, line.value)
          }
          b.build()
        }

        val froms: Seq[String] = vp.filter(_.isInstanceOf[From]).map(_.asInstanceOf[From]).map(_.from)

        (froms.head, toImmutableMap[Label](),
          toImmutableMap[Env](),
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




