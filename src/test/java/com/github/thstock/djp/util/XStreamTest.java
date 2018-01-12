package com.github.thstock.djp.util;

import static org.junit.Assert.assertEquals;

import java.util.function.Function;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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

  @Test
  public void test_drop() {
    assertEquals(XStream.from("1").toList(), XStream.from("2", "1").drop(1).toList());
  }

  @Test
  public void test_take() {
    assertEquals(XStream.from("2").toList(), XStream.from("2", "1").take(1).toList());
  }

  @Test
  public void test_mkString() {
    assertEquals("21", XStream.from("2", "1").mkString());
    assertEquals("a-b", XStream.from("a", "b").mkString("-"));
    assertEquals("1->2", XStream.from(1, 2).mkString("->"));
  }

  @Test
  public void test_toMapFn() {
    Function<String, Tuple<String, Integer>> fn = in -> {
      String[] split = in.split("=");
      return Tuple.of(split[0], Integer.valueOf(split[1]));
    };
    assertEquals(ImmutableMap.of("2", 1), XStream.from("2=1").toMap(fn));
  }

  @Test
  public void test_toMapSplitter() {
    Splitter splitter = Splitter.on("->");
    assertEquals(ImmutableMap.of("a", "a"), XStream.from("a->a").toMap(splitter));
  }
}
