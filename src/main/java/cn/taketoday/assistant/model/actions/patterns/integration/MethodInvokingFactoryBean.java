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

package cn.taketoday.assistant.model.actions.patterns.integration;

import com.intellij.codeInsight.completion.JavaLookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.codeInsight.template.impl.MacroCallNode;
import com.intellij.codeInsight.template.macro.MacroFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.NlsActions;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.util.xml.actions.generate.DomTemplateRunner;
import com.intellij.util.xml.impl.DomTemplateRunnerImpl;

import java.util.LinkedHashSet;
import java.util.Map;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.actions.generate.InfraBeanGenerateProvider;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.lang.Nullable;

public abstract class MethodInvokingFactoryBean extends InfraBeanGenerateProvider {

  public MethodInvokingFactoryBean(@NlsActions.ActionText String text) {
    super(text, null);
  }

  protected void runTemplate(Editor editor, PsiFile file, InfraBean springBean, Map<String, String> predefinedVars) {
    super.runTemplate(editor, file, springBean, predefinedVars);
    ((DomTemplateRunnerImpl) DomTemplateRunner.getInstance(file.getProject())).runTemplate(springBean, editor, this.getTemplate(springBean), predefinedVars);
  }

  protected Template getTemplate(InfraBean springBean) {
    TemplateManager manager = TemplateManager.getInstance(springBean.getManager().getProject());
    Template template = manager.createTemplate("", "");
    template.setToReformat(true);
    Expression completeExpression = new MacroCallNode(MacroFactory.createMacro("complete"));
    Expression targetMethodExpression = getTargetMethodExpression(springBean);
    template.addTextSegment("<");
    template.addVariableSegment("NS_PREFIX");
    template.addTextSegment("bean id=\"");
    template.addVariable("BEAN_NAME", completeExpression, completeExpression, true);
    template.addTextSegment("\" class=\"" + this.getClassName() + "\">");
    template.addTextSegment("<");
    template.addVariableSegment("NS_PREFIX");
    template.addTextSegment("property name=\"targetObject\" ref=\"");
    template.addVariable("TARGET_OBJECT", completeExpression, completeExpression, true);
    template.addTextSegment("\" />");
    template.addTextSegment("<");
    template.addVariableSegment("NS_PREFIX");
    template.addTextSegment("property name=\"targetMethod\" value=\"");
    template.addVariable("TARGET_METHOD", targetMethodExpression, targetMethodExpression, true);
    template.addTextSegment("\" /></");
    template.addVariableSegment("NS_PREFIX");
    template.addTextSegment("bean>");
    return template;
  }

  protected abstract String getClassName();

  private static Expression getTargetMethodExpression(InfraBean springBean) {
    final InfraBean copy = springBean.createStableCopy();
    return new Expression() {
      public Result calculateResult(ExpressionContext context) {
        return new TextResult("");
      }

      public LookupElement[] calculateLookupItems(ExpressionContext context) {
        PsiClass psiClass = MethodInvokingFactoryBean.getTargetObjectPsiClass(copy);
        if (psiClass == null) {
          return LookupElement.EMPTY_ARRAY;
        }
        else {
          LinkedHashSet<LookupElement> items = new LinkedHashSet();

          for (PsiMethod psiMethod : psiClass.getAllMethods()) {
            if (psiMethod.hasModifierProperty("public") && psiMethod.getParameterList().getParametersCount() == 0) {
              items.add(JavaLookupElementBuilder.forMethod(psiMethod, PsiSubstitutor.EMPTY));
            }
          }

          return items.toArray(LookupElement.EMPTY_ARRAY);
        }
      }
    };
  }

  private static @Nullable PsiClass getTargetObjectPsiClass(InfraBean springBean) {
    InfraPropertyDefinition property = InfraPropertyUtils.findPropertyByName(springBean, "targetObject");
    if (property == null) {
      return null;
    }
    else {
      BeanPointer<?> beanPointer = property.getRefValue();
      return beanPointer != null ? beanPointer.getBeanClass() : null;
    }
  }
}
