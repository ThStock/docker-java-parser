package com.github.thstock.djp;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class JavaParserTest {

    @Test
    public void testJoined_nothing() {
        ImmutableList<String> lines = JavaParser.joindLines(ImmutableList.of("a", "b"), ",");

        Assertions.assertThat(lines).isEqualTo(ImmutableList.of("a", "b"));
    }

    @Test
    public void testJoined() {
        ImmutableList<String> lines = JavaParser.joindLines(ImmutableList.of("a,", "b"), ",");

        Assertions.assertThat(lines).isEqualTo(ImmutableList.of("a\nb"));
    }

    @Test
    public void testJoinedNewlines() {
        ImmutableList<String> lines = JavaParser.joindLines(ImmutableList.of("a\\\n", "b"), "\\");

        Assertions.assertThat(lines).isEqualTo(ImmutableList.of("a\nb"));
    }

    @Test
    public void testJoinedNewlinesWin() {
        ImmutableList<String> lines = JavaParser.joindLines(ImmutableList.of("a\\\r\n", "b"), "\\");

        Assertions.assertThat(lines).isEqualTo(ImmutableList.of("a\nb"));
    }

    @Test
    public void testJoined_var() {
        ImmutableList<String> lines = JavaParser.joindLines(ImmutableList.of("a,a,", "b"), ",");

        Assertions.assertThat(lines).isEqualTo(ImmutableList.of("a,a\nb"));
    }
}
