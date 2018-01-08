package com.github.thstock.djp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.thstock.djp.util.HashEquals;

class DockerfileLine extends HashEquals {
  final String token;
  final String value;
  private static Pattern p = Pattern.compile("(^[^ \t]+[ \t]+)(.+)");

  public static DockerfileLine from(String line) {
    Matcher matcher = p.matcher(line);
    if (!matcher.find()) {
      throw new IllegalStateException("Invalid Dockerfileline: '" + line + "'");
    }
    String token = matcher.group(1);
    String value = line.substring(token.length());
    return new DockerfileLine(token.trim(), value);
  }

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

  public boolean isToken(String token) {
    return this.token.equals(token);
  }
}
