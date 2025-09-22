# Collecting JAR Files

This example shows how to collect all required JAR files using Maven Dependency Plugin.

All dependencies are placed into the same directory ```${project.basedir}/target/mods``` together with the main 
application jar.

```xml

<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <configuration>
            <outputDirectory>${project.basedir}/target/mods</outputDirectory>
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
                    <outputDirectory>${project.basedir}/target/mods</outputDirectory>
                </configuration>
            </execution>
        </executions>
    </plugin>

    <plugin>
        <groupId>org.panteleyev</groupId>
        <artifactId>jpackage-maven-plugin</artifactId>
        <configuration>
            <modulePaths>
                <modulePath>${project.basedir}/target/mods</modulePath>
            </modulePaths>
        </configuration>
    </plugin>
</plugins>
```
