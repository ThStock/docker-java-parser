package com.github.thstock.djp;

import com.github.thstock.djp.util.Tuple;
import com.github.thstock.djp.util.XStream;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

class JavaParser {

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

    static Dockerfile parse(File file, boolean strict) {
        return parse(lines(file), strict);
    }

    static Dockerfile parse(String content, boolean strict) {
        return parse(lines(content), strict);
    }

    static Dockerfile parse(ImmutableList<String> allLines, boolean strict) {
        ImmutableList<String> lines = XStream.from(allLines)
                .filterNot(in -> in.trim().isEmpty())
                .filterNot(in -> in.trim().startsWith("#"))
                .toList();

        if (allLines.isEmpty() || lines.isEmpty()) {
            throw new IllegalStateException("Dockerfile cannot be empty");
        }

        ImmutableList<String> elements = joindLines(lines, Dockerfile.LC);
        ImmutableList<DockerfileLine> tokenLines = XStream.from(elements)
                .map(DockerfileLine::from)
                .toList();
        ImmutableList<String> invalids = XStream.from(tokenLines)
                .map(DockerfileLine::getToken)
                .filterNot(Dockerfile.tokens::contains)
                .toList();
        if (!invalids.isEmpty()) {
            throw new IllegalStateException("invalid token(s): " + invalids);
        }
        String from;
        try {
            from = XStream.from(tokenLines)
                    .filter(l -> l.isToken(Dockerfile.FROM))
                    .map(DockerfileLine::getValue)
                    .last();
        } catch (NoSuchElementException e) {
            if (strict) {
                throw new IllegalStateException("Dockerfile must start with FROM");
            } else {
                from = "";
            }
        }
        ImmutableMap<String, String> labels = XStream.from(tokenLines)
                .filter(l -> l.isToken(Dockerfile.LABEL))
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
                .toMap(JavaParser::getObject);
        ImmutableMap<String, String> env = XStream.from(tokenLines)
                .filter(l -> l.isToken(Dockerfile.ENV))
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
                .toMap(JavaParser::getObject);

        ImmutableMap<String, String> copy = XStream.from(tokenLines)
                .filter(l -> l.isToken(Dockerfile.COPY))
                .map(DockerfileLine::valueTokens)
                .flatMap(in -> {
                    ImmutableList.Builder<ImmutableList<String>> builder = ImmutableList.builder();
                    if (in.size() == 3) {
                        builder.add(in);
                        return XStream.from(builder.build());
                    } else {
                        // TODO error handling
                        return XStream.from(builder.build());
                    }
                })
                .toMap(JavaParser::getObject);
        return new Dockerfile(allLines, lines, from, labels, env, copy);
    }
}
