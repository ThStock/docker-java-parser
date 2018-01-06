package com.github.thstock.djp;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class DockerfileTest {

  @Test
  public void test_lines_newline() {
    // GIVEN / WHEN
    Dockerfile testee = Dockerfile.parse("FROM a\r\nLABEL\ta=b");

    // THEN
    assertEquals(ImmutableList.of("FROM a", "LABEL\ta=b"), testee.lines);

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
  public void test_file() {
    // GIVEN
    File content = Dockerfile.resourceFile("samples/Nginx");

    // WHEN
    Dockerfile testee = Dockerfile.parse(content);

    // THEN
    assertEquals(99, testee.lines.size());

  }
}
