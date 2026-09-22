package com.aireview.engine;

import com.aireview.model.IssueModel;
import com.intellij.psi.PsiFile;

import java.util.List;

/**
 * 评审服务：规则引擎 + 大模型 + 融合裁决的总入口。
 * 实现人：盛雅梅；调用方：葛佩娴（执行评审动作）。
 */
public interface ReviewService {

    /**
     * 对单个文件执行完整评审。
     *
     * @param file 待评审文件（Java 文件）
     * @return 评审结果列表，按出现位置排序
     */
    List<IssueModel> review(PsiFile file);
}
