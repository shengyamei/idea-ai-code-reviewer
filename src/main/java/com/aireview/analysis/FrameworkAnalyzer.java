package com.aireview.analysis;

import com.aireview.model.IssueModel;
import com.intellij.psi.PsiFile;

import java.util.List;

/**
 * 框架感知分析器：基于 PSI 做框架级缺陷检测（首条规则：@Transactional 事务失效）。
 * 由分析层实现，供评审服务在融合裁决阶段调用。
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
