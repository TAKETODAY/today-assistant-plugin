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

package cn.taketoday.assistant.web.mvc.mapping;

import com.intellij.microservices.url.UrlPath;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.PomNamedTarget;
import com.intellij.pom.PomTarget;
import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiElement;
import com.intellij.xml.util.PsiElementPointer;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.lang.Nullable;

public interface UrlMappingElement {
  @Deprecated
  String getURL();

  UrlPath getUrlPath();

  @Nullable
  PomNamedTarget getPomTarget();

  @Nullable
  PsiElement getNavigationTarget();

  String getPresentation();

  RequestMethod[] getMethod();

  boolean isDefinedInBean(BeanPointer<? extends CommonInfraBean> beanPointer);

  @Deprecated
  default PsiElementPointer getDefinition() {
    return () -> {
      PomNamedTarget pomTarget = getPomTarget();
      if (pomTarget == null && (this instanceof PomTarget)) {
        pomTarget = (PomNamedTarget) this;
      }
      PsiElement navigationTarget = getNavigationTarget();
      if (navigationTarget == null) {
        return null;
      }
      if (pomTarget != null) {
        Project project = navigationTarget.getProject();
        return PomService.convertToPsi(project, pomTarget);
      }
      return navigationTarget;
    };
  }

  @Nullable
  default PsiElement getDocumentationPsiElement() {
    return getNavigationTarget();
  }

  static String getPathPresentation(UrlMappingElement item) {
    String presentation = item.getPresentation();
    return StringUtil.startsWithChar(presentation, '/') ? presentation : "/" + presentation;
  }

  @Nullable
  static String getContainingFileName(UrlMappingElement item) {
    VirtualFile file;
    PsiElement psiElement = item.getNavigationTarget();
    if (psiElement == null || (file = psiElement.getContainingFile().getVirtualFile()) == null) {
      return null;
    }
    return file.getName();
  }

  @Nullable
  static String getRequestMethodPresentation(UrlMappingElement item) {
    if (item.getMethod().length == 0) {
      return null;
    }
    return RequestMethod.getDisplay(item.getMethod());
  }
}
