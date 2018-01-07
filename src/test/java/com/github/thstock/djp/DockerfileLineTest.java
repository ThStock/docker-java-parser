package com.github.thstock.djp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DockerfileLineTest {

  @Test
  public void testLine() {
    DockerfileLine testee = DockerfileLine.from("LABEL a=a");

    assertEquals(new DockerfileLine("LABEL", "a=a"), testee);
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

}
