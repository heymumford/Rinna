/*
 * Service implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.rinna.domain.model.DocumentConfig;
import org.rinna.domain.model.Project;
import org.rinna.domain.model.Release;
import org.rinna.domain.model.WorkItem;
import org.rinna.usecase.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default document service implementation that uses Apache POI and PDFBox.
 * This is a fallback service when Docmosis is not available.
 */
public class DefaultDocumentService implements DocumentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDocumentService.class);

    private final DocumentConfig config;

    /**
     * Creates a new DefaultDocumentService with the given configuration.
     * 
     * @param config the document configuration
     */
    public DefaultDocumentService(DocumentConfig config) {
        this.config = config;
    }

    @Override
    public void generateWorkItemDocument(WorkItem workItem, Format format, TemplateType templateType, OutputStream output) {
        try {
            String content = "# Work Item Details\n\n" +
                "- ID: " + workItem.getId() + "\n" +
                "- Title: " + workItem.getTitle() + "\n" +
                "- Type: " + workItem.getType() + "\n" +
                "- Status: " + workItem.getStatus() + "\n" +
                "- Priority: " + workItem.getPriority() + "\n" +
                "- Assignee: " + (workItem.getAssignee() != null ? workItem.getAssignee() : "Unassigned") + "\n" +
                "- Created: " + workItem.getCreatedAt() + "\n" +
                "- Updated: " + workItem.getUpdatedAt();
            content += "\n\n## Description\n\n" +
                (workItem.getDescription() != null ? workItem.getDescription() : "No description provided");

            generateDocument(content, format, output);
        } catch (Exception e) {
            LOGGER.error("Failed to generate work item document", e);
            throw new IllegalStateException("Document generation failed", e);
        }
    }

    @Override
    public void generateProjectDocument(Project project, Format format, TemplateType templateType, OutputStream output) {
        try {
            String content = "# Project Summary: " + project.getName() + "\n\n" +
                "- ID: " + project.getId() + "\n" +
                "- Key: " + project.getKey() + "\n" +
                "- Status: " + (project.isActive() ? "Active" : "Inactive") + "\n" +
                "- Created: " + project.getCreatedAt() + "\n" +
                "- Updated: " + project.getUpdatedAt() + "\n\n" +
                "## Description\n\n" +
                (project.getDescription() != null ? project.getDescription() : "No description provided");

            generateDocument(content, format, output);
        } catch (Exception e) {
            LOGGER.error("Failed to generate project document", e);
            throw new IllegalStateException("Document generation failed", e);
        }
    }

    @Override
    public void generateReleaseDocument(Release release, Format format, TemplateType templateType, OutputStream output) {
        try {
            String content = "# Release Notes\n\n" +
                "- Release: " + release.getVersion() + "\n" +
                "- Date: " + release.getCreatedAt() + "\n\n" +
                "## Description\n\n" +
                (release.getDescription() != null ? release.getDescription() : "No description provided");

            generateDocument(content, format, output);
        } catch (Exception e) {
            LOGGER.error("Failed to generate release document", e);
            throw new IllegalStateException("Document generation failed", e);
        }
    }

    @Override
    public void generateWorkItemsDocument(List<WorkItem> workItems, Format format, TemplateType templateType, OutputStream output) {
        try {
            StringBuilder content = new StringBuilder("# Work Items Report\n\n");
            content.append("Total items: ").append(workItems.size()).append("\n\n");

            for (WorkItem item : workItems) {
                content.append("## ").append(item.getTitle()).append("\n\n");
                content.append("- ID: ").append(item.getId()).append("\n");
                content.append("- Type: ").append(item.getType()).append("\n");
                content.append("- Status: ").append(item.getStatus()).append("\n");
                content.append("- Priority: ").append(item.getPriority()).append("\n");
                content.append("- Assignee: ").append(item.getAssignee() != null ? item.getAssignee() : "Unassigned").append("\n\n");
            }

            generateDocument(content.toString(), format, output);
        } catch (Exception e) {
            LOGGER.error("Failed to generate work items document", e);
            throw new IllegalStateException("Document generation failed", e);
        }
    }

    @Override
    public void generateCustomDocument(String templatePath, Map<String, Object> data, Format format, OutputStream output) {
        try {
            // Very basic implementation - just dumps the data
            StringBuilder content = new StringBuilder("# Custom Document\n\n");

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                content.append("## ").append(entry.getKey()).append("\n\n");
                content.append(entry.getValue()).append("\n\n");
            }

            generateDocument(content.toString(), format, output);
        } catch (Exception e) {
            LOGGER.error("Failed to generate custom document", e);
            throw new IllegalStateException("Document generation failed", e);
        }
    }

    /**
     * Generates a document in the requested format.
     */
    private void generateDocument(String content, Format format, OutputStream output) throws IOException {
        switch (format) {
            case PDF -> generatePdf(content, output);
            case DOCX -> generateDocx(content, output);
            case HTML -> generateHtml(content, output);
        }
    }

    /**
     * Generates a PDF document.
     */
    private void generatePdf(String content, OutputStream output) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                // Use Standard14Fonts
                PDType1Font font = new PDType1Font(FontName.TIMES_ROMAN);
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(50, 700);

                // Very basic content rendering - just line by line
                String[] lines = content.split("\n");
                float leading = 14;

                for (String line : lines) {
                    // Handle headers with different font sizes
                    if (line.startsWith("# ")) {
                        contentStream.setFont(font, 16);
                        contentStream.showText(line.substring(2));
                    } else if (line.startsWith("## ")) {
                        contentStream.setFont(font, 14);
                        contentStream.showText(line.substring(3));
                    } else {
                        contentStream.setFont(font, 12);
                        contentStream.showText(line);
                    }

                    contentStream.newLineAtOffset(0, -leading);
                }

                contentStream.endText();
            }

            document.save(output);
        }
    }

    /**
     * Generates a DOCX document.
     */
    private void generateDocx(String content, OutputStream output) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            String[] lines = content.split("\n");

            for (String line : lines) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();

                // Handle headers with larger font
                if (line.startsWith("# ")) {
                    run.setBold(true);
                    run.setFontSize(16);
                    run.setText(line.substring(2));
                } else if (line.startsWith("## ")) {
                    run.setBold(true);
                    run.setFontSize(14);
                    run.setText(line.substring(3));
                } else {
                    run.setFontSize(12);
                    run.setText(line);
                }
            }

            document.write(output);
        }
    }

    /**
     * Generates an HTML document.
     */
    private void generateHtml(String content, OutputStream output) throws IOException {
        StringBuilder html = new StringBuilder("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Document</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 40px; }
                        h1 { font-size: 24px; color: #333; }
                        h2 { font-size: 20px; color: #555; }
                        p { font-size: 16px; line-height: 1.5; }
                    </style>
                </head>
                <body>
                """);

        // Very basic Markdown to HTML conversion
        String[] lines = content.split("\n");
        boolean inParagraph = false;

        for (String line : lines) {
            if (line.isBlank()) {
                if (inParagraph) {
                    html.append("</p>\n");
                    inParagraph = false;
                }
                continue;
            }

            if (line.startsWith("# ")) {
                if (inParagraph) {
                    html.append("</p>\n");
                    inParagraph = false;
                }
                html.append("<h1>").append(line.substring(2)).append("</h1>\n");
            } else if (line.startsWith("## ")) {
                if (inParagraph) {
                    html.append("</p>\n");
                    inParagraph = false;
                }
                html.append("<h2>").append(line.substring(3)).append("</h2>\n");
            } else if (line.startsWith("- ")) {
                if (inParagraph) {
                    html.append("</p>\n");
                    inParagraph = false;
                }
                html.append("<p>• ").append(line.substring(2)).append("</p>\n");
            } else {
                if (!inParagraph) {
                    html.append("<p>");
                    inParagraph = true;
                }
                html.append(line).append("<br>\n");
            }
        }

        if (inParagraph) {
            html.append("</p>\n");
        }

        html.append("</body></html>");

        output.write(html.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean isAvailable() {
        return true; // Always available as fallback
    }

    @Override
    public String getServiceName() {
        return "Default Document Service";
    }
}
