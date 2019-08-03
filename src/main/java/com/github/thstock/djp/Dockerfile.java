package com.github.thstock.djp;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import scala.Tuple4;

public class Dockerfile {
  private static final Logger LOGGER = LoggerFactory.getLogger(Dockerfile.class);
  static final String FROM = "FROM";
  private static final String RUN = "RUN";
  private static final String CMD = "CMD";
  static final String LABEL = "LABEL";
  private static final String MAINTAINER = "MAINTAINER";
  private static final String EXPOSE = "EXPOSE";
  static final String ENV = "ENV";
  private static final String ADD = "ADD";
  static final String COPY = "COPY";
  private static final String ENTRYPOINT = "ENTRYPOINT";
  private static final String VOLUME = "VOLUME";
  private static final String USER = "USER";
  private static final String WORKDIR = "WORKDIR";
  private static final String ARG = "ARG";
  private static final String ONBUILD = "ONBUILD";
  private static final String STOPSIGNAL = "STOPSIGNAL";
  private static final String HEALTHCHECK = "HEALTHCHECK";
  private static final String SHELL = "SHELL";
  static final String LC = "\\";
  static final ImmutableList<String> tokens = ImmutableList.of("#", LC, FROM, RUN, CMD, LABEL, MAINTAINER, EXPOSE, ENV,
      ADD, COPY, ENTRYPOINT, VOLUME, USER, WORKDIR, ARG, ONBUILD, STOPSIGNAL, HEALTHCHECK, SHELL);
  final ImmutableList<String> allLines;
  final ImmutableList<String> lines;
  final String from;
  final ImmutableMap<String, String> labels;
  final ImmutableMap<String, String> env;
  final ImmutableMap<String, String> copy;

  Dockerfile(ImmutableList<String> allLines, ImmutableList<String> lines,
      String from,
      ImmutableMap<String, String> labels,
      ImmutableMap<String, String> env,
      ImmutableMap<String, String> copy) {
    this.allLines = allLines;
    this.lines = lines;
    this.from = from;
    this.labels = labels;
    this.env = env;
    this.copy = copy;
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

  public ImmutableMap<String, String> getEnv() {
    return env;
  }

  void getAdd() {
    throw new UnsupportedOperationException("Will be implemented later"); // TODO
  }

  ImmutableMap<String, String> getCopy() {
    return copy;
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

  static File resourceFile(String resouce) {
    URL resource = Resources.getResource(resouce);
    try {
      return new File(resource.toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  /*
   * Like docker build
   */
  @Deprecated
  public static Dockerfile parseJ(String content) {
    return JavaParser.parse(content, false);
  }

  /*
   * Like docker build
   */
  @Deprecated
  public static Dockerfile parseJ(File file) {
    return JavaParser.parse(file, false);
  }

  @Deprecated
  public static Dockerfile parseJStrict(String content) {
    return JavaParser.parse(content, true);
  }

  @Deprecated
  public static Dockerfile parseJStrict(File file) {
    return JavaParser.parse(file, true);
  }

  private static String getContent(File file) {
    try {
      return Files.toString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /*
   * Like docker build
   */
  public static Dockerfile parse(String content) {
    return toDf(ScalaParser.doParse(content, false));
  }

  /*
   * Like docker build
   */
  public static Dockerfile parse(File file) {
    return parse(getContent(file));
  }

  public static Dockerfile parseStrict(String content) {
    return toDf(ScalaParser.doParse(content, true));
  }

  private static Dockerfile toDf(Tuple4<String, ImmutableMap<String, String>, ImmutableMap<String, String>, ImmutableMap<String, String>> p) {
    return new Dockerfile(ImmutableList.of(), ImmutableList.of(), p._1(), p._2(), p._3(), p._4());
  }

  public static Dockerfile parseStrict(File file) {
    return parseStrict(getContent(file));
  }
}
