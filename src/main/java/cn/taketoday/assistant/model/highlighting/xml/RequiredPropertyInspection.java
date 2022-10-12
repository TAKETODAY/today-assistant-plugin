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

package cn.taketoday.assistant.model.highlighting.xml;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.SmartList;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.beans.AutowireUtil;
import cn.taketoday.assistant.model.actions.generate.InfraPropertiesGenerateProvider;
import cn.taketoday.assistant.model.actions.generate.InfraTemplateBuilder;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.lang.Nullable;

public final class RequiredPropertyInspection extends InfraBeanInspectionBase {

  @Override
  protected void checkBean(InfraBean infraBean, Beans beans, DomElementAnnotationHolder holder, @Nullable CommonInfraModel infraModel) {
    PsiClass psiClass;
    if (infraBean.isAbstract() || infraModel == null || (psiClass = PsiTypesUtil.getPsiClass(infraBean.getBeanType())) == null) {
      return;
    }
    Map<String, PsiMethod> properties = PropertyUtilBase.getAllProperties(psiClass, true, false);
    List<InfraPropertyDefinition> definedProperties = infraBean.getAllProperties();
    SmartList smartList = new SmartList();
    SmartList smartList2 = new SmartList();
    for (Map.Entry<String, PsiMethod> entry : properties.entrySet()) {
      if (AnnotationUtil.isAnnotated(entry.getValue(), AnnotationConstant.REQUIRED, 0) && !isDefined(definedProperties,
              entry.getKey()) && !AutowireUtil.isAutowired(infraBean, infraModel, entry.getValue())) {
        smartList.add(entry.getKey());
        smartList2.add(entry.getValue());
      }
    }
    if (smartList.isEmpty()) {
      return;
    }
    holder.createProblem(DomUtil.hasXml(infraBean.getClazz()) ? infraBean.getClazz() : infraBean, HighlightSeverity.ERROR,
            InfraBundle.message("required.properties.missed", StringUtil.join(smartList, ",")), new LocalQuickFix() {

              public String getFamilyName() {
                String message = InfraBundle.message("create.missing.properties");
                return message;
              }

              public void applyFix(Project project, ProblemDescriptor descriptor) {
                Editor editor = InfraTemplateBuilder.getEditor(descriptor);
                InfraPropertiesGenerateProvider.doGenerate(editor, infraBean, project, (PsiMethod[]) smartList2.toArray(PsiMethod.EMPTY_ARRAY));
              }
            });
  }

  private static boolean isDefined(List<InfraPropertyDefinition> list, String property) {
    for (InfraPropertyDefinition definition : list) {
      String name = definition.getPropertyName();
      if (name != null && name.equals(property)) {
        return true;
      }
    }
    return false;
  }
}
