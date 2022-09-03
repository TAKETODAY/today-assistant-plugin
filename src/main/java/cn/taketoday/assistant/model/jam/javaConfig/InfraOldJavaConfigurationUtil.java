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
package cn.taketoday.assistant.model.jam.javaConfig;

import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.SmartList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.lang.Nullable;

public final class InfraOldJavaConfigurationUtil {

  public static List<JavaConfigConfiguration> getJavaConfigurations(Module module) {
    JamService jamService = JamService.getJamService(module.getProject());

    return jamService.getJamClassElements(JavaConfigConfiguration.META,
            AnnotationConstant.CONFIGURATION,
            GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module));
  }

  public static List<BeanPointer<?>> findExternalBeans(PsiMethod psiMethod) {
    Module module = ModuleUtilCore.findModuleForPsiElement(psiMethod);
    PsiClass psiClass = psiMethod.getContainingClass();
    if (module == null || psiClass == null) {
      return Collections.emptyList();
    }
    if (getExternalBean(psiMethod) == null) {
      return Collections.emptyList();
    }
    List<BeanPointer<?>> extBeans = new SmartList<>();
    for (InfraModel model : InfraManager.from(psiMethod.getProject()).getAllModels(module)) {
      boolean hasJavaConfigBean = InfraModelSearchers.doesBeanExist(model, ModelSearchParameters.byClass(psiClass));
      if (hasJavaConfigBean) {
        String externalBeanName = psiMethod.getName();
        for (BeanPointer springBean : InfraModelVisitorUtils.getAllDomBeans(model)) {
          String beanName = springBean.getName();
          if (externalBeanName.equals(beanName) || Arrays.asList(springBean.getAliases()).contains(externalBeanName)) {
            extBeans.add(springBean);
          }
        }
      }
    }
    return extBeans;
  }

  public static List<InfraJavaExternalBean> findExternalBeanReferences(CommonInfraBean infraBean) {
    XmlTag element = infraBean.getXmlTag();
    if (element == null) {
      return Collections.emptyList();
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) {
      return Collections.emptyList();
    }

    Set<String> beanNames = InfraBeanUtils.of().findBeanNames(infraBean);
    if (beanNames.isEmpty()) {
      return Collections.emptyList();
    }

    List<InfraJavaExternalBean> extBeans = new ArrayList<>();
    for (JavaConfigConfiguration javaConfiguration : getJavaConfigurations(module)) {
      for (InfraJavaExternalBean externalBean : javaConfiguration.getExternalBeans()) {
        PsiMethod psiMethod = externalBean.getPsiElement();
        if (beanNames.contains(psiMethod.getName())) {
          extBeans.add(externalBean);
        }
      }
    }
    return extBeans;
  }

  @Nullable
  public static InfraJavaExternalBean getExternalBean(PsiMethod psiMethod) {
    Module module = ModuleUtilCore.findModuleForPsiElement(psiMethod);
    if (module != null) {
      for (JavaConfigConfiguration javaConfiguration : getJavaConfigurations(module)) {
        if (psiMethod.getContainingFile().equals(javaConfiguration.getPsiClass().getContainingFile())) {
          for (InfraJavaExternalBean externalBean : javaConfiguration.getExternalBeans()) {
            if (psiMethod.equals(externalBean.getPsiElement())) {
              return externalBean;
            }
          }
        }
      }
    }
    return null;
  }
}
