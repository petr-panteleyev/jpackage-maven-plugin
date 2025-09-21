# JPackage Maven Plugin

Maven plugin for [jpackage](https://openjdk.org/jeps/392).

```xml

<plugin>
    <groupId>org.panteleyev</groupId>
    <artifactId>jpackage-maven-plugin</artifactId>
    <version>2.0.0</version>
</plugin>
```

## Compatibility

| JPackage Plugin | Maven Plugin API | JDK |
|-----------------|------------------|-----|
| 2.0.0           | 4.0.0            | 17  |
| 1.6.6           | 3.9.7            | 8   |

## Finding jpackage

Plugin searches for ```jpackage``` executable using the following priority list:

1. ```maven-toolchains-plugin``` configured in the project. Toolchain "jdk" will be queried for
   tool = "jpackage".

2. ```java.home``` system property.

## Assembling Dependencies

Before executing ```jpackage``` all runtime dependencies should be copied into a single folder together with main
application jar.

[This example](./docs/examples/dependency-plugin.md) shows how to do this via
```maven-dependency-plugin```.

## Configuration

There are generic parameters as well as OS-specific parameters for OS X and Windows.
Plugin determines OS name using ```os.name``` system property in order to configure OS-specific parameters.

Generic parameters should be placed in the root plugin configuration. OS-specific parameters should be separated with
[executions](./docs/examples/executions.md) or [profiles](./docs/examples/profiles.md).

### Parameters

| Parameter                             | Type                     | JPackage Argument                                               | Min Version |
|---------------------------------------|--------------------------|-----------------------------------------------------------------|-------------|
| **Generic**                           |
| aboutUrl                              | String                   | --about-url &lt;url>                                            | 17          |
| additionalOptions                     | List&lt;String>          | See [Additional JPackage Options](#additional-jpackage-options) | -           |
| addModules                            | List&lt;String>          | --add-modules &lt;module>[,&lt;module>]                         | 14          |
| [appContentPaths](./docs/lists.md)    | List&lt;Path>            | --app-content additional-content[,additional-content...]        | 18          |
| appImage                              | Path                     | --app-image &lt;name>                                           | 14          |
| appVersion                            | String                   | --app-version &lt;version>                                      | 14          |
| arguments                             | List&lt;String>          | --arguments &lt;main class arguments>                           | 14          |
| copyright                             | String                   | --copyright &lt;copyright string>                               | 14          |
| description                           | String                   | --description &lt;description string>                           | 14          |
| destination                           | Path                     | --dest &lt;destination path>                                    | 14          |
| fileAssociations                      | List&lt;Path>            | --file-associations &lt;file association property file>         | 14          |
| icon                                  | Path                     | --icon &lt;icon file path>                                      | 14          |
| input                                 | Path                     | --input &lt;input path>                                         | 14          |
| installDir                            | String                   | --install-dir &lt;file path>                                    | 14          |
| javaOptions                           | List&lt;String>          | --java-options &lt;JVM options>                                 | 14          |
| jLinkOptions                          | List&lt;String>          | --jlink-options &lt;options>                                    | 16          |
| launchers                             | List&lt;Launcher>        | --add-launcher &lt;name>=&lt;property file>                     | 14          |
| launcherAsService                     | Boolean                  | --launcher-as-service                                           | 19          |
| licenseFile                           | Path                     | --license-file &lt;license file path>                           | 14          |
| mainClass                             | String                   | --main-class &lt;class name>                                    | 14          |
| mainJar                               | String                   | --main-jar &lt;main jar file>                                   | 14          |
| module                                | String                   | --module &lt;module name>[/&lt;main class>]                     | 14          |
| modulePaths                           | List&lt;Path>            | --module-path &lt;module path>                                  | 14          |
| name                                  | String                   | --name &lt;name>                                                | 14          |
| removeDestination                     | Boolean                  | See [Destination Directory](#destination-directory)             | -           |
| resourceDir                           | Path                     | --resource-dir &lt;resource dir path>                           | 14          |
| runtimeImage                          | Path                     | --runtime-image &lt;dir path>                                   | 14          |
| skip                                  | Boolean                  | Skips plugin execution                                          | -           |
| temp                                  | Path                     | --temp &lt;temp dir path>                                       | 14          |
| type                                  | [ImageType](#image-type) | --type &lt;type>                                                | 14          |
| vendor                                | String                   | --vendor &lt;vendor string>                                     | 14          |
| verbose                               | Boolean                  | --verbose                                                       | 14          |
| **Windows**                           |
| winConsole                            | Boolean                  | --win-console                                                   | 14          |
| winDirChooser                         | Boolean                  | --win-dir-chooser                                               | 14          |
| winHelpUrl                            | String                   | --win-help-url &lt;url>                                         | 17          |
| winMenu                               | Boolean                  | --win-menu                                                      | 14          |
| winMenuGroup                          | String                   | --win-menu-group &lt;menu group name>                           | 14          |
| winPerUserInstall                     | Boolean                  | --win-per-user-install                                          | 14          |
| winShortcut                           | Boolean                  | --win-shortcut                                                  | 14          |
| winShortcutPrompt                     | Boolean                  | --win-shortcut-prompt                                           | 17          |
| winUpdateUrl                          | String                   | --win-update-url &lt;url>                                       | 17          |
| winUpgradeUuid                        | String                   | --win-upgrade-uuid &lt;id string>                               | 14          |
| **OS X**                              |
| macAppCategory                        | String                   | --mac-app-category &lt;category string>                         | 17          |
| macAppStore                           | Boolean                  | --mac-app-store                                                 | 17          |
| [macDmgContentPaths](./docs/lists.md) | List<Path>               | --mac-dmg-content additional-content[,additional-content...]    | 18          |
| macEntitlements                       | Path                     | --mac-entitlements &lt;file path>                               | 17          |
| macPackageIdentifier                  | String                   | --mac-package-identifier &lt;ID string>                         | 14          |
| macPackageName                        | String                   | --mac-package-name &lt;name string>                             | 14          |
| macPackageSigningPrefix               | String                   | --mac-package-signing-prefix &lt;prefix string>                 | 17          |
| macSign                               | Boolean                  | --mac-sign                                                      | 14          |
| macSigningKeychain                    | String                   | --mac-signing-keychain &lt;keychain name>                       | 14          |
| macSigningKeyUserName                 | String                   | --mac-signing-key-user-name &lt;key user or team name>          | 14          |
| **Linux**                             |
| linuxAppCategory                      | String                   | --linux-app-category &lt;category value>                        | 14          |
| linuxAppRelease                       | String                   | --linux-app-release &lt;release value>                          | 14          |
| linuxDebMaintainer                    | String                   | --linux-deb-maintainer &lt;email address>                       | 14          |
| linuxMenuGroup                        | String                   | --linux-menu-group &lt;menu-group-name>                         | 14          |
| linuxPackageName                      | String                   | --linux-package-name &lt;package name>                          | 14          |
| linuxPackageDeps                      | Boolean                  | --linux-package-deps                                            | 14          |
| linuxRpmLicenseType                   | String                   | --linux-rpm-license-type &lt;type string>                       | 14          |
| linuxShortcut                         | Boolean                  | --linux-shortcut                                                | 14          |

Since version ```2.0.0``` the plugin does not check if parameter is applicable to ```jpackage``` tool version.
Users are advised to consult the corresponding User's Guide.

Plugin does not provide configuration options for ```jpackage``` parameters which are not valid for JDK 17+.
Use ```additionalOptions``` for earlier versions.

### Mandatory Parameters

To enable various configuration approaches mandatory parameters are validated during plugin execution:

* name
* destination

### Image Type

| Plugin Value | JPackage Type                   |
|--------------|---------------------------------|
| DEFAULT      | Default image type, OS specific |
| APP_IMAGE    | app-image                       |
| DMG          | dmg                             |
| PKG          | pkg                             |
| EXE          | exe                             |
| MSI          | msi                             |
| RPM          | rpm                             |
| DEB          | deb                             |

### Relative Path Resolution

Parameters of type ```Path``` are resolved to absolute paths. To avoid unexpected results it is advised to supply 
absolute paths explicitly using Maven variables such as ```${project.basedir}```.

```xml
<configuration>
    <destination>${project.basedir}/target/dist</destination>
</configuration>
```

### Java Options

&lt;javaOptions> defines options for JVM running the application. Each option should be specified in a separate
&lt;option> tag.

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
boolean option ```removeDestination```. If ```true``` plugin will try to delete directory specified by
```destination```.
This might be useful to relaunch ```jpackage``` task without rebuilding an entire project.

For safety reasons plugin will not process ```removeDestination``` if ```destination``` points to a location outside of
```${project.build.directory}```.

### Default Command-Line Arguments

Default command line arguments are passed to the main class when the application is started without providing arguments.
Each argument should be specified using &lt;argument> configuration parameter.

```xml
<arguments>
    <argument>SomeArgument</argument>
    <argument>Argument with spaces</argument>
    <argument>Argument with "quotes"</argument>
</arguments>
```

### Additional Launchers

Additional launchers provide the opportunity to install alternative ways to start an application.

```xml
<launchers>
    <launcher>
        <name>App1</name>
        <file>${project.basedir}/src/resources/App1.properties</file>
    </launcher>
    <launcher>
        <name>App2</name>
        <file>${project.basedir}/src/resources/App2.properties</file>
    </launcher>
</launchers>
```

### File Associations

If you want your application to be started when a user opens a specific type of file, use ```<fileAssociations>```
configuration.

```xml
<fileAssociations>
    <fileAssociation>${project.basedir}/src/properties/java.properties</fileAssociation>
    <fileAssociation>${project.basedir}/src/properties/cpp.properties</fileAssociation>
</fileAssociations>
```

Note: apparently this option does not work for modular applications.

### jlink Options

Options that are passed to underlying jlink call.

```xml
<jLinkOptions>
    <jLinkOption>--strip-native-commands</jLinkOption>
    <jLinkOption>--strip-debug</jLinkOption>
</jLinkOptions>
```

### Module Paths

Each module path is specified by a separate &lt;modulePath> parameter:

```xml
<modulePaths>
    <modulePath>${project.basedir}/target/jmods</modulePath>
</modulePaths>
```

### Additional JPackage Options

Additional options allow passing jpackage command line options not supported by the plugin. These options are passed as
is without any transformation.

```xml
<additionalOptions>
    <option>--jlink-options</option>
    <option>--bind-services</option>
</additionalOptions>
```

## Dry Run Mode

To print jpackage parameters without executing jpackage set ```jpackage.dryRun``` property to ```true```.

```
mvn clean package jpackage:jpackage@win -Djpackage.dryRun=true
```

## Examples

* [Image with full JRE](./docs/examples/full-jre.md)
* [Configuration via executions](./docs/examples/executions.md)
* [Configuration via profiles](./docs/examples/profiles.md)
* [Using Maven Dependency Plugin](./docs/examples/dependency-plugin.md)

## Packaging Tool User's Guide

* [Release 25](https://docs.oracle.com/en/java/javase/25/jpackage/packaging-tool-user-guide.pdf)
* [Release 21](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-tool-user-guide.pdf)
* [Release 17](https://docs.oracle.com/en/java/javase/17/jpackage/packaging-tool-user-guide.pdf)
