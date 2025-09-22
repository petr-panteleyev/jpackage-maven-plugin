## Usage

For detailed information about ```jpackage``` please refer to
[Packaging Tool User's Guide](https://docs.oracle.com/en/java/javase/25/jpackage/packaging-tool-user-guide.pdf).

### Finding jpackage

Plugin searches for ```jpackage``` executable using the following priority list:

1. ```maven-toolchains-plugin``` configured in the project. Toolchain "jdk" will be queried for 
tool = "jpackage".

2. ```java.home``` system property.

### Collecting JAR Files

Before executing ```jpackage``` all image jar files such as main jar and all dependencies should be copied into a 
single folder. This [example](./examples/collecting-components.md) shows how to do this via 
```maven-dependency-plugin```.

### Configuration

There are generic parameters as well as OS-specific parameters for OS X and Windows.
Plugin determines OS name using ```os.name``` system property in order to configure OS-specific parameters.

Generic parameters should be placed in the root plugin configuration. OS-specific parameters should be separated with
[executions](./examples/executions.md) or [profiles](./examples/profiles.md).

#### Mandatory Parameters

To enable various configuration approaches mandatory parameters are validated during plugin execution:

* name
* destination

#### Relative Path Resolution

Parameters of type ```Path``` are resolved to absolute paths. To avoid unexpected results it is advised to supply
absolute paths explicitly using Maven variables such as ```${project.basedir}```.

```xml
<configuration>
    <destination>${project.basedir}/target/dist</destination>
</configuration>
```

#### Java Options

&lt;javaOptions> defines options for JVM running the application. Each option should be specified in a separate 
&lt;option> tag.

_Example:_

```xml
<javaOptions>
    <javaOption>--enable-preview</javaOption>
    <javaOption>-Dfile.encoding=UTF-8</javaOption>
    <javaOption>--add-export</javaOption>
    <javaOption>java.base/sun.security.util=ALL-UNNAMED</javaOption>
</javaOptions>
``` 

#### Destination Directory

```jpackage``` utility fails if generated binary already exists. In order to work around this behaviour there is plugin
boolean option ```removeDestination```. If ```true``` plugin will try to delete directory specified by ```destination```.
This might be useful to relaunch ```jpackage``` task without rebuilding an entire project.

For safety reasons plugin will not process ```removeDestination``` if ```destination``` points to a location outside of
```${project.build.directory}```.

#### Default Command-Line Arguments

Default command line arguments are passed to the main class when the application is started without providing arguments.
Each argument should be specified using &lt;argument> configuration parameter.

_Example:_

```xml
<arguments>
    <argument>SomeArgument</argument>
    <argument>Argument with spaces</argument>
    <argument>Argument with "quotes"</argument>
</arguments>
```

#### Additional Launchers

Additional launchers provide the opportunity to install alternative ways to start an application.

_Example:_

```xml
<launchers>
    <launcher>
        <name>App1</name>
        <file>src/resources/App1.properties</file>
    </launcher>
    <launcher>
        <name>App2</name>
        <file>src/resources/App2.properties</file>
    </launcher>
</launchers>
```

#### File Associations

If you want your application to be started when a user opens a specific type of file, use ```<fileAssociations>``` 
configuration.

_Example:_

```xml
<fileAssociations>
    <fileAssociation>src/properties/java.properties</fileAssociation>
    <fileAssociation>src/properties/cpp.properties</fileAssociation>
</fileAssociations>
```

Note: apparently this option does not work for modular applications.

#### jlink options

Options that are passed to underlying jlink call.

_Example:_

```xml
<jLinkOptions>
    <jLinkOption>--strip-native-commands</jLinkOption>
    <jLinkOption>--strip-debug</jLinkOption>
</jLinkOptions>
```

#### Additional JPackage Options

Additional options allow passing jpackage command line options not supported by the plugin. These options are passed as is without any transformation.

_Example:_

```xml
<additionalOptions>
    <option>--jlink-options</option>
    <option>--bind-services</option>
</additionalOptions>
```


## Dry Run Mode

To print jpackage parameters without executing jpackage set ```jpackage.dryRun``` property to ```true```.

_Example:_

```
mvn clean package jpackage:jpackage@win -Djpackage.dryRun=true
```
