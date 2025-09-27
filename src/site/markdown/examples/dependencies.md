# Application Image Dependencies

This example shows how to gather main jar and all required dependencies for ```jpackage``` using 
```maven-jar-plugin``` and ```maven-dependency-plugin```.

```xml
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <configuration>
            <outputDirectory>${project.build.directory}/mods</outputDirectory>
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
                    <outputDirectory>${project.build.directory}/mods</outputDirectory>
                </configuration>
            </execution>
        </executions>
    </plugin>

    <plugin>
        <groupId>org.panteleyev</groupId>
        <artifactId>jpackage-maven-plugin</artifactId>
        <configuration>
            <modulePaths>
                <modulePath>${project.build.directory}/mods</modulePath>
            </modulePaths>
        </configuration>
    </plugin>
</plugins>
```
