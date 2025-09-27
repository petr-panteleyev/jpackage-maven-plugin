# Configuration with Executions

This example shows how to use different executions to configure OS-specific parameters for ```jpackage```.

```xml
<plugin>
    <groupId>org.panteleyev</groupId>
    <artifactId>jpackage-maven-plugin</artifactId>

    <configuration>
        <name>Application Name</name>
        <appVersion>${project.version}</appVersion>
        <vendor>app.org</vendor>
        <module>${appModule}/${appMainClass}</module>
        <modulePaths>
            <modulePath>${project.build.directory}/mods</modulePath>
        </modulePaths>
        <destination>${project.build.directory}/dist</destination>
        <javaOptions>
            <option>--enable-preview</option>
            <option>-Dfile.encoding=UTF-8</option>
        </javaOptions>
        <removeDestination>true</removeDestination>
        <!-- Windows -->
        <winMenu>true</winMenu>
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
            </configuration>
        </execution>
        <execution>
            <id>linux</id>
            <configuration>
              <type>APP_IMAGE</type>
              <icon>${project.basedir}/icons/icon.png</icon>
            </configuration>
        </execution>
    </executions>
</plugin>
```

With above configuration the following command lines can be used:

* for OS X package: 
    ```
    mvn clean package jpackage:jpackage@mac
    ```

* for Windows package: 
    ```
    mvn clean package jpackage:jpackage@win
    ```

* for Linux package: 
    ```
    mvn clean package jpackage:jpackage@linux
    ```
