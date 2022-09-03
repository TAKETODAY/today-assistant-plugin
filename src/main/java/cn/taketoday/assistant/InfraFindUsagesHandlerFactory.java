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

package cn.taketoday.assistant;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.beanProperties.BeanProperty;
import com.intellij.psi.impl.beanProperties.BeanPropertyFindUsagesHandler;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomUtil;

import java.util.List;

import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.highlighting.jam.AutowiredBeanFindUsagesHandler;
import cn.taketoday.assistant.model.highlighting.jam.JavaBeanReferencesFindUsagesHandler;
import cn.taketoday.assistant.model.properties.InfraPropertiesUtil;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.util.InfraUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/26 19:56
 */
public class InfraFindUsagesHandlerFactory extends FindUsagesHandlerFactory {

  @Override
  public boolean canFindUsages(PsiElement psiElement) {
    Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (InfraUtils.hasFacet(module)) {
      PsiFile containingFile = psiElement.getContainingFile();
      if (containingFile instanceof XmlFile xmlFile) {
        return InfraDomUtils.isInfraXml(xmlFile);
      }
      else {
        return isContextBeanCandidate(psiElement);
      }
    }
    return false;
  }

  private static boolean isContextBeanCandidate(PsiElement psiElement) {
    return psiElement instanceof PsiMethod method
            && MetaAnnotationUtil.isMetaAnnotated(method, List.of(AnnotationConstant.COMPONENT));
  }

  @Override
  public FindUsagesHandler createFindUsagesHandler(PsiElement element, boolean forHighlightUsages) {
    if (isContextBeanCandidate(element)) {
      return new AutowiredBeanFindUsagesHandler(element);
    }
    else {
      BeanProperty property = InfraPropertiesUtil.getBeanProperty(element);
      if (property != null) {
        return new BeanPropertyFindUsagesHandler(property);
      }
      else {
        CommonInfraBean bean = DomUtil.findDomElement(element, DomInfraBean.class, false);
        return bean != null ? new JavaBeanReferencesFindUsagesHandler(bean) : null;
      }
    }
  }
}
