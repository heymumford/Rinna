/**
 * Unit tests for ReportFormat
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.unit.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.rinna.base.UnitTest;
import org.rinna.cli.report.ReportFormat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReportFormat enum.
 */
public class ReportFormatTest extends UnitTest {

    @Test
    void testFileExtensions() {
        assertEquals("txt", ReportFormat.TEXT.getFileExtension(), "TEXT format should have 'txt' extension");
        assertEquals("csv", ReportFormat.CSV.getFileExtension(), "CSV format should have 'csv' extension");
        assertEquals("json", ReportFormat.JSON.getFileExtension(), "JSON format should have 'json' extension");
        assertEquals("md", ReportFormat.MARKDOWN.getFileExtension(), "MARKDOWN format should have 'md' extension");
        assertEquals("html", ReportFormat.HTML.getFileExtension(), "HTML format should have 'html' extension");
        assertEquals("xml", ReportFormat.XML.getFileExtension(), "XML format should have 'xml' extension");
    }
    
    @Test
    void testGetExtensionMethod() {
        // Test that getExtension() returns the same as getFileExtension()
        for (ReportFormat format : ReportFormat.values()) {
            assertEquals(format.getFileExtension(), format.getExtension(), 
                    "getExtension() should return the same as getFileExtension() for " + format);
        }
    }
    
    @ParameterizedTest
    @CsvSource({
        "TEXT, txt",
        "CSV, csv",
        "JSON, json",
        "MARKDOWN, md",
        "HTML, html",
        "XML, xml"
    })
    void testFileExtensionsParameterized(ReportFormat format, String expectedExtension) {
        assertEquals(expectedExtension, format.getFileExtension(), 
                format + " format should have '" + expectedExtension + "' extension");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"TEXT", "text", "Text", "TXT", "txt"})
    void testFromStringValidTextFormats(String formatString) {
        assertEquals(ReportFormat.TEXT, ReportFormat.fromString(formatString),
                "'" + formatString + "' should be parsed as TEXT format");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"CSV", "csv", "Csv"})
    void testFromStringValidCsvFormats(String formatString) {
        assertEquals(ReportFormat.CSV, ReportFormat.fromString(formatString),
                "'" + formatString + "' should be parsed as CSV format");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"JSON", "json", "Json"})
    void testFromStringValidJsonFormats(String formatString) {
        assertEquals(ReportFormat.JSON, ReportFormat.fromString(formatString),
                "'" + formatString + "' should be parsed as JSON format");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"MARKDOWN", "markdown", "Markdown", "MD", "md"})
    void testFromStringValidMarkdownFormats(String formatString) {
        assertEquals(ReportFormat.MARKDOWN, ReportFormat.fromString(formatString),
                "'" + formatString + "' should be parsed as MARKDOWN format");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"HTML", "html", "Html"})
    void testFromStringValidHtmlFormats(String formatString) {
        assertEquals(ReportFormat.HTML, ReportFormat.fromString(formatString),
                "'" + formatString + "' should be parsed as HTML format");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"XML", "xml", "Xml"})
    void testFromStringValidXmlFormats(String formatString) {
        assertEquals(ReportFormat.XML, ReportFormat.fromString(formatString),
                "'" + formatString + "' should be parsed as XML format");
    }
    
    @Test
    void testFromStringWithNull() {
        assertEquals(ReportFormat.TEXT, ReportFormat.fromString(null),
                "null should be parsed as TEXT format (default)");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalid", "unknown", "doc", "pdf", ""})
    void testFromStringWithInvalidFormat(String invalidFormat) {
        assertEquals(ReportFormat.TEXT, ReportFormat.fromString(invalidFormat),
                "Invalid format '" + invalidFormat + "' should be parsed as TEXT format (default)");
    }
    
    @Test
    void testSpecificAliases() {
        // Test specific alias for Markdown
        assertEquals(ReportFormat.MARKDOWN, ReportFormat.fromString("md"),
                "'md' should be parsed as MARKDOWN format");
    }
}