
package com.github.thstock.djp

import com.google.common.collect.ImmutableMap

import scala.util.matching.Regex
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
  val eol = sys.props("line.separator")
  val eoi = """\z""".r // end of input
  val nl = eoi | eol
  val indentOpt = """[ \t]*""".r
  val spaceOpt = "[ ]*".r
  val space = "[ ]+".r
  val noSpace = """\S+""".r
  val cont = """\"""
  val word = """\w+""".r
  val noQuote = """[^"]+""".r
  val wordEq = """[\w=:{}]|\\\$""".r

  def contNl = spaceOpt ~ cont ~ nl ~> indentOpt ^^ (term => ContNl(term))

  def noQuoteEscapedQuote = "[^\\\\\"]|\\\\\"|\\\\#|\\\\n".r

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

  def assign1: Parser[Assign] = word ~ "=" ~ rep1(wordEq) ^^ {
    terms: (String ~ String ~ List[String]) => {
      Assign(terms._1._1, terms._2.mkString("").replace("\\$", "$"))
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

  def assignP1(string: Regex): Parser[Seq[Assign]] = (string ~> rep1(assign <~ spaceOpt)) ^^ {
    terms => {
      terms
    }
  }

  def assignP2(string: Regex): Parser[Seq[Assign]] = (string ~> (rep1(assign <~ contNl) ~ assign)) ^^ {
    terms => {
      val aT: Seq[Assign] = terms._1
      Seq(terms._2) ++ aT
    }
  }

  def assignP(key: Regex): Parser[Seq[Assign]] = (assignP2(key) | assignP1(key))

  def label: Parser[Label] = {
    assignP("^LABEL ".r) ^^ { assigns => Label(assigns) }
  }

  val comment = "^[ ]*#.*".r

  def run = "RUN " ~ rep1("[^\\\\]+".r | "\\\\".r ~ opt(contNl)) | contNl | comment

  def CMD = "^CMD .*".r

  def MAINTAINER = "^MAINTAINER .*".r

  def EXPOSE = "^EXPOSE .*".r

  def ENV: Parser[Env] = {
    val envW = "^ENV ".r

    def envAssign = assignP(envW) ^^ { assigns => Env(assigns) }

    def envNoSpace = envW ~> word ~ space ~ ".+".r ^^ {
      (terms) => Env(Seq(Assign(terms._1._1, terms._2)))
    }

    (envAssign | envNoSpace) ^^ { en => en.copy(assigns = en.assigns.reverse) }
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

  def doParse(s: String, strict: Boolean): (String,
    ImmutableMap[String, String],
    ImmutableMap[String, String],
    ImmutableMap[String, String]) = {

    val list: Parser[List[Any]] = rep(from | label |
      run | CMD | MAINTAINER | EXPOSE | ENV | ADD | COPY | ENTRYPOINT | VOLUME | USER |
      WORKDIR | ARG | ONBUILD | STOPSIGNAL | HEALTHCHECK | SHELL)
    val lists: Parser[List[List[Any]]] = repsep(list, rep1(eol))

    val value = parseAll(lists, s)
    val r = ScalaParser.PResult(value.map(_.flatten))

    r.pr match {
      case Success(vp, v) => {

        def toImmutableMap[T <: Assignable]: ImmutableMap[String, String] = {
          val labels: Seq[Assign] = vp.filter(_.isInstanceOf[T]).map(_.asInstanceOf[T]).flatMap(_.assigns)
          val b = ImmutableMap.builder[String, String]()
          for (line <- labels) {
            b.put(line.key, line.value)
          }
          b.build()
        }

        val froms: Seq[String] = vp.filter(_.isInstanceOf[From]).map(_.asInstanceOf[From]).map(_.from)

        (froms.head, toImmutableMap[Label],
          toImmutableMap[Env],
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




