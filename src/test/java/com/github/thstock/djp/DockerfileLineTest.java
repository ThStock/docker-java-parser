package com.github.thstock.djp;

import static org.junit.Assert.assertEquals;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class DockerfileLineTest {

  @Test
  public void testLine() {
    DockerfileLine testee = DockerfileLine.from("LABEL a=a");

    assertEquals(new DockerfileLine("LABEL", "a=a"), testee);
  }

  @Test
  public void testLine_invalid_no() {
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> DockerfileLine.from("LABEL"))
        .withMessage("Invalid Dockerfileline: 'LABEL'");

  }

  @Test
  public void testLine_invalid() {
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> DockerfileLine.from("LABEL "))
        .withMessage("Invalid Dockerfileline: 'LABEL '");

  }

  @Test
  public void testLine_tab() {
    DockerfileLine testee = DockerfileLine.from("LABEL\t\ta=a");

    assertEquals(new DockerfileLine("LABEL", "a=a"), testee);
  }

  @Test
  public void testLine_whitespace() {
    DockerfileLine testee = DockerfileLine.from("LABEL\t  \ta=a");

    assertEquals(new DockerfileLine("LABEL", "a=a"), testee);
  }

  @Test
  public void testLine_nl() {
    DockerfileLine testee = DockerfileLine.from("LABEL  a=a \\\n  b=c");

    assertEquals(new DockerfileLine("LABEL", "a=a \\\n  b=c"), testee);
  }

  @Test
  public void testLine_value_tokens() {
    DockerfileLine testee = DockerfileLine.from("LABEL a=a");

    assertEquals(ImmutableList.of("a", "=", "a"), testee.valueTokens());
  }

  @Test
  public void testLine_value_tokens_escape_ws() {
    DockerfileLine testee = DockerfileLine.from("ENV a=a\\ a");

    assertEquals(ImmutableList.of("a", "=", "a a"), testee.valueTokens());
  }

  @Test
  public void testLine_value_strange() {
    // GIVEN / WHEN
    DockerfileLine testee = DockerfileLine.from("LABEL a = b");

    // THEN
    assertEquals(ImmutableList.of("a", " ", "=", " ", "b"), testee.valueTokens());

  }

  @Test
  public void testLine_value_strange2() {
    // GIVEN / WHEN
    DockerfileLine testee = DockerfileLine.from("LABEL a==b");

    // THEN
    assertEquals(ImmutableList.of("a", "=", "=", "b"), testee.valueTokens());
  }

  @Test
  public void testLine_value_invalid() {
    // GIVEN / WHEN
    assertEquals(ImmutableList.of("a", "=", " ", "=", " ", "b"), DockerfileLine.from("LABEL a= = b").valueTokens());
  }

  @Test
  public void testLine_value_invalid_two() {
    // GIVEN / WHEN
    assertEquals(ImmutableList.of("a", "=", "b", " ", "z", " ", "c", "=", "d"),
        DockerfileLine.from("LABEL a=b z c=d").valueTokens());
  }

  @Test
  public void testLine_value_tokens_multi() {
    DockerfileLine testee = DockerfileLine.from("LABEL a=a    b=b");

    assertEquals(ImmutableList.of("a", "=", "a", " ", "b", "=", "b"), testee.valueTokens());
  }

  @Test
  public void testLine_value_tokens_multiline() {
    DockerfileLine testee = DockerfileLine.from("LABEL a=a \nb=b");

    assertEquals(ImmutableList.of("a", "=", "a", " ","b", "=", "b"), testee.valueTokens());
  }

  @Test
  public void testLine_value_tokens_multiline_indent() {
    DockerfileLine testee = DockerfileLine.from("LABEL a=a \n    b=b");

    assertEquals(ImmutableList.of("a", "=", "a", " ", "b", "=", "b"), testee.valueTokens());
  }

  @Test
  public void testLine_value_tokens_multiline_indent_quote() {
    DockerfileLine testee = DockerfileLine.from("LABEL \" a \"=a \n   \" b\"=b");

    assertEquals(ImmutableList.of(" a ", "=", "a", " "," b", "=", "b"), testee.valueTokens());
  }

  @Test
  public void testLine_value_tokens_multiline_indent_quote_all() {
    DockerfileLine testee = DockerfileLine.from("LABEL \" a \"=\" a \"   \n   \" b \"=\" b \"");

    assertEquals(ImmutableList.of(" a ", "=", " a ", " ", " b ", "=", " b "), testee.valueTokens());
  }

  @Test
  public void testLine_value_tokens_multiline_indent_quote_other() {
    DockerfileLine testee = DockerfileLine.from("LABEL \"a\"=\"b b\" \"b\"=\"c\"");

    assertEquals(ImmutableList.of("a", "=", "b b", " ", "b", "=", "c"), testee.valueTokens());
  }

  @Test
  public void testLine_value_tokens_quote() {
    DockerfileLine testee = DockerfileLine.from("LABEL a=\"a\"");

    assertEquals(ImmutableList.of("a", "=", "a"), testee.valueTokens());
  }

  @Test
  public void testLine_value_tokens_quote_space() {
    DockerfileLine testee = DockerfileLine.from("LABEL a=\"a \"");

    assertEquals(ImmutableList.of("a", "=", "a "), testee.valueTokens());
  }

  @Test
  public void testLine_value_tokens_quote_unbalanced() {
    DockerfileLine testee = DockerfileLine.from("LABEL a=\"a");
    // TODO error
    assertEquals(ImmutableList.of("a", "=", "a"), testee.valueTokens());
  }

  @Test
  public void testLine_value_tokens_quote_unbalanced_2() {
    DockerfileLine testee = DockerfileLine.from("LABEL a=a\"");
    // TODO error
    assertEquals(ImmutableList.of("a", "=", "a"), testee.valueTokens());
  }
}
