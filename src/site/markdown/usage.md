## Usage

For detailed information about ```jpackage``` please refer to
[Packaging Tool User's Guide](https://docs.oracle.com/en/java/javase/22/jpackage/packaging-tool-user-guide.pdf).

### Finding jpackage

Plugin searches for ```jpackage``` executable using the following priority list:

1. ```maven-toolchains-plugin``` configured in the project. Toolchain "jdk" will be queried for 
tool = "jpackage".

2. ```java.home``` system property.

### Configuration

There are generic parameters as well as OS-specific parameters for OS X and Windows.
Plugin determines OS name using ```os.name``` system property in order to configure OS-specific parameters.

Generic parameters should be placed in the root plugin configuration. OS-specific parameters should be separated with
executions or profiles.

See examples:

* [Configuration with executions](./examples/executions.html)
* [Configuration with profiles](./examples/profiles.html)

#### Mandatory Parameters

To enable various configuration approaches mandatory parameters are validated during plugin execution:

* name
* destination

#### Relative Path Resolution

The following plugin parameters define directory or file location:

* destination
* icon
* input
* installDir
* resourceDir 
* modulePath
* runtimeImage
* appImage  
* temp
* licenseFile
* launcher.file
* appContentPath
* macEntitlements
* macDmgContentPath
 
If path is not absolute is will be resolved as relative to ```${project.basedir}```.

One exception is ```installDir``` which is passed as is.

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

### Destination Directory

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

### Assembling Dependencies

Before executing ```jpackage``` all runtime dependencies should be copied into a single folder together with main
application jar. This example shows how to do this via ```maven-dependency-plugin```.

```xml
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <configuration>
            <outputDirectory>target/jmods</outputDirectory>
        </configuration>
    </plugin>
    
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
            <execution>
                <id>copy-dependencies</id>
                <phase>package</phase>
                <goals>
                    <goal>copy-dependencies</goal>
                </goals>
                <configuration>
                    <includeScope>runtime</includeScope>
                    <outputDirectory>target/jmods</outputDirectory>
                </configuration>
            </execution>
        </executions>
    </plugin>

    <plugin>
        <groupId>org.panteleyev</groupId>
        <artifactId>jpackage-maven-plugin</artifactId>
        <configuration>
            <modulePaths>
                <modulePath>target/jmods</modulePath>
            </modulePaths>
        </configuration>
    </plugin>
</plugins>
```

## Dry Run Mode

To print jpackage parameters without executing jpackage set ```jpackage.dryRun``` property to ```true```.

_Example:_

```
mvn clean package jpackage:jpackage@win -Djpackage.dryRun=true
```
