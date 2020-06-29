# JPackage Maven Plugin

Maven plugin for [jpackage](https://openjdk.java.net/jeps/343) tool available in JDK-14.

[![BSD-2 license](https://img.shields.io/badge/License-BSD--2-informational.svg)](LICENSE)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.panteleyev/jpackage-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.panteleyev/jpackage-maven-plugin/)

## Usage

This plugin expects ```maven-toolchains-plugin``` configured in the project. Toolchain "jdk" will be queried for 
tool = "jpackage". If ```jpackage``` cannot be found using toolchain then plugin will try to use 'jpackage' executable
from path specified by ```java.home``` system property.

## Configuration

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

### Generic Parameters

| Parameter | JPackage Argument |
|---|---|
|type|--type &lt;type>|
|name|--name &lt;name>|
|appVersion|--app-version &lt;version>|
|destination|--dest &lt;destination path>|
|copyright|--copyright &lt;copyright string>|
|description|--description &lt;description string>|
|vendor|--vendor &lt;vendor string>|
|runtimeImage|--runtime-image &lt;file path>|
|input|--input &lt;input path>|
|installDir|--install-dir &lt;file path>|
|module|--module &lt;module name>[/&lt;main class>]|
|modulePath|--module-path &lt;module path>...|
|mainClass|--main-class &lt;class name>|
|mainJar|--main-jar &lt;main jar file>|
|icon|--icon &lt;icon file path>|
|verbose|--verbose|
|arguments|--arguments &lt;main class arguments>|

### Windows Specific Parameters

| Parameter | jpackage argument |
|---|---|
|winMenu|--win-menu|
|winDirChooser|--win-dir-chooser|
|winUpgradeUuid|--win-upgrade-uuid &lt;id string>|
|winMenuGroup|--win-menu-group &lt;menu group name>|
|winShortcut|--win-shortcut|
|winPerUserInstall|--win-per-user-install|

### OS X Specific Parameters

| Parameter | jpackage argument |
|---|---|
|macPackageIdentifier|--mac-package-identifier &lt;ID string>|
|macPackageName|--mac-package-name &lt;name string>|
|macPackageSigningPrefix|--mac-package-signing-prefix &lt;prefix string>|
|macSign|--mac-sign|
|macSigningKeychain|--mac-signing-keychain &lt;file path>|
|macSigningKeyUserName|--mac-signing-key-user-name &lt;team name>|

### Linux Specific Parameters

| Parameter | jpackage argument |
|---|---|
|linuxPackageName|--linux-package-name &lt;package name>|
|linuxDebMaintainer|--linux-deb-maintainer &lt;email address>|
|linuxMenuGroup|--linux-menu-group &lt;menu-group-name>|
|linuxRpmLicenseType|--linux-rpm-license-type &lt;type string>|
|linuxAppRelease|--linux-app-release &lt;release value>|
|linuxAppCategory|--linux-app-category &lt;category value>|
|linuxShortcut|--linux-shortcut|

### Image Type

|Plugin Value|JPackage Type|
|---|---|
|APP_IMAGE|app-image|
|DMG|dmg|
|PKG|pkg|
|EXE|exe|
|MSI|msi|
|RPM|rpm|
|DEB|deb|

### Default Command-Line Arguments

Default command line arguments are passed to the main class when the application is started without providing arguments.
Each argument should be specified using &lt;argument> configuration parameter.

_Example:_

```$xml
<arguments>
    <argument>SomeArgument</argument>
    <argument>Argument with spaces</argument>
    <argument>Argument with "quotes"</argument>
</arguments>
```

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
                <winMenu>true</winMenu>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## References

[Packaging Tool User's Guide](https://docs.oracle.com/en/java/javase/14/jpackage/packaging-tool-user-guide.pdf)