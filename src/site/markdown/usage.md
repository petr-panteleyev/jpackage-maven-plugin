## Usage

For detailed information about ```jpackage``` please refer to [Packaging Tool User's Guide](https://docs.oracle.com/en/java/javase/15/jpackage/packaging-tool-user-guide.pdf).

### Finding jpackage

Plugin searches for ```jpackage``` executable using the following priority list:

1. Environment variable ```JPACKAGE_HOME```. If specific version of jpackage is required then this variable must point
to the corresponding JDK same way as ```JAVA_HOME```.

2. ```maven-toolchains-plugin``` configured in the project. Toolchain "jdk" will be queried for 
tool = "jpackage".

3. ```java.home``` system property.

### Configuration

There are generic parameters as well as OS-specific parameters for OS X and Windows.
Plugin determines OS name using ```os.name``` system property in order to configure OS-specific parameters.

Generic parameters should be placed in the root plugin configuration. OS-specific parameters should be separated via
executions. Id of the executions are irrelevant however using OS names improves usability.

*Example:*

```xml
<executions>
    <execution>
        <id>mac</id>
        <configuration>
            <icon>icons/icons.icns</icon>
        </configuration>
    </execution>
    <execution>
        <id>win</id>
        <configuration>
            <icon>icons/icon.ico</icon>
            <winMenu>true</winMenu>
        </configuration>
    </execution>
</executions>
```
With above execution configuration the following command lines can be used:

* for OS X package: ```mvn jpackage:jpackage@mac```

* for Windows package: ```mvn jpackage:jpackage@win```

#### Relative Path Resolution

The following plugin parameters define directory or file location:
* destination
* icon
* input
* installDir
* resourceDir 
* modulePath
* runtimeImage
* temp
 
If path is not absolute is will be resolved as relative to ```${project.basedir}```.

#### Java Options

&lt;javaOptions> defines options for JVM running the application. Each option should be specified in a separate 
&lt;option> tag.

_Example:_

```xml
<javaOptions>
    <option>--enable-preview</option>
    <option>-Dfile.encoding=UTF-8</option>
    <options>--add-export</options>
    <option>java.base/sun.security.util=ALL-UNNAMED</option>
</javaOptions>
``` 

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
