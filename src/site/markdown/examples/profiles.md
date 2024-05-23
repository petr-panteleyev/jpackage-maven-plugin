## Configuration with profiles

```xml

<project>
    <profiles>
        <profile>
            <id>win</id>
            <activation>
                <os>
                    <family>windows</family>
                </os>
            </activation>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.panteleyev</groupId>
                        <artifactId>jpackage-maven-plugin</artifactId>
                        <version>1.6.3</version>

                        <configuration>
                            <icon>icons/icons.ico</icon>
                            <winMenu>true</winMenu>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
        <profile>
            <id>mac</id>
            <activation>
                <os>
                    <family>mac</family>
                </os>
            </activation>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.panteleyev</groupId>
                        <artifactId>jpackage-maven-plugin</artifactId>
                        <version>1.6.3</version>

                        <configuration>
                            <icon>icons/icons.icns</icon>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>

    <build>
        <plugins>
            <plugin>
                <groupId>org.panteleyev</groupId>
                <artifactId>jpackage-maven-plugin</artifactId>
                <version>1.4.0</version>

                <!-- Common part of configuration -->
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
            </plugin>
        </plugins>
    </build>
</project>
```

With above execution configuration the following command lines can be used regardles of the platform:

```
mvn clean package jpackage:jpackage
```
