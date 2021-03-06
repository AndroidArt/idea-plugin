package codes.ai.ep;

import codes.ai.java.pojo.ResultSnippet;
import codes.ai.snippet.Intention;
import codes.ai.snippet.IntentionExtractor;
import codes.ai.snippet.Label;
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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiComment;
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
        /* After comment, skipping comments in between.
         *  TODO: relax this condition to cater for implicit intentions.
         *  Can potentially only match empty lines.
         *
         */
        PlatformPatterns.psiElement()
            .afterLeafSkipping(
                PlatformPatterns.psiElement(PsiWhiteSpace.class), PlatformPatterns.psiComment())
            .withLanguage(JavaLanguage.INSTANCE),
        new CompletionProvider<CompletionParameters>() {
          public void addCompletions(
              @NotNull CompletionParameters parameters,
              ProcessingContext context,
              @NotNull CompletionResultSet resultSet) {
            Editor editor = parameters.getEditor();
            Project project = editor.getProject();
            Intention intention = IntentionExtractor.getInstance().getIntention(project, editor);
            if (intention == null) return;
            
            boolean isImplicit = (intention.label == Label.IMPLICIT);
            SnippetInsertHandler snippetInsertHandler = isImplicit ?
                SnippetInsertHandler.IMPLICIT_INSTANCE : SnippetInsertHandler.EXPLICIT_INSTANCE;
            // Issue query to API.
            List<ResultSnippet> candidates = new ArrayList<>();
            ApiClient.getInstance().getSnippets(intention.content, candidates);
            int count = 0;
            for (ResultSnippet candidate : candidates) {
              count+=1;
              if (count > 1) return;  // HACK, only use the first candidate.
              candidate.rank = count;
              RowIcon rowIcon = new RowIcon(PlatformIcons.JAVA_OUTSIDE_SOURCE_ICON, AicodesIcons.AICODES);
              resultSet.addElement(
                  LookupElementBuilder.create(candidate, candidate.code)
                      .withIcon(rowIcon)
                      .withInsertHandler(snippetInsertHandler));
            }
            System.out.println("Total number of snippets fetched: ");
            System.out.println(candidates.size());
          }
        });
        
  }
  
  /** Unfortunately invokeAutoPopup won't work here.
   * The triggering character has already been consumed
   * by upper stream processing. It is too late to intercept typedChar.
   * @param position
   * @param typeChar
  
  @Override
  public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
    if (typeChar == '\r' || typeChar == '\n') {
      System.out.println("Enter pressed");
      if (position.getPrevSibling() instanceof PsiComment) {
        return true;
      }
    }
    return false;
  }*/
}
