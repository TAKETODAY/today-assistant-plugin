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
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;

public final class InfraOldJavaConfigurationUtil {

  public static List<JavaConfigConfiguration> getJavaConfigurations(Module module) {
    JamService jamService = JamService.getJamService(module.getProject());

    return jamService.getJamClassElements(JavaConfigConfiguration.META,
            AnnotationConstant.CONFIGURATION,
            GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module));
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

    ArrayList<InfraJavaExternalBean> extBeans = new ArrayList<>();
    for (JavaConfigConfiguration beans : getJavaConfigurations(module)) {
      for (InfraJavaExternalBean externalBean : beans.getExternalBeans()) {
        PsiMethod psiMethod = externalBean.getPsiElement();
        if (beanNames.contains(psiMethod.getName())) {
          extBeans.add(externalBean);
        }
      }
    }
    return extBeans;
  }

}
