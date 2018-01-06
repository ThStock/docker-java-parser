package com.github.thstock.djp;

import static org.junit.Assert.assertEquals;

import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class XStreamTest {

  @Test
  public void test_filter() {
    XStream<String> testee = XStream.from(Stream.of("a", "b"));

    ImmutableList<String> result = testee.filter(p -> p.equals("a")).toList();

    Assertions.assertThat(result)
        .isEqualTo(ImmutableList.of("a"));
  }

  @Test
  public void test_filter_not() {
    XStream<String> testee = XStream.from("a", "b");

    ImmutableList<String> result = testee.filterNot(p -> p.equals("a")).toList();

    Assertions.assertThat(result)
        .isEqualTo(ImmutableList.of("b"));
  }

  @Test
  public void test_isEmpty() {
    Assertions.assertThat(XStream.empty().isEmpty()).isTrue();
  }

  @Test
  public void test_last_predicate() {
    assertEquals("-a2", XStream.from("-a", "-a2", "-b").last(in -> in.contains("a")));
  }

  @Test
  public void test_last() {
    assertEquals("-a2", XStream.from("-a", "-a2", "-b").filter(in -> in.contains("a")).last());
  }

  @Test
  public void test_map_last() {
    assertEquals("-a2", XStream.from("-a").map(in -> in + "2").last());
  }

  @Test
  public void test_head() {
    assertEquals("2", XStream.from("2", "1").head());
  }
}
