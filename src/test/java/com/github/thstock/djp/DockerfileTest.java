package com.github.thstock.djp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.File;
import java.io.UncheckedIOException;

import static org.junit.Assert.assertEquals;

public class DockerfileTest {

  @Test
  public void test_lines_newline() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\r\nLABEL\ta=b");

    // THEN
    assertEquals(ImmutableList.of("FROM a", "LABEL\ta=b"), testee.allLines);
  }

  @Test
  public void test_lines_effective() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("\n\nFROM a\nLABEL a=b \t\nLABEL b=c\n \t");

    // THEN
    assertEquals(ImmutableList.of("FROM a", "LABEL a=b \t", "LABEL b=c"), testee.lines);
  }

  @Test
  public void test_labels() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseB("FROM a\nLABEL a=b");

    // THEN
    assertEquals(ImmutableMap.of("a", "b"), testee.getLabels());
  }

  @Test
  public void test_labels_strange() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\nLABEL a = b");

    // THEN
    Assertions.assertThat(testee.getLabels())
        .isEqualTo(ImmutableMap.of("a", "= b"));
  }

  @Test
  public void test_labels_strange2() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\nLABEL a==b");

    // THEN
    assertEquals(ImmutableMap.of("a", "=b"), testee.getLabels());
  }

  @Test
  public void test_labels_strange3() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\nLABEL a==");

    // THEN
    assertEquals(ImmutableMap.of("a", "="), testee.getLabels());
  }

  @Test
  public void test_labels_strange4() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\nLABEL a=\"=\"");

    // THEN
    assertEquals(ImmutableMap.of("a", "="), testee.getLabels());
  }

  @Test
  public void test_labels_strange_strict() {
    // GIVEN / WHEN
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Dockerfile.parseStrict("FROM a\nLABEL a = b"))
        .withMessage("Syntax error - can't find = in \"=\". Must be of the form: name=value");

  }

  @Test
  public void test_labels_strange2_strict() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseStrict("FROM a\nLABEL a==b");

    // THEN
    assertEquals(ImmutableMap.of("a", "="), testee.getLabels()); // TODO Exception?
  }

  @Test
  public void test_labels_strange3_strict() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseStrict("FROM a\nLABEL a==");

    // THEN
    assertEquals(ImmutableMap.of("a", "="), testee.getLabels()); // TODO Exception?
  }

  @Test
  public void test_labels_strange4_strict() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseStrict("FROM a\nLABEL a=\"=\"");

    // THEN
    assertEquals(ImmutableMap.of("a", "="), testee.getLabels()); // TODO Exception?
  }

  @Test
  public void test_labels_invalid() {
    // GIVEN / WHEN
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Dockerfile.parse("FROM a\nLABEL a= = z"))
        .withMessage("Syntax error - can't find = in \"z\". Must be of the form: name=value")
    ;
  }

  @Test
  public void test_labels_invalid_two() {
    // GIVEN / WHEN
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Dockerfile.parse("FROM a\nLABEL a=b b c=d"))
        .withMessage("Syntax error - can't find = in \"c\". Must be of the form: name=value")
    ;
  }

  @Test
  public void test_labels_multi() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseB("FROM a\nLABEL a=b\nLABEL b=c");

    // THEN
    assertEquals(ImmutableMap.of("a", "b", "b", "c"), testee.getLabels());
  }

  @Test
  public void test_labels_multi_two() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\nLABEL \"a\"=\"b\" \"b\"=\"c\"");

    // THEN
    assertEquals(ImmutableMap.of("a", "b", "b", "c"), testee.getLabels());
  }

  @Test
  public void test_labels_multi_two_space() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\nLABEL \"a\"=\"b b\" \"b\"=\"c\"");

    // THEN
    assertEquals(ImmutableMap.of("a", "b b", "b", "c"), testee.getLabels());
  }

  @Test
  public void test_labels_multi_two_var() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\nLABEL a=b  b=c");

    // THEN
    assertEquals(ImmutableMap.of("a", "b", "b", "c"), testee.getLabels());
  }

  @Test
  public void test_labels_multiline() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseB("FROM a\nLABEL a=b\\\n   b=c");

    // THEN
    assertEquals(ImmutableMap.of("a", "b", "b", "c"), testee.getLabels());
  }

  @Test
  public void testMinimal() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM debian:stretch-slim");

    // THEN
    assertEquals(1, testee.allLines.size());
    assertEquals("debian:stretch-slim", testee.getFrom());
    assertEquals(ImmutableMap.of(), testee.getLabels());
  }

  @Test
  public void testMinimal_multistaged() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM debian:stretch-slim\nFROM alpine:3.7");

    // THEN
    assertEquals("alpine:3.7", testee.getFrom());
  }

  @Test
  public void test_empty() {
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Dockerfile.parse(""))
        .withMessage("Dockerfile cannot be empty")
    ;

  }

  @Test
  public void test_empty_var() {
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Dockerfile.parse(" \n\t"))
        .withMessage("Dockerfile cannot be empty")
    ;

  }

  @Test
  public void test_start_with_token() {
    Dockerfile testee = Dockerfile.parse("LABEL a=b");
    assertEquals(1, testee.lines.size());
  }

  @Test
  public void test_start_with_token_strict() {
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Dockerfile.parseStrict("LABEL a=b"))
        .withMessage("Dockerfile must start with FROM")
    ;
  }

  @Test
  public void test_start_with_no_token() {
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Dockerfile.parse("a=b"))
        .withMessage("Invalid Dockerfileline: 'a=b'")
    ;
  }

  @Test
  public void test_env_equal() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\nENV myName=\"John Doe\" myDog=Rex\\ The\\ Dog \\\n"
        + "    myCat=fluffy");

    // THEN
    assertEquals(ImmutableMap.of("myName", "John Doe", "myDog", "Rex The Dog", "myCat", "fluffy"),
        testee.getEnv());
  }

  @Test
  public void test_env() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\nENV myName John Doe\n"
        + "ENV myDog Rex The Dog\n"
        + "ENV myCat fluffy");

    // THEN
    assertEquals(ImmutableMap.of("myName", "John Doe", "myDog", "Rex The Dog", "myCat", "fluffy"),
        testee.getEnv());
  }

  @Test
  public void test_var() {
    // GIVEN / WHEN
    // TODO export env variable "some"
    Dockerfile testee = Dockerfile.parse("FROM scratch\nLABEL test=${some}\n");
    // THEN
    // TODO unclear what will happen
  }

  @Test
  public void test_var_escaped() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM scratch\nLABEL test=\\${some:invalid}\n");
    // THEN
    assertEquals(ImmutableMap.of("test", "${some:invalid}"), testee.getLabels());
  }

  @Test
  public void test_invalid_var() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM scratch\nLABEL test=${some:invalid}\n");
    // THEN
    // failed to process "${some:invalid}": unsupported modifier (i) in substitution
    // TODO test for exception
  }

  @Test
  public void test_copy() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM scratch\nCOPY a b\n");
    // THEN
    assertEquals(ImmutableMap.of("a", "b"), testee.getCopy());
  }

  @Test
  public void test_copy_two() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM scratch\nCOPY a b\nCOPY ./from /to/other\n");
    // THEN
    assertEquals(ImmutableMap.of("a", "b", "./from", "/to/other"), testee.getCopy());
  }


  @Test
  public void test_file() {
    // GIVEN
    File content = Dockerfile.resourceFile("samples/Nginx");

    // WHEN
    Dockerfile testee = Dockerfile.parse(content);

    // THEN
    assertEquals(99, testee.allLines.size());
    assertEquals(75, testee.lines.size());
    assertEquals(ImmutableMap.of("maintainer", "NGINX Docker Maintainers <docker-maint@nginx.com>"), testee.getLabels());
  }

  @Test
  public void test_file_strict() {
    // GIVEN
    File content = Dockerfile.resourceFile("samples/Nginx");

    // WHEN
    Dockerfile testee = Dockerfile.parseStrict(content);

    // THEN
    assertEquals(99, testee.allLines.size());
    assertEquals(75, testee.lines.size());
    assertEquals(ImmutableMap.of("maintainer", "NGINX Docker Maintainers <docker-maint@nginx.com>"), testee.getLabels());

  }

  @Test
  public void test_file_not_existing() {
    // GIVEN
    File content = new File("n.a");

    // WHEN / THEN
    Assertions.assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> Dockerfile.parse(content))
        .withMessageStartingWith("n.a ("); // No such file or directory)

  }

}
