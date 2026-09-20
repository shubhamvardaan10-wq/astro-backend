package com.astro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Automated PDF Export Service for Desktop & Generated Astrological Reports.
 *
 * Utilizes the embedded Python ReportLab rendering engine to convert Markdown reports
 * into publication-quality multi-page PDF documents with celestial headers, footers,
 * tables, and two-pass page numbering.
 */
@Service
public class PdfExportService {

    private static final Logger log = LoggerFactory.getLogger(PdfExportService.class);

    private final String pythonExecutable;
    private final String scriptPath;

    public PdfExportService(
            @Value("${astro.python.executable:}") String customPython,
            @Value("${astro.python.exporter-script:}") String customScript) {

        this.pythonExecutable = com.astro.util.PythonPathResolver.resolve(customPython);

        Path defaultScript = Paths.get("worker/pdf_exporter.py").toAbsolutePath().normalize();
        if (customScript != null && !customScript.isBlank() && Files.exists(Paths.get(customScript))) {
            this.scriptPath = customScript;
        } else {
            this.scriptPath = defaultScript.toString();
        }

        log.info("Initialized PdfExportService with Python: {} and Script: {}", this.pythonExecutable, this.scriptPath);
    }

    /**
     * Exports a specific Markdown report file to PDF.
     *
     * @param inputMdPath Path to source .md file
     * @param outputPdfPath Optional target .pdf file (if null, replaces .md with .pdf)
     * @return Path to generated PDF
     */
    public String exportFile(String inputMdPath, String outputPdfPath) throws IOException, InterruptedException {
        File inputFile = new File(inputMdPath);
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Input Markdown file does not exist: " + inputMdPath);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(pythonExecutable);
        cmd.add(scriptPath);
        cmd.add(inputMdPath);
        if (outputPdfPath != null && !outputPdfPath.isBlank()) {
            cmd.add(outputPdfPath);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);

        if (!finished || process.exitValue() != 0) {
            log.error("PDF export failed (exit: {}): {}", process.exitValue(), output);
            throw new RuntimeException("PDF export failed: " + output);
        }

        String targetPdf = outputPdfPath != null ? outputPdfPath : inputMdPath.replaceAll("(?i)\\.md$", ".pdf");
        log.info("Successfully exported report to PDF: {}", targetPdf);
        return targetPdf;
    }

    /**
     * Converts raw markdown string content into a PDF file on the Desktop.
     *
     * @param markdownContent Text content of report
     * @param fileName Base file name (e.g. "shubham-marriage-report")
     * @return Path of generated PDF
     */
    public String exportMarkdownContentToDesktop(String markdownContent, String fileName) throws IOException, InterruptedException {
        String cleanName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!cleanName.endsWith(".md")) {
            cleanName += ".md";
        }

        String desktopDir = System.getProperty("user.home") + "/Desktop";
        File tempMd = new File(desktopDir, cleanName);

        try (FileWriter writer = new FileWriter(tempMd)) {
            writer.write(markdownContent);
        }

        return exportFile(tempMd.getAbsolutePath(), null);
    }

    /**
     * Scans Desktop for all astrological report markdown files (*report*.md, *astrology*.md)
     * and automatically converts each to PDF.
     *
     * @return List of generated PDF file paths
     */
    public List<String> scanAndExportDesktopReports() throws IOException, InterruptedException {
        List<String> cmd = List.of(pythonExecutable, scriptPath, "--desktop");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output = new String(process.getInputStream().readAllBytes());
        boolean finished = process.waitFor(90, TimeUnit.SECONDS);

        if (!finished || process.exitValue() != 0) {
            log.error("Batch Desktop PDF export failed: {}", output);
            throw new RuntimeException("Batch Desktop PDF export failed: " + output);
        }

        // Collect existing PDFs on desktop matching reports
        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        List<String> pdfs = new ArrayList<>();
        if (desktop.exists() && desktop.isDirectory()) {
            File[] files = desktop.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".pdf") &&
                    (name.toLowerCase().contains("report") || name.toLowerCase().contains("astrology") || name.toLowerCase().contains("vardaan"))
            );
            if (files != null) {
                for (File f : files) {
                    pdfs.add(f.getAbsolutePath());
                }
            }
        }
        return pdfs;
    }
}
