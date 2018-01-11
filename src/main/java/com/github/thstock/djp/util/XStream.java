package com.github.thstock.djp.util;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

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

  public <R> XStream<R> map(Function<? super T, ? extends R> f) {
    return stream(stream().map(f));
  }

  public <R> XStream<R> flatMap(Function<? super T, ? extends XStream<R>> f) {
    return stream(stream().flatMap(in -> f.apply(in).stream()));
  }

  public T last(Predicate<? super T> predicate) {
    return filter(predicate).last();
  }

  public T last() {
    return Iterables.getLast(toList());
  }

  Stream<T> stream() {
    return inner;
  }

  static <T> XStream<T> from(T... elements) {
    return new XStream<>(Stream.of(elements));
  }

  public static <T> XStream<T> from(List<T> elements) {
    return new XStream<>(elements.stream());
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

  public XStream<T> drop(int limit) {
    if (limit < 0) {
      return from(toList());
    } else {
      return from(toList().stream().skip(limit));
    }
  }

  public XStream<T> take(int size) {
    return from(toList().stream().limit(size));
  }

  public T head() {
    return Iterables.getFirst(toList(), null); // TODO check
  }

  public <K, V> ImmutableMap<K, V> toMap(Function<T, Tuple<K, V>> fn) {
    Map<K, V> collect = stream().collect(Collectors.toMap(k -> fn.apply(k).getKey(), v -> fn.apply(v).getValue()));
    return ImmutableMap.copyOf(collect);
  }

  public ImmutableMap<String, String> toMap(Splitter splitter) {
    return map(Object::toString).toMap(in -> {
      List<String> strings = splitter.splitToList(in);
      if (strings.size() != 2) {
        throw new IllegalStateException("invalid split: " + strings);
      }
      return Tuple.of(strings.get(0), strings.get(1));
    });
  }
}
