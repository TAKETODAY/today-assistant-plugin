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

package cn.taketoday.assistant.spel;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.spring.el.contextProviders.SpringElContextsExtension;
import com.intellij.util.SmartList;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 01:19
 */
final class CommentVariablesExtension extends SpringElContextsExtension {
  public static final String EL_VAR = "@el";
  private static final Pattern VAR_PATTERN = Pattern.compile("(.+)\\s*:\\s*(.*)");

  @Override
  public Collection<? extends PsiVariable> getContextVariables(PsiElement contextElement) {
    List<PsiVariable> variables = new SmartList<>();
    PsiFile containingFile = contextElement.getContainingFile();
    if (containingFile != null) {
      PsiElement context = containingFile.getContext();
      if (context != null) {
        for (PsiModifierListOwner commentOwner = PsiTreeUtil.getParentOfType(context,
                PsiModifierListOwner.class); commentOwner != null; commentOwner = PsiTreeUtil.getParentOfType(commentOwner, PsiModifierListOwner.class)) {
          variables.addAll(getVariableFromComments(commentOwner));
        }
      }
    }

    return variables;
  }

  private static Collection<? extends PsiVariable> getVariableFromComments(PsiModifierListOwner commentOwner) {
    List<PsiVariable> variables = new SmartList<>();
    processComments(variables, PsiTreeUtil.getChildrenOfType(commentOwner, PsiComment.class));
    PsiModifierList modifierList = commentOwner.getModifierList();
    if (modifierList != null) {
      processComments(variables, PsiTreeUtil.getChildrenOfType(modifierList, PsiComment.class));
    }

    return variables;
  }

  private static void processComments(List<PsiVariable> variables, @Nullable PsiComment[] comments) {
    if (comments != null) {
      for (PsiComment comment : comments) {
        processVariableDeclarations(comment.getText(), (vars, start, end) -> {
          variables.addAll(getVariables(comment, vars));
        });
      }
    }
  }

  public static void processVariableDeclarations(String text, ElVarsProcessor processor) {
    for (int elVarIndex = text.indexOf(EL_VAR); elVarIndex >= 0; elVarIndex = text.indexOf(EL_VAR, elVarIndex + 1)) {
      int start = text.indexOf('(', elVarIndex) + 1;
      int end = text.indexOf(")", elVarIndex);
      if (start >= end) {
        break;
      }

      String varsString = text.substring(start, end);
      if (StringUtil.isNotEmpty(varsString)) {
        processor.process(varsString, start, end);
      }
    }

  }

  private static List<PsiVariable> getVariables(PsiComment comment, String vars) {
    List<PsiVariable> variables = new SmartList<>();
    for (String var : StringUtil.split(vars, ",")) {
      Matcher varMatcher = VAR_PATTERN.matcher(var.trim());
      if (varMatcher.matches()) {
        String name = varMatcher.group(1).trim();
        String typeName = varMatcher.group(2).trim();
        String fqn = typeName.indexOf(36) >= 0 ? typeName.replace('$', '.') : typeName;
        PsiType type = PsiImplUtil.buildTypeFromTypeString(fqn.trim(), comment, comment.getContainingFile());
        variables.add(new CommentLightVariableBuilder(name, type, comment));
      }
    }

    return variables;
  }

  public interface ElVarsProcessor {

    void process(String var1, int var2, int var3);
  }
}
