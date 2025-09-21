# Application Image with Full JRE

```xml
<plugin>
    <groupId>org.panteleyev</groupId>
    <artifactId>jpackage-maven-plugin</artifactId>

    <configuration>
        <name>Application Name</name>
        <appVersion>${project.version}</appVersion>
        <vendor>app.org</vendor>
        <runtimeImage>${java.home}</runtimeImage>
        <module>${appModule}/${appMainClass}</module>
        <modulePaths>
            <modulePath>${project.basedir}/target/jmods</modulePath>
        </modulePaths>
        <destination>${project.basedir}/target/dist</destination>
        <javaOptions>
            <option>--enable-preview</option>
            <option>-Dfile.encoding=UTF-8</option>
        </javaOptions>
    </configuration>

    <executions>
        <execution>
            <id>mac</id>
            <configuration>
                <icon>${project.basedir}/icons/icons.icns</icon>
            </configuration>
        </execution>
        <execution>
            <id>win</id>
            <configuration>
                <icon>${project.basedir}/icons/icon.ico</icon>
                <winMenu>true</winMenu>
            </configuration>
        </execution>
    </executions>
</plugin>
```
