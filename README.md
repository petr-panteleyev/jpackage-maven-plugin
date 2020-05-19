# JPackage Maven Plugin

Maven plugin for [jpackage](https://openjdk.java.net/jeps/343) tool available in JDK-14.

[![BSD-2 license](https://img.shields.io/badge/License-BSD--2-informational.svg)](LICENSE)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.panteleyev/jpackage-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.panteleyev/jpackage-maven-plugin/)

## Usage

```xml
<plugin>
    <groupId>org.panteleyev</groupId>
    <artifactId>jpackage-maven-plugin</artifactId>
    <version>0.0.1</version>
</plugin>
```

This plugin requires ```maven-toolchains-plugin``` configured in the project. Toolchain "jdk" will be queried for 
tool = "jpackage". If ```jpackage``` cannot be found for any reason the build fails.

Plugin determines OS name using ```${os.name}``` property. This is used to configure specific parameters.

## Configuration

There are generic parameters as well as OS-specific parameters for OS X, Windows, Linux.

Generic parameters should be placed in the root plugin configuration. OS-specific parameters should be separated via
executions. Id of the executions are irrelevant however using OS names improve usability.

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

### Generic Parameters

| Parameter | Type | jpackage argument |
|---|---|---|
|type|[Enum](#markdown-header-image-type)|--type &lt;type>|
|name|String|--name &lt;name>|
|appVersion|String|--app-version &lt;version>|
|destination|String|--dest &lt;destination path>|
|copyright|String|--copyright &lt;copyright string>|
|description|String|--description &lt;description string>|
|vendor|String|--vendor &lt;vendor string>|
|runtimeImage|String|--runtime-image &lt;file path>|
|input|String|--input &lt;input path>|
|module|String|--module &lt;module name>[/&lt;main class>]|
|modulePath|String|--module-path &lt;module path>...|
|mainClass|String|--main-class &lt;class name>|
|mainJar|String|--main-jar &lt;main jar file>|
|icon|String|--icon &lt;icon file path>|

### Windows Specific Parameters

| Parameter | jpackage argument |
|---|---|
|winMenu|--win-menu|
|winDirChooser|--win-dir-chooser|
|winUpgradeUuid|--win-upgrade-uuid &lt;id string>|
|winMenuGroup|--win-menu-group &lt;menu group name>|
|winShortcut|--win-shortcut|
|winPerUserInstall|--win-per-user-install|

### Image Type

| Plugin value | jpackage type |
|---|---|
|APP_IMAGE|app-image|
|DMG|dmg|
|PKG|pkg|
|EXE|exe|
|MSI|msi|

## Samples

### Application image with full JRE

```xml
<plugin>
    <groupId>org.panteleyev</groupId>
    <artifactId>jpackage-maven-plugin</artifactId>
    <version>0.0.1</version>

    <configuration>
        <name>Application Name</name>
        <appVersion>${project.version}</appVersion>
        <vendor>app.org</vendor>
        <runtimeImage>${java.home}</runtimeImage>
        <module>${appModule}/${appMainClass}</module>
        <modulePath>target/mods</modulePath>
        <destination>target/dist</destination>
        <javaOptions>
            <option>--enable-preview</option>
            <option>-Dfile.encoding=UTF-8</option>
        </javaOptions>
    </configuration>

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
            </configuration>
        </execution>
    </executions>
</plugin>
```
