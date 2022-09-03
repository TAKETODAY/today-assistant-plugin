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
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.AddDomElementQuickFix;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import java.util.HashSet;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.jam.javaConfig.InfraJavaConfiguration;
import cn.taketoday.assistant.model.jam.javaConfig.InfraOldJavaConfigurationUtil;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

public final class BeanInstantiationInspection extends InfraBeanInspectionBase {

  @Override
  protected void checkBean(InfraBean infraBean, Beans beans, DomElementAnnotationHolder holder, @Nullable CommonInfraModel springModel) {
    String message;
    PsiClass psiClass = infraBean.getClazz().getValue();
    if (psiClass != null && !infraBean.isAbstract() && psiClass.hasModifierProperty("abstract")) {
      boolean factory = DomUtil.hasXml(infraBean.getFactoryMethod());
      boolean lookup = hasLookupMethods(infraBean) || hasAnnotatedLookupMethods(psiClass);
      if (!factory && !lookup && !isJavaConfigBean(infraBean)) {
        GenericAttributeValue<PsiClass> clazz = infraBean.getClazz();
        HighlightSeverity highlightSeverity = HighlightSeverity.WARNING;
        if (psiClass.isInterface()) {
          message = InfraBundle.message("interface.not.allowed");
        }
        else {
          message = InfraBundle.message("abstract.class.not.allowed");
        }
        holder.createProblem(clazz, highlightSeverity, message, new MarkAbstractFix(infraBean.getAbstract()));
      }
    }
  }

  private static boolean hasLookupMethods(InfraBean infraBean) {
    return hasLookupMethods(infraBean, new HashSet<>());
  }

  private static boolean hasLookupMethods(InfraBean infraBean, Set<InfraBean> visited) {
    BeanPointer<?> parent;
    if (visited.contains(infraBean)) {
      return false;
    }
    if (infraBean.getLookupMethods().size() > 0) {
      return true;
    }
    visited.add(infraBean);
    GenericAttributeValue<BeanPointer<?>> parentBeanValue = infraBean.getParentBean();
    if (DomUtil.hasXml(parentBeanValue) && (parent = parentBeanValue.getValue()) != null) {
      CommonInfraBean bean = parent.getBean();
      if (bean instanceof InfraBean) {
        return hasLookupMethods((InfraBean) bean, visited);
      }
      return false;
    }
    return false;
  }

  private static boolean hasAnnotatedLookupMethods(PsiClass psiClass) {
    for (PsiModifierListOwner psiModifierListOwner : psiClass.getAllMethods()) {
      if (AnnotationUtil.isAnnotated(psiModifierListOwner, AnnotationConstant.LOOKUP_ANNOTATION, 0)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isJavaConfigBean(InfraBean infraBean) {
    Module module;
    PsiClass beanClass = PsiTypesUtil.getPsiClass(infraBean.getBeanType());
    if (DomUtil.hasXml(infraBean) && beanClass != null && (module = infraBean.getModule()) != null) {
      for (InfraJavaConfiguration javaConfiguration : InfraOldJavaConfigurationUtil.getJavaConfigurations(module)) {
        if (beanClass.equals(javaConfiguration.getPsiClass())) {
          return true;
        }
      }
      return false;
    }
    return false;
  }

  private static class MarkAbstractFix extends AddDomElementQuickFix<GenericAttributeValue<Boolean>> {

    MarkAbstractFix(GenericAttributeValue<Boolean> value) {
      super(value);
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      this.myElement.setValue(Boolean.TRUE);
    }

    public String getName() {
      return InfraBundle.message("mark.bean.as.abstract");
    }
  }
}
