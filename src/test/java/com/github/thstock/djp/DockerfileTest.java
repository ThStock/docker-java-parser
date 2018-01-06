package com.github.thstock.djp;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.UncheckedIOException;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

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
  public void testMinimal() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM debian:stretch-slim");

    // THEN
    assertEquals(1, testee.allLines.size());
    assertEquals("debian:stretch-slim", testee.getFrom());
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
        .withMessage("invalid token(s): [a=b]")
    ;
  }

  @Test
  @Ignore
  public void test_file() {
    // GIVEN
    File content = Dockerfile.resourceFile("samples/Nginx");

    // WHEN
    Dockerfile testee = Dockerfile.parse(content);

    // THEN
    assertEquals(99, testee.allLines.size());
  }

  @Test
  @Ignore
  public void test_file_strict() {
    // GIVEN
    File content = Dockerfile.resourceFile("samples/Nginx");

    // WHEN
    Dockerfile testee = Dockerfile.parseStrict(content);

    // THEN
    assertEquals(99, testee.allLines.size());
  }

  @Test
  public void test_file_not_existing() {
    // GIVEN
    File content = new File("n.a");

    // WHEN / THEN
    Assertions.assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> Dockerfile.parse(content))
        .withMessage("n.a (No such file or directory)");

  }
}
