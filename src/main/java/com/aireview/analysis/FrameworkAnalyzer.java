package com.aireview.analysis;

import com.aireview.model.IssueModel;
import com.intellij.psi.PsiFile;

import java.util.List;

/**
 * 框架感知分析器：基于 PSI 做框架级缺陷检测（首条规则：@Transactional 事务失效）。
 * 实现人：左晶晶；调用方：盛雅梅（融合裁决）。
 */
public interface FrameworkAnalyzer {

    /**
     * 对单个文件执行框架级分析。
     *
     * @param file 待分析文件（Java 文件）
     * @return 缺陷列表
     */
    List<IssueModel> analyze(PsiFile file);
}
