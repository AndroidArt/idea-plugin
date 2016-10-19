package codes.ai.ep;

import codes.ai.java.pojo.ResultSnippet;
import codes.ai.snippet.SnippetInsertHandler;
import codes.ai.localapi.ApiClient;
import codes.ai.ui.AicodesIcons;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.RowIcon;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** @author xuy. Copyright (c) Ai.codes */
public class AiSnippetContributor extends CompletionContributor  {
  public AiSnippetContributor() {
    extend(
        CompletionType.BASIC,
        /* After comment, skipping comments in between */
        PlatformPatterns.psiElement()
            .afterLeafSkipping(
                PlatformPatterns.psiElement(PsiWhiteSpace.class), PlatformPatterns.psiComment())
            .withLanguage(JavaLanguage.INSTANCE),
        new CompletionProvider<CompletionParameters>() {
          public void addCompletions(
              @NotNull CompletionParameters parameters,
              ProcessingContext context,
              @NotNull CompletionResultSet resultSet) {
            // Get the async line, which is a comment starting with three slashes.
            PsiElement comment =
                PsiTreeUtil.skipSiblingsBackward(
                    parameters.getOriginalPosition(), PsiWhiteSpace.class);
            if (comment == null || !comment.getText().startsWith("///")) {
              return;
            }
            String intention = comment.getText().substring(3).trim();
            // Issue query to API.
            List<ResultSnippet> candidates = new ArrayList<>();
            ApiClient.getInstance().getSnippets(intention, candidates);
            int count = 0;
            for (ResultSnippet candidate : candidates) {
              count+=1;
              if (count > 1) return;  // HACK, only use the first candidate.
              candidate.rank = count;
              RowIcon rowIcon = new RowIcon(PlatformIcons.JAVA_OUTSIDE_SOURCE_ICON, AicodesIcons.AICODES);
              resultSet.addElement(
                  LookupElementBuilder.create(candidate, candidate.code)
                      .withIcon(rowIcon)
                      .withInsertHandler(SnippetInsertHandler.INSTANCE));
            }
            System.out.println("Total number of snippets fetched: ");
            System.out.println(candidates.size());
          }
        });
  }
}