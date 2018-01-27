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
  private static final String EXPOSE = "EXPOSE";
  private static final String STOPSIGNAL = "STOPSIGNAL";
  private static final String CMD = "CMD";
  private final String LC = "\\";
  private final ImmutableList<String> tokens = ImmutableList.of("#", LC, FROM, LABEL, ENV, RUN, EXPOSE, STOPSIGNAL, CMD);
  final ImmutableList<String> allLines;
  final ImmutableList<String> lines;
  private ImmutableList<DockerfileLine> tokenLines;
  private String from;
  private ImmutableMap<String, String> labels;
  private ImmutableMap<String, String> env;


  Dockerfile(File file, boolean strict) {
    this(lines(file), strict);
  }

  Dockerfile(String content, boolean strict) {
    this(lines(content), strict);
  }

  Dockerfile(ImmutableList<String> allLines, boolean strict) {
    this.allLines = allLines;
    this.lines = XStream.from(allLines)
        .filterNot(in -> in.trim().isEmpty())
        .filterNot(in -> in.trim().startsWith("#"))
        .toList();

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
            int drop = 3;
            ImmutableList<String> strings = XStream.from(l).take(drop).toList();

            String key = strings.get(0);
            String equal = strings.get(1);
            String value = strings.get(2);
            String last = XStream.from(l).take(4).last();
            if (last.equals(" ")) {
              drop = 4;
            }
            if (!strict && equal.equals(" ") && value.equals("=")) {
              ImmutableList<String> strings2 = XStream.from(l).take(5).toList();
              equal = value;
              value = "= " + strings2.get(4);
              drop = 5;
            } else if (!strict && equal.equals("=") && value.equals("=")) {
              ImmutableList<String> strings2 = XStream.from(l).take(4).toList();
              equal = value;
              value = strings2.get(2);
              if (strings2.size() > 3) {
                value += strings2.get(3);
              }
              drop = 4;
            }
            if (!equal.equals("=")) {
              throw new IllegalStateException(
                  "Syntax error - can't find = in \"" + value + "\". Must be of the form: name=value");
            }

            builder.add(ImmutableList.of(key, equal, value));
            l = XStream.from(l).drop(drop).toList();
          }

          return XStream.from(builder.build());
        })
        .toMap(Dockerfile::getObject);

    env = XStream.from(tokenLines)
        .filter(l -> l.isToken(ENV))
        .map(DockerfileLine::valueTokens)
        .flatMap(in -> {
          ImmutableList.Builder<ImmutableList<String>> builder = ImmutableList.builder();
          ImmutableList<String> l = in;
          while (l.size() > 2) {
            if (l.contains("=")) {
              int drop = 3;
              ImmutableList<String> strings = XStream.from(l).take(drop).toList();

              String key = strings.get(0);
              String equal = strings.get(1);
              String value = strings.get(2);
              String last = XStream.from(l).take(4).last();
              if (last.equals(" ")) {
                drop = 4;
              }
              builder.add(ImmutableList.of(key, equal, value));
              l = XStream.from(l).drop(drop).toList();
            } else {
              builder.add(ImmutableList.of(in.get(0), "=", XStream.from(in).drop(2).mkString("")));
              l = ImmutableList.of();
            }

          }

          return XStream.from(builder.build());
        })
        .toMap(Dockerfile::getObject);
  }

  static Tuple<String, String> getObject(ImmutableList<String> line) {
    String key = line.get(0);
    String value = line.get(2);
    return Tuple.of(key, value);
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

  public String getCmdShell() {
    // strict: more then one cmd exception; else use latest cmd
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  public ImmutableList<String> getCmd() {
    // strict: more then one cmd exception; else use latest cmd
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  public ImmutableMap<Integer, String> getExpose() {
    // e.g. 80/tcp, 99/udp
    // strict: redundant expose lines exceptions; else use all distinct
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  ImmutableMap<String, String> getEnv() {
    return env;
  }

  void getAdd() {
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  void getCopy() {
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  void getEntrypoint() {
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  void getVolume() {
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  void getUser() {
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  void getWorkdir() {
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  void getArg() {
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  void getOnbuild() {
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  void getStopsignal() {
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  void getHealthcheck() {
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  void getShell() {
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  public ImmutableList<String> getRunLines() {
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
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
