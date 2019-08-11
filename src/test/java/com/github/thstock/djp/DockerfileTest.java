package com.github.thstock.djp;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.UncheckedIOException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class DockerfileTest {

  @Test
  public void test_labels() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\nLABEL a=b");

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
    Dockerfile testee = Dockerfile.parse("FROM a\nLABEL a.a=\"=\"");

    // THEN
    assertEquals(ImmutableMap.of("a.a", "="), testee.getLabels());
  }

  @Test
  public void test_labels_strange4_multiline() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\nLABEL a=\"b-@\" \\\n  b.b=\"a:@ \\\" \"");

    // THEN
    assertEquals(ImmutableMap.of("a", "b-@", "b.b", "a:@ \" "), testee.getLabels());
  }

  @Test
  public void test_labels_strange_strict() {
    // GIVEN / WHEN
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Dockerfile.parseJStrict("FROM a\nLABEL a = b"))
        .withMessage("Syntax error - can't find = in \"=\". Must be of the form: name=value");

  }

  @Test
  public void test_labels_strange2_strict() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseJStrict("FROM a\nLABEL a==b");

    // THEN
    assertEquals(ImmutableMap.of("a", "="), testee.getLabels()); // TODO Exception?
  }

  @Test
  public void test_labels_strange3_strict() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseJStrict("FROM a\nLABEL a==");

    // THEN
    assertEquals(ImmutableMap.of("a", "="), testee.getLabels()); // TODO Exception?
  }

  @Test
  public void test_labels_strange4_strict() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseJStrict("FROM a\nLABEL a=\"=\"");

    // THEN
    assertEquals(ImmutableMap.of("a", "="), testee.getLabels()); // TODO Exception?
  }

  @Test
  public void test_labels_invalid() {
    // GIVEN / WHEN
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Dockerfile.parseJ("FROM a\nLABEL a= = z"))
        .withMessage("Syntax error - can't find = in \"z\". Must be of the form: name=value")
    ;
  }

  @Test
  public void test_labels_invalid_two() {
    // GIVEN / WHEN
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Dockerfile.parse("FROM a\nLABEL a=b e c=d"))
        .withMessage("Syntax error - can't find = in \"e\". Must be of the form: name=value")
    ;
  }

  @Test
  public void test_labels_multi() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\nLABEL a=b\nLABEL b=c");

    // THEN
    assertEquals(ImmutableMap.of("a", "b", "b", "c"), testee.getLabels());
  }

  @Test
  public void test_label_escaped() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM scratch\nLABEL \"a.s\"=\"b\\\" a\"");

    // THEN
    assertEquals(ImmutableMap.of("a.s", "b\" a"), testee.getLabels());
  }

  @Test
  public void test_label_escaped_max() {
    // GIVEN / WHEN
    String m = TestUtil.repeat("b\\\" a", 2500);
    Dockerfile testee = Dockerfile.parse("FROM scratch\nLABEL \"a.s\"=\"" + m + "\"");

    // THEN
    assertEquals(ImmutableMap.of("a.s", TestUtil.repeat("b\" a", 2500)), testee.getLabels());
  }

  @Test
  public void test_label_multi() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM scratch\n"
        + "LABEL a=a\\\n"
        + "      b=\"n\\\nn\""
    );

    // THEN
    assertEquals(ImmutableMap.of("a", "a", "b", "nn"), testee.getLabels());
  }

  @Test
  public void test_label_multi2() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM scratch\n"
        + "LABEL a=\"a\\n\\\nb\""
    );

    // THEN
    assertEquals(ImmutableMap.of("a", "a\nb"), testee.getLabels());
  }

  @Test
  public void test_label_multi3() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM scratch\n"
        + "LABEL a=\"a\\\n  b\""
    );

    // THEN
    assertEquals(ImmutableMap.of("a", "a  b"), testee.getLabels());
  }

  @Test
  public void test_label_escaped_comment() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM scratch\n"
        + "LABEL a=\"\\#\""
    );

    // THEN
    assertEquals(ImmutableMap.of("a", "#"), testee.getLabels());
  }

  @Test
  public void test_labels_multi_two() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\nLABEL \"a.s\"=\"b\" \"b\"=\"c\"");

    // THEN
    assertEquals(ImmutableMap.of("a.s", "b", "b", "c"), testee.getLabels());
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
    Dockerfile testee = Dockerfile.parse("FROM a\nLABEL a=b\\\n   b=c");

    // THEN
    assertEquals(ImmutableMap.of("a", "b", "b", "c"), testee.getLabels());
  }

  @Test
  public void testMinimal() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM debian:stretch-slim");

    // THEN
    assertEquals("debian:stretch-slim", testee.getFrom());
    assertEquals(ImmutableMap.of(), testee.getLabels());
  }

  @Test
  public void testMinimal_multistaged() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseJ("FROM debian:stretch-slim\nFROM alpine:3.7");

    // THEN
    assertEquals("alpine:3.7", testee.getFrom());
  }

  @Test
  public void test_empty() {
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Dockerfile.parseJ(""))
        .withMessage("Dockerfile cannot be empty")
    ;

  }

  @Test
  public void test_empty_var() {
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Dockerfile.parseJ(" \n\t"))
        .withMessage("Dockerfile cannot be empty")
    ;

  }

  @Test
  public void test_start_with_token_strict() {
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Dockerfile.parseJStrict("LABEL a=b"))
        .withMessage("Dockerfile must start with FROM")
    ;
  }

  @Test
  public void test_start_with_no_token() {
    Assertions.assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Dockerfile.parseJ("a=b"))
        .withMessage("Invalid Dockerfileline: 'a=b'")
    ;
  }

  @Test
  public void test_env_equal() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseJ("FROM a\nENV myName=\"John Doe\" myDog=Rex\\ The\\ Dog \\\n"
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
    Dockerfile testee = Dockerfile.parseJ("FROM scratch\nLABEL test=${some}\n");
    // THEN
    // TODO unclear what will happen
  }

  @Test
  public void test_var_escaped() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseJ("FROM scratch\nLABEL test=\\${some:invalid}\n");
    // THEN
    assertEquals(ImmutableMap.of("test", "${some:invalid}"), testee.getLabels());
  }

  @Test
  public void test_invalid_var() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseJ("FROM scratch\nLABEL test=${some:invalid}\n");
    // THEN
    // failed to process "${some:invalid}": unsupported modifier (i) in substitution
    // TODO test for exception
  }

  @Test
  public void test_copy() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseJ("FROM scratch\nCOPY a b\n");
    // THEN
    assertEquals(ImmutableMap.of("a", "b"), testee.getCopy());
  }

  @Test
  public void test_copy_two() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parseJ("FROM scratch\nCOPY a b\nCOPY ./from /to/other\n");
    // THEN
    assertEquals(ImmutableMap.of("a", "b", "./from", "/to/other"), testee.getCopy());
  }

  @Test
  public void test_file() {
    // GIVEN
    File content = Dockerfile.resourceFile("samples/Nginx");

    // WHEN
    Dockerfile testee = Dockerfile.parseJ(content);

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
    Dockerfile testee = Dockerfile.parseJStrict(content);

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
        .isThrownBy(() -> Dockerfile.parseJ(content))
        .withMessageStartingWith("n.a ("); // No such file or directory)

  }

}
