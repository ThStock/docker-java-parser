package com.github.thstock.djp;

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

}
