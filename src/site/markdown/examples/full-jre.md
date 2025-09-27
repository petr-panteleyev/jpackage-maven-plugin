# Non-Modular Application

This example shows how to build an application image in case not all dependencies represent Java modules. 

Entire JDK is used as runtime image and ```jlink``` is not involved.

```xml
<plugin>
    <groupId>org.panteleyev</groupId>
    <artifactId>jpackage-maven-plugin</artifactId>

    <configuration>
        <name>Application Name</name>
        <appVersion>${project.version}</appVersion>
        <vendor>app.org</vendor>
        <!-- Entire JDK is used as runtime image -->
        <runtimeImage>${java.home}</runtimeImage>
        <module>${appModule}/${appMainClass}</module>
        <modulePaths>
            <modulePath>${project.build.directory}/mods</modulePath>
        </modulePaths>
        <destination>${project.build.directory}/dist</destination>
        <removeDestination>true</removeDestination>
    </configuration>
</plugin>
```
