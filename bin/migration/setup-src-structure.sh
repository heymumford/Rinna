#!/bin/bash

# Set the default directory path to the current directory
RINNA_DIR="${RINNA_DIR:-/home/emumford/NativeLinuxProjects/Rinna}"

# Timestamp for logging
timestamp() {
  date
}

echo "Starting domain model copy at $(timestamp)..."

# Clean any existing structure first
echo "Cleaning existing structure..."
rm -rf "${RINNA_DIR}/src/main"
rm -rf "${RINNA_DIR}/src/test"

# Create proper directory structure
echo "Creating directory structure..."
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/domain/model"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/domain/service" 
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/adapter/service"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/adapter/repository"
mkdir -p "${RINNA_DIR}/src/main/resources"
mkdir -p "${RINNA_DIR}/src/test/java/org/rinna"
mkdir -p "${RINNA_DIR}/src/test/resources/features"

# Copy all source files from rinna-core to src
echo "Copying all source files from rinna-core to src..."
rsync -av --include="*.java" --include="*/" --exclude="*" \
    "${RINNA_DIR}/rinna-core/src/main/java/" \
    "${RINNA_DIR}/src/main/java/"

# Copy test files
echo "Copying test files..."
rsync -av --include="*.java" --include="*/" --exclude="*" \
    "${RINNA_DIR}/rinna-core/src/test/java/" \
    "${RINNA_DIR}/src/test/java/"

# Copy resources
echo "Copying resources..."
rsync -av --include="*/" --include="*.properties" --include="*.template" --include="*.feature" --exclude="*" \
    "${RINNA_DIR}/rinna-core/src/main/resources/" \
    "${RINNA_DIR}/src/main/resources/"

# Copy test resources
echo "Copying test resources..."
rsync -av --include="*/" --include="*.properties" --include="*.feature" --exclude="*" \
    "${RINNA_DIR}/rinna-core/src/test/resources/" \
    "${RINNA_DIR}/src/test/resources/"

# Create a proper pom.xml in src directory
cat > "${RINNA_DIR}/src/pom.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.rinna</groupId>
        <artifactId>Rinna</artifactId>
        <version>1.3.0</version>
    </parent>

    <artifactId>rinna-src</artifactId>
    <name>Rinna Source</name>
    <description>Core domain model and services for Rinna workflow management</description>

    <dependencies>
        <!-- SQLite for local storage -->
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>3.49.1.0</version>
        </dependency>
        
        <!-- PDF generation (fallback) -->
        <dependency>
            <groupId>org.apache.pdfbox</groupId>
            <artifactId>pdfbox</artifactId>
            <version>2.0.30</version>
        </dependency>

        <!-- DOCX generation (fallback) -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.4.0</version>
        </dependency>
        
        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.12</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.4.14</version>
        </dependency>
        
        <!-- Testing dependencies -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-params</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>5.17.0</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <version>5.17.0</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>3.25.3</version>
            <scope>test</scope>
        </dependency>
        
        <!-- Cucumber for BDD -->
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-java</artifactId>
            <version>7.16.1</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-junit-platform-engine</artifactId>
            <version>7.16.1</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-spring</artifactId>
            <version>7.16.1</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
            </resource>
        </resources>
        <testResources>
            <testResource>
                <directory>src/test/resources</directory>
                <filtering>false</filtering>
                <includes>
                    <include>**/*.feature</include>
                    <include>**/*.properties</include>
                </includes>
            </testResource>
        </testResources>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.12.1</version>
                <configuration>
                    <source>\${java.version}</source>
                    <target>\${java.version}</target>
                    <release>\${java.version}</release>
                    <compilerArgs>
                        <arg>-Xlint:all</arg>
                        <arg>--enable-preview</arg>
                    </compilerArgs>
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
                    <excludes>
                        <exclude>**/bdd/**</exclude>
                    </excludes>
                    <argLine>--enable-preview</argLine>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter-engine</artifactId>
                        <version>5.10.2</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
EOF

echo "Creating Main.java in src directory..."
cat > "${RINNA_DIR}/src/main/java/org/rinna/Main.java" << EOF
package org.rinna;

/**
 * Main application entry point for Rinna.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Rinna Workflow Management");
        Rinna rinna = new Rinna();
        rinna.initialize();
    }
}
EOF

echo "Domain model copy completed at $(timestamp)"
echo "Try running tests with: cd ${RINNA_DIR}/src && mvn test"