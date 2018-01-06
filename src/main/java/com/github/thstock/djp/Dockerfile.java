package com.github.thstock.djp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

public class Dockerfile {
  private static final Logger LOGGER = LoggerFactory.getLogger(Dockerfile.class);
  private static final String FROM = "FROM";
  private static final String LABEL = "LABEL";
  private static final String ENV = "ENV";
  private static final String RUN = "RUN";
  private final String LC = "\\";
  private final ImmutableList<String> tokens = ImmutableList.of("#", LC, FROM, LABEL, ENV, RUN);
  final ImmutableList<String> allLines;
  final ImmutableList<String> lines;

  Dockerfile(File file, boolean strict) {
    this(lines(file), strict);
  }

  Dockerfile(String content, boolean strict) {
    this(lines(content), strict);
  }

  Dockerfile(ImmutableList<String> allLines, boolean strict) {
    this.allLines = allLines;
    this.lines = XStream.from(allLines).filterNot(in -> in.trim().isEmpty()).toList();

    ImmutableList<String> potentialTokens = XStream.from(lines)
        .map(in -> in.replaceFirst("(^[^ \t]+).*", "$1"))
        .toList();
    ImmutableList<String> invalids = XStream.from(potentialTokens)
        .filterNot(tokens::contains)
        .toList();
    if (!invalids.isEmpty()) {
      throw new IllegalStateException("invalid token(s): " + invalids);
    }
    if (strict && !XStream.from(potentialTokens).head().equals(FROM)) {
      throw new IllegalStateException("Dockerfile must start with FROM");
    }
  }

  private static ImmutableList<String> lines(String content) {
    return toLines(CharSource.wrap(content));
  }

  private static ImmutableList<String> lines(File file) {
    return toLines(Files.asCharSource(file, StandardCharsets.UTF_8));
  }

  private static ImmutableList<String> toLines(CharSource charSource) {
    try {
      ImmutableList<String> lines = charSource.readLines();
      if (lines.isEmpty() || XStream.from(lines.stream()).filterNot(l -> l.trim().isEmpty()).isEmpty()) {
        throw new IllegalStateException("Dockerfile cannot be empty");
      }
      return lines;
    } catch (FileNotFoundException e) {
      throw new UncheckedIOException(e.getMessage(), e);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  static File resourceFile(String resouce) {
    URL resource = Resources.getResource(resouce);
    try {
      return new File(resource.toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Like docker build
   */
  public static Dockerfile parse(String content) {
    return new Dockerfile(content, false);
  }

  /**
   * Like docker build
   */
  public static Dockerfile parse(File file) {
    return new Dockerfile(file, false);
  }

  public static Dockerfile parseStrict(String content) {
    return new Dockerfile(content, true);
  }

  public static Dockerfile parseStrict(File file) {
    return new Dockerfile(file, true);
  }

  public String getFrom() {
    return XStream.from(allLines)
        .filter(l -> l.startsWith("FROM"))
        .map(l -> l.replaceFirst(FROM + " ", ""))
        .last();
  }
}
