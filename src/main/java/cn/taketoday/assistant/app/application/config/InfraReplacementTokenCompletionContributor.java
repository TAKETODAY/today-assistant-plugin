/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.assistant.app.application.config;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupActionProvider;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementAction;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInspection.ex.EditInspectionToolsSettingsAction;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.microservices.jvm.config.ConfigPlaceholderReference;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.ElementPattern;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.psi.PsiElement;
import com.intellij.util.Consumer;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ProcessingContext;

import java.util.List;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.lang.Nullable;

/**
 * Adds completion for configured replacement tokens and placeholder.
 * Additional registration via {@link LookupActionProvider} adds intention to open highlighting inspection's settings to configure them.
 */
public abstract class InfraReplacementTokenCompletionContributor extends CompletionContributor implements LookupActionProvider {

  private final Key<Couple<String>> myTokenKey;

  @Nullable
  private final String myInspectionId;

  private final TokenInsertHandler myTokenInsertHandler = new TokenInsertHandler();

  /**
   * @param place Invocation place.
   * @return List valid {@code prefix|suffix} tokens.
   * @see InfraReplacementTokenStorage
   */

  protected abstract List<Couple<String>> getReplacementTokens(PsiElement place);

  /**
   * CTOR.
   *
   * @param place Value pattern.
   * @param tokenKey Unique key for storing data in lookup elements.
   * @param inspectionId (Optional) ID of inspection to open settings for configuring replacement tokens.
   */
  protected InfraReplacementTokenCompletionContributor(ElementPattern<? extends PsiElement> place,
          Key<Couple<String>> tokenKey,
          @Nullable String inspectionId) {
    myTokenKey = tokenKey;
    myInspectionId = inspectionId;

    extend(CompletionType.BASIC, place, new CompletionProvider<>() {
      @Override
      protected void addCompletions(CompletionParameters parameters,
              ProcessingContext context,
              CompletionResultSet result) {
        if (!parameters.isExtendedCompletion()) {
          final String shortcut = KeymapUtil.getFirstKeyboardShortcutText(IdeActions.ACTION_CODE_COMPLETION);
          result.addLookupAdvertisement(InfraAppBundle.message(
                  "InfraReplacementTokenCompletionContributor.press.again.to.show.replacement.tokens", shortcut));
          return;
        }

        List<Couple<String>> replacementTokens = getReplacementTokens(parameters.getPosition());
        String text = parameters.getPosition().getText();
        for (Couple<String> token : replacementTokens) {
          if (StringUtil.contains(text, token.first)) {
            return;
          }
        }

        CompletionResultSet noPrefixResult = result.withPrefixMatcher("");
        for (Couple<String> token : replacementTokens) {
          addCompletionVariant(noPrefixResult, token, "Replacement token");
        }

        addCompletionVariant(noPrefixResult,
                Couple.of(ConfigPlaceholderReference.PLACEHOLDER_PREFIX, ConfigPlaceholderReference.PLACEHOLDER_SUFFIX),
                "Placeholder");
      }

      private void addCompletionVariant(CompletionResultSet completionResultSet,
              Couple<String> token,
              String typeText) {
        LookupElementBuilder lookupElement = LookupElementBuilder.create(token.first + token.second)
                .withPresentableText(token.first + "..." + token.second)
                .withBoldness(true)
                .withIcon(Icons.Today)
                .withTypeText(typeText, true)
                .withInsertHandler(myTokenInsertHandler);
        lookupElement.putUserData(myTokenKey, token);
        completionResultSet.addElement(PrioritizedLookupElement.withPriority(lookupElement, -50));
      }
    });
  }

  @Override
  public void fillActions(LookupElement element, Lookup lookup, Consumer<LookupElementAction> consumer) {
    if (myInspectionId != null && myTokenKey.isIn(element)) {
      consumer.consume(new LookupElementAction(PlatformIcons.EDIT, InfraAppBundle.message(
              "InfraReplacementTokenCompletionContributor.configure.replacement.tokens")) {
        @Override
        public Result performLookupAction() {
          Project project = lookup.getProject();

          ApplicationManager.getApplication().invokeLater(() -> {
            InspectionProfileImpl profile = InspectionProfileManager.getInstance(project).getCurrentProfile();
            EditInspectionToolsSettingsAction.editToolSettings(project, profile, myInspectionId);
          }, project.getDisposed());

          return Result.HIDE_LOOKUP;
        }
      });
    }
  }

  private class TokenInsertHandler implements InsertHandler<LookupElement> {

    @Override
    public void handleInsert(InsertionContext context, LookupElement item) {
      Couple<String> token = item.getUserData(myTokenKey);
      assert token != null;

      context.getEditor().getCaretModel().moveCaretRelatively(-token.second.length(), 0, false, false, false);

      AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
    }
  }
}
