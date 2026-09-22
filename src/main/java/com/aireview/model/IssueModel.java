package com.aireview.model;

public class IssueModel {
    private String id;
    private Source source;
    private String ruleId;
    private Severity severity;
    private String filePath;
    private int startLine;
    private int endLine;
    private String message;
    private String evidence;
    private boolean fixable;
    private String fixPatch;

    public IssueModel() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public int getStartLine() { return startLine; }
    public void setStartLine(int startLine) { this.startLine = startLine; }

    public int getEndLine() { return endLine; }
    public void setEndLine(int endLine) { this.endLine = endLine; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public boolean isFixable() { return fixable; }
    public void setFixable(boolean fixable) { this.fixable = fixable; }

    public String getFixPatch() { return fixPatch; }
    public void setFixPatch(String fixPatch) { this.fixPatch = fixPatch; }
}
