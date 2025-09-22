# Introduction

Maven plugin for [jpackage](https://openjdk.org/jeps/392).

## Goals Overview

* [jpackage:jpackage](./jpackage-mojo.html) generates application package.

## Usage

  General instructions on how to use the JPackage Maven Plugin can be found on the [usage page](./usage.md). Some more
  specific use cases are described in the examples given below.

  If you feel like the plugin is missing a feature or has a defect, you can fill a feature request or bug report in our
  [issue management system](https://github.com/petr-panteleyev/jpackage-maven-plugin/issues). When creating a new issue,
  please provide a comprehensive description of your concern. Especially for fixing bugs it is crucial that the developers
  can reproduce your problem. For this reason, entire debug logs, POMs or most preferably little demo projects attached
  to the issue are very much appreciated.

## Examples

To provide you with better understanding of some usages of the JPackage Maven Plugin,
you can take a look into the following examples:

* [Image with full JRE](./examples/full-jre.md)
* [Configuration via executions](./examples/executions.md)
* [Configuration via profiles](./examples/profiles.md)
* [Collecting jar files](./examples/collecting-components.md)
* [Using jlink output](./examples/jlink.md)

## Packaging Tool User's Guide

* [Release 25](https://docs.oracle.com/en/java/javase/25/jpackage/packaging-tool-user-guide.pdf)
* [Release 21](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-tool-user-guide.pdf)
* [Release 17](https://docs.oracle.com/en/java/javase/17/jpackage/packaging-tool-user-guide.pdf)
