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

package cn.taketoday.assistant.run;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.execution.filters.Filter;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PsiNavigateUtil;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.lang.Nullable;

class AutowireMethodFieldLinkFilter extends MultipleOccurrencesFilter {
  private static final String AUTOWIRE_FIELD = "autowire field: ";
  private static final String[] DETECTION_MESSAGES = { "autowire method: ", "autowire field: " };

  @Nullable
  protected Filter.Result findNextOccurrence(int startOffset, String line, int entireLength) {
    int index = -1;
    int messageLength = -1;
    boolean localIsField = false;
    int methodSignatureEnd = DETECTION_MESSAGES.length;
    for (int i = 0; i < methodSignatureEnd; ++i) {
      String message = DETECTION_MESSAGES[i];
      index = StringUtil.indexOf(line, message, startOffset);
      if (index != -1) {
        messageLength = message.length();
        localIsField = AUTOWIRE_FIELD.equals(message);
        break;
      }
    }

    if (index == -1) {
      return null;
    }
    else {
      int methodSignatureStart = index + messageLength;
      methodSignatureEnd = StringUtil.indexOf(line, ';', methodSignatureStart);
      if (methodSignatureEnd == -1) {
        return null;
      }
      else {
        String methodSignature = line.substring(methodSignatureStart, methodSignatureEnd);
        int textStartOffset = entireLength - line.length() + methodSignatureStart;
        boolean finalLocalIsField = localIsField;
        return new Filter.Result(textStartOffset, textStartOffset + methodSignature.length(), (project) -> {
          Couple<String> parseResult = parseSignature(methodSignature, finalLocalIsField);
          if (parseResult == null) {
            showResult((editor) -> {
              HintManager.getInstance().showErrorHint(editor, InfraBundle.message("model.method.signature.parse.error.message", methodSignature));
            });
          }
          else {
            String classname = parseResult.getFirst();
            String methodName = parseResult.getSecond();
            showResult((editor) -> {
              PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(classname.replace('$', '.'), GlobalSearchScope.allScope(project));
              if (psiClass == null) {
                HintManager.getInstance().showErrorHint(editor, InfraBundle.message("model.method.resolve.class.error.message", classname));
              }
              else if (finalLocalIsField) {
                PsiField field = psiClass.findFieldByName(methodName, true);
                PsiNavigateUtil.navigate(field);
              }
              else {
                PsiMethod[] methods = psiClass.findMethodsByName(methodName, true);
                if (methods.length == 1) {
                  PsiNavigateUtil.navigate(methods[0]);
                }
                else {
                  JBPopup popup = NavigationUtil.getPsiElementPopup(methods, InfraBundle.message("model.method.choose.method"));
                  popup.showInBestPositionFor(editor);
                }
              }
            });
          }
        });
      }
    }
  }

  @Nullable
  static Couple<String> parseSignature(String methodSignature, boolean isFieldMode) {
    int openParenthIdx = isFieldMode ? methodSignature.length() : methodSignature.lastIndexOf(40);
    int lastSpace = methodSignature.lastIndexOf(32);
    if (openParenthIdx != -1 && lastSpace != -1) {
      if (lastSpace + 1 >= openParenthIdx) {
        return null;
      }
      else {
        String qualifiedMethodName = methodSignature.substring(lastSpace + 1, openParenthIdx);
        int methodNameLastDot = qualifiedMethodName.lastIndexOf(46);
        if (methodNameLastDot == -1) {
          return null;
        }
        else {
          String classname = qualifiedMethodName.substring(0, methodNameLastDot);
          String methodName = isFieldMode ? methodSignature.substring(methodSignature.lastIndexOf(46) + 1)
                                          : methodSignature.substring(methodSignature.indexOf(classname) + classname.length() + 1, openParenthIdx);
          return Couple.of(classname, methodName);
        }
      }
    }
    else {
      return null;
    }
  }
}
