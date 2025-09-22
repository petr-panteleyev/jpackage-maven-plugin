# Using JLink Output

This example shows how to use [jlink-maven-plugin](https://github.com/petr-panteleyev/jlink-maven-plugin) output.

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.panteleyev</groupId>
            <artifactId>jlink-maven-plugin</artifactId>

            <configuration>
                <output>${project.build.directory}/jlink</output>
                <modulePaths>
                    <modulePath>${project.build.directory}/mods</modulePath>
                </modulePaths>
                <addModules>
                    <addModule>${appModule}</addModule>
                </addModules>
                <noHeaderFiles>true</noHeaderFiles>
                <noManPages>true</noManPages>
                <stripDebug>true</stripDebug>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.panteleyev</groupId>
            <artifactId>jpackage-maven-plugin</artifactId>

            <configuration>
                <name>Project Name</name>
                <appVersion>${project.version}</appVersion>
                <vendor>vendor.org</vendor>
                <module>${appModule}/${appMainClass}</module>
                <runtimeImage>${project.build.directory}/jlink</runtimeImage>
                <destination>${project.build.directory}/dist</destination>
                <removeDestination>true</removeDestination>
            </configuration>
        </plugin>
    </plugins>
</build>
```