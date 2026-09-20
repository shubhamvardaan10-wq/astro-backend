package com.astro.model;

public class PdfExportRequest {

    private String filePath;
    private String fileName;
    private String markdownContent;
    private boolean scanDesktop;

    public String getFilePath()               { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileName()               { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getMarkdownContent()                  { return markdownContent; }
    public void setMarkdownContent(String markdownContent) { this.markdownContent = markdownContent; }

    public boolean isScanDesktop()                 { return scanDesktop; }
    public void setScanDesktop(boolean scanDesktop) { this.scanDesktop = scanDesktop; }
}
