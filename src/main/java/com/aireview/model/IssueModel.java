package com.aireview.model;

/**
 * 统一缺陷模型：三个模块之间只传这个对象。
 * 字段为三人共同约定的契约，冻结后不得私自修改，如需变更先在群里同步。
 */
public class IssueModel {

    /** 唯一标识，建议 UUID 字符串。 */
    private String id;

    /** 来源：规则引擎或大模型。 */
    private Source source;

    /** 规则 ID（RULE 来源时有值，LLM 来源可为空）。 */
    private String ruleId;

    /** 缺陷级别。 */
    private Severity severity;

    /** 文件路径。 */
    private String filePath;

    /** 起始行（1-based）。 */
    private int startLine;

    /** 结束行。 */
    private int endLine;

    /** 问题描述。 */
    private String message;

    /** 证据链：为什么判定这里是问题。 */
    private String evidence;

    /** 是否可自动修复。 */
    private boolean fixable;

    /** 修复补丁（可为空）。 */
    private String fixPatch;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public boolean isFixable() {
        return fixable;
    }

    public void setFixable(boolean fixable) {
        this.fixable = fixable;
    }

    public String getFixPatch() {
        return fixPatch;
    }

    public void setFixPatch(String fixPatch) {
        this.fixPatch = fixPatch;
    }
}
