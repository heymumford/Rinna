#!/bin/bash

set -e

echo "Running CLI Tests directly with Maven Surefire..."

cd "$(dirname "$0")/.."
PROJECT_ROOT=$(pwd)

cd "$PROJECT_ROOT/rinna-cli"

# Create a temporary pom.xml with tests not skipped
cat > temp-pom.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.rinna</groupId>
        <artifactId>Rinna</artifactId>
        <version>1.6.2</version>
    </parent>

    <artifactId>rinna-cli</artifactId>
    <name>Rinna CLI</name>
    <description>Command-line interface for the Rinna workflow management system</description>

    <properties>
        <picocli.version>4.7.5</picocli.version>
        <rinna.version>\${project.parent.version}</rinna.version>
    </properties>

    <dependencies>
        <!-- JUnit platform suite for tests -->
        <dependency>
            <groupId>org.junit.platform</groupId>
            <artifactId>junit-platform-suite-api</artifactId>
            <version>1.10.2</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.platform</groupId>
            <artifactId>junit-platform-suite-engine</artifactId>
            <version>1.10.2</version>
            <scope>test</scope>
        </dependency>
        <!-- Core module dependency -->
        <dependency>
            <groupId>org.rinna</groupId>
            <artifactId>rinna-core</artifactId>
            <version>\${rinna.version}</version>
        </dependency>

        <!-- Command line parsing -->
        <dependency>
            <groupId>info.picocli</groupId>
            <artifactId>picocli</artifactId>
            <version>\${picocli.version}</version>
        </dependency>

        <!-- For HTTP client -->
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
            <version>4.12.0</version>
        </dependency>

        <!-- For JSON processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.16.0</version>
        </dependency>

        <!-- For configuration -->
        <dependency>
            <groupId>com.typesafe</groupId>
            <artifactId>config</artifactId>
            <version>1.4.3</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
                <configuration>
                    <includes>
                        <include>**/*Test.java</include>
                    </includes>
                    <skipTests>false</skipTests>
                    <skip>false</skip>
                    <properties>
                        <configurationParameters>
                            junit.jupiter.execution.parallel.enabled=true
                            junit.jupiter.execution.parallel.mode.default=same_thread
                            junit.jupiter.execution.parallel.mode.classes.default=concurrent
                        </configurationParameters>
                    </properties>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-checkstyle-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>org.rinna.cli.RinnaCli</mainClass>
                        </manifest>
                    </archive>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
EOF

# Force Maven to run the tests with debugging enabled using the temporary POM
mvn -f temp-pom.xml test 

# Remove the temporary POM
rm temp-pom.xml

echo "CLI Tests execution completed!"