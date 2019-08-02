package com.github.thstock.djp

import scala.util.parsing.combinator.RegexParsers

private class ScalaParser extends RegexParsers {

  override val skipWhitespace = false

  def doParse(s: String): ParseResult[List[List[Any]]] = {
    val eol = sys.props("line.separator")
    val eoi = """\z""".r // end of input
    val nl = eoi | eol
    val indent = """[ \t]*""".r
    val cont = """ \""" ~ nl ~ indent
    val word = """\w+""".r
    val assign = word ~ " = " ~ word
    val assignCont = assign ~ cont

    val from = """FROM """ ~ word <~ nl
    val label2 = """LABEL """ ~ (rep1(assignCont) ~ assign | assign)
    val list: Parser[List[Any]] = rep(from | label2)
    val lists: Parser[List[List[Any]]] = repsep(list, rep1(eol))

    val value = parseAll(lists, s)
    value
  }
}
