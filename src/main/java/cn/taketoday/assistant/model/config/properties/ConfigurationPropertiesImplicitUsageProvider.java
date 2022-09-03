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

package cn.taketoday.assistant.model.config.properties;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PropertyUtilBase;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.InfraClassesConstants;
import cn.taketoday.assistant.app.application.config.InfraConfigurationPropertyReferenceSearcher;
import cn.taketoday.assistant.util.InfraUtils;

public class ConfigurationPropertiesImplicitUsageProvider implements ImplicitUsageProvider {

  public boolean isImplicitUsage(PsiElement element) {
    if (element instanceof PsiMethod) {
      return isGetterOrSetterInConfigurationPropertiesClass((PsiMethod) element);
    }
    return false;
  }

  private static boolean isGetterOrSetterInConfigurationPropertiesClass(PsiMethod psiMethod) {
    Project project = psiMethod.getProject();
    if (!InfraUtils.hasFacets(project) || !InfraLibraryUtil.hasFrameworkLibrary(project)) {
      return false;
    }
    if (PropertyUtilBase.isSimplePropertyAccessor(psiMethod, true)) {
      return InfraConfigurationPropertyReferenceSearcher.getPrefixIfRelevantPropertyMethod(psiMethod, false) != null;
    }
    else
      return psiMethod.isConstructor()
              && psiMethod.hasModifierProperty("public")
              && psiMethod.hasParameters()
              && AnnotationUtil.findAnnotation(psiMethod, true, InfraClassesConstants.CONSTRUCTOR_BINDING) != null;
  }

  public boolean isImplicitRead(PsiElement element) {
    return false;
  }

  public boolean isImplicitWrite(PsiElement element) {
    return false;
  }
}
