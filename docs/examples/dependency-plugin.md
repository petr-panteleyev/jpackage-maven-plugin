# Using Maven Dependency Plugin

```xml

<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <configuration>
            <outputDirectory>${project.basedir}/target/jmods</outputDirectory>
        </configuration>
    </plugin>

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
            <execution>
                <id>copy-dependencies</id>
                <phase>package</phase>
                <goals>
                    <goal>copy-dependencies</goal>
                </goals>
                <configuration>
                    <includeScope>runtime</includeScope>
                    <outputDirectory>${project.basedir}/target/jmods</outputDirectory>
                </configuration>
            </execution>
        </executions>
    </plugin>

    <plugin>
        <groupId>org.panteleyev</groupId>
        <artifactId>jpackage-maven-plugin</artifactId>
        <configuration>
            <modulePaths>
                <modulePath>${project.basedir}/target/jmods</modulePath>
            </modulePaths>
        </configuration>
    </plugin>
</plugins>
```
