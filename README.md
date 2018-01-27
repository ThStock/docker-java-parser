## docker-java-parser
[![Build Status](https://travis-ci.org/ThStock/docker-java-parser.svg?branch=master)](https://travis-ci.org/ThStock/docker-java-parser)
[![mvnrepository](https://img.shields.io/maven-central/v/com.github.thstock/docker-java-parser.svg)](https://mvnrepository.com/artifact/com.github.thstock/docker-java-parser)

A parser for Dockerfiles in Java.

```java
Dockerfile df = Dockerfile.parse(new File("Dockerfile"));
df.getFrom(); // alpine:3.7
df.getLabels(); // {maintainer=ThStock@example.org}
df.getEnv(); // {VERSION=9}

```

## See also
* https://docs.docker.com/engine/reference/builder/
