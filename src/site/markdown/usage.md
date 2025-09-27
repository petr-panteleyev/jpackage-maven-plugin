# Usage

For detailed information about ```jpackage``` please refer to
[Packaging Tool User's Guide](https://docs.oracle.com/en/java/javase/25/jpackage/packaging-tool-user-guide.pdf).

The goals of JPackage Plugin are not bound to any phase and must be executed manually after ```package```:

```shell
mvn clean verify jpackage:jpackage
```

## Finding jpackage

Plugin searches for ```jpackage``` executable using the following priority list:

1. ```maven-toolchains-plugin``` configured in the project. Toolchain "jdk" will be queried for 
tool = "jpackage".

2. ```java.home``` system property.

## Configuration

Full details about plugin configuration and respective ```jpackage``` options can be found in
[jpackage:jpackage](./jpackage-mojo.html) goal description.

```jpackage``` defines a number of OS-specific options. They can be recognized by their prefix: ```win```,
```mac``` or ```linux```. Plugin detects current OS using ```os.name``` system property and ignores corresponding 
irrelevant parameters. This allows to set all OS-specific parameters in the root configuration block.

If some common parameters like ```icon``` depend on OS they should be separated with either 
[executions](./examples/executions.md) or [profiles](./examples/profiles.md).

### Relative Path Resolution

Parameters of type ```File``` are resolved to absolute paths. To avoid unexpected results it is advised to supply
absolute paths explicitly using Maven variables such as ```${project.basedir}```.

## Assembling Dependencies

This plugin does not utilize any classpath or modulepath from the build. This is a design decision made to avoid 
unexpected effects especially with JavaFX maven artifacts.

All required dependencies must be specified via plugin configuration. One way to do it is to gather all dependencies
using ```maven-dependency-plugin``` as shown in [this example](examples/dependencies.md).

## Dry Run Mode

To print jpackage parameters without executing jpackage set ```jpackage.dryRun``` property to ```true```.

```shell
mvn clean verify jpackage:jpackage -Djpackage.dryRun=true
```
