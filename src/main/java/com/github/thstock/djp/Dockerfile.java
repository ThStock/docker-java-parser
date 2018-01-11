package com.github.thstock.djp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.thstock.djp.util.Tuple;
import com.github.thstock.djp.util.XStream;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
  private ImmutableList<DockerfileLine> tokenLines;
  private String from;
  private ImmutableMap<String, String> labels;

  Dockerfile(File file, boolean strict) {
    this(lines(file), strict);
  }

  Dockerfile(String content, boolean strict) {
    this(lines(content), strict);
  }

  Dockerfile(ImmutableList<String> allLines, boolean strict) {
    this.allLines = allLines;
    this.lines = XStream.from(allLines).filterNot(in -> in.trim().isEmpty()).toList();

    if (allLines.isEmpty() || lines.isEmpty()) {
      throw new IllegalStateException("Dockerfile cannot be empty");
    }

    ImmutableList<String> elements = joindLines(lines, LC);
    tokenLines = XStream.from(elements)
        .map(DockerfileLine::from)
        .toList();
    ImmutableList<String> invalids = XStream.from(tokenLines)
        .map(DockerfileLine::getToken)
        .filterNot(tokens::contains)
        .toList();
    if (!invalids.isEmpty()) {
      throw new IllegalStateException("invalid token(s): " + invalids);
    }

    try {
      from = XStream.from(tokenLines)
          .filter(l -> l.isToken(FROM))
          .map(DockerfileLine::getValue)
          .last();
    } catch (NoSuchElementException e) {
      if (strict) {
        throw new IllegalStateException("Dockerfile must start with FROM");
      } else {
        from = "";
      }
    }

    labels = XStream.from(tokenLines)
        .filter(l -> l.isToken(LABEL))
        .map(DockerfileLine::valueTokens)
        .flatMap(in -> {
          ImmutableList.Builder<ImmutableList<String>> builder = ImmutableList.builder();
          ImmutableList<String> l = in;
          while (l.size() > 2) {
            ImmutableList<String> strings = XStream.from(l).take(3).toList();
            l = XStream.from(l).drop(3).toList();
            builder.add(strings);
          }

          return XStream.from(builder.build());
        })
        .toMap(Dockerfile::getObject);
  }

  static Tuple<String, String> getObject(ImmutableList<String> line) {
    return Tuple.of(line.get(0), line.get(2));
  }

  static ImmutableList<String> joindLines(ImmutableList<String> lines, String lc) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    String buff = "";
    for (int i = 0; i < lines.size(); i++) {
      String l = lines.get(i);
      String trim = l.trim();
      if (!trim.endsWith(lc)) {
        buff += l;
        builder.add(buff);
        buff = "";
      } else {
        String substring = trim.substring(0, trim.length() - 1);
        buff += substring + "\n";
      }
    }
    return builder.build();
  }

  public String getFrom() {
    return from;
  }

  public ImmutableMap<String, String> getLabels() {
    return labels;
  }

  private static ImmutableList<String> lines(String content) {
    return toLines(CharSource.wrap(content));
  }

  private static ImmutableList<String> lines(File file) {
    return toLines(Files.asCharSource(file, StandardCharsets.UTF_8));
  }

  private static ImmutableList<String> toLines(CharSource charSource) {
    try {
      return charSource.readLines();
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

}
