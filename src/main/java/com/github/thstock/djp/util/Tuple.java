package com.github.thstock.djp.util;

class Tuple<T1, T2> extends HashEquals {

  private final T1 key;
  private final T2 value;

  Tuple(T1 key, T2 value) {
    this.key = key; // TODO check null
    this.value = value; // TODO check null
  }

  public T1 getKey() {
    return key;
  }

  public T2 getValue() {
    return value;
  }

  public static <K, V> Tuple<K, V> of(K key, V value) {
    return new Tuple<K, V>(key, value);
  }
}
