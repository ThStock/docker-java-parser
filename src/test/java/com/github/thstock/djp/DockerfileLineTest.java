package com.github.thstock.djp;

import static org.junit.Assert.assertEquals;

import org.assertj.core.api.Assertions;
import org.junit.Test;

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

}
