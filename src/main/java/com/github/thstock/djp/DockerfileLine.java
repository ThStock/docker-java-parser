package com.github.thstock.djp;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.thstock.djp.util.HashEquals;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

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

  public ImmutableList<String> valueTokens() {
    ArrayList<String> objects = Lists.newArrayList();
    String token = "";
    boolean quoteOpen = false;
    boolean newline = false;
    for (char c : getValue().toCharArray()) {
      if (c == '"' || c == '\'') {
        quoteOpen = !quoteOpen;
        if (!quoteOpen) {
          if (newline) {
            newline = false;
          }
          applyNonEmpty(objects, token);
          token = "";
        }
      }
      if (!quoteOpen) {
        if (c == '=') {
          applyNonEmpty(objects, token);
          objects.add("=");
          token = "";
        } else if (c == '\n' || c == ' ' || c == '\t') {
          newline = true;
          applyNonEmpty(objects, token.trim());
          if (!Iterables.getLast(objects).equals(" ")) {
                objects.add(" ");
          }
          token = "";
        } else {
          if (c != '"' && c != '\'') {
            token += c;
          }
        }
      } else {
        if (c != '"' && c != '\'') {
          token += c;
        }
      }

    }
    if (token.length() > 0) {
      if (newline) {
        objects.add(token.replaceFirst("^[ \t]+", ""));
      } else {
        objects.add(token);
      }

    }
    return ImmutableList.copyOf(objects);
  }

  private void applyNonEmpty(ArrayList<String> objects, String token) {
    if (token.trim().length() > 0) {
      objects.add(token);
    }
  }
}
