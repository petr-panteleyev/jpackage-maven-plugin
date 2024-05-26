## Configuration with executions

```xml
<plugin>
    <groupId>org.panteleyev</groupId>
    <artifactId>jpackage-maven-plugin</artifactId>
    <version>1.6.4</version>

    <configuration>
        <name>Application Name</name>
        <appVersion>${project.version}</appVersion>
        <vendor>app.org</vendor>
        <module>${appModule}/${appMainClass}</module>
        <modulePaths>
            <modulePath>target/mods</modulePath>
        </modulePaths>
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
        <execution>
            <id>linux</id>
            <configuration>
              <type>APP_IMAGE</type>
              <icon>icons/icon.png</icon>
            </configuration>
        </execution>
    </executions>
</plugin>
```

With above execution configuration the following command lines can be used:

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
