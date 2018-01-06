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
  final ImmutableList<String> lines;

  Dockerfile(File file) {
    lines = lines(file);
  }

  Dockerfile(String content) {
    lines = lines(content);
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

  public static Dockerfile parse(String content) {
    return new Dockerfile(content);
  }

  public static Dockerfile parse(File file) {
    return new Dockerfile(file);
  }

  public String getFrom() {
    return XStream.from(lines)
        .filter(l -> l.startsWith("FROM"))
        .map(l -> l.replaceFirst("FROM ", ""))
        .last();
  }
}
