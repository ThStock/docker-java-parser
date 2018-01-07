package com.github.thstock.djp;

import com.github.thstock.djp.util.HashEquals;

class DockerfileLine extends HashEquals {
  final String token;
  final String value;

  DockerfileLine(String token, String value) {
    this.token = token;
    this.value = value;
  }

  String getToken() {
    return token;
  }

  String getValue() {
    return value;
  }

  public static DockerfileLine from(String line) {
    String pattern = "(^[^ \t]+)[ \t]+(.*)";
    String token = line.replaceFirst(pattern, "$1");
    String value = line.replaceFirst(pattern, "$2");
    return new DockerfileLine(token, value);
  }

  public boolean isToken(String token) {
    return this.token.equals(token);
  }
}
