package com.github.thstock.djp;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

public class XStream<T> {

  private final Stream<T> inner;

  XStream(Stream<T> stream) {
    this.inner = stream;
  }

  public XStream<T> filter(Predicate<? super T> predicate) {
    return stream(stream().filter(predicate));
  }

  public XStream<T> filterNot(Predicate<? super T> predicate) {
    return filter(p -> !predicate.test(p));
  }

  private Stream<T> stream() {
    return inner;
  }

  static <T> XStream<T> from(T... elements) {
    return new XStream<>(Stream.of(elements));
  }

  static <T> XStream<T> from(Stream<T> stream) {
    return new XStream<>(stream);
  }

  private static <T> XStream<T> stream(Stream<T> stream) {
    return new XStream<>(stream);
  }

  public boolean isEmpty() {
    return toList().isEmpty();
  }

  public ImmutableList<T> toList() {
    return ImmutableList.copyOf(stream().collect(Collectors.toList()));
  }

  public static <T> XStream<T> empty() {
    return new XStream<>(Stream.empty());
  }
}
