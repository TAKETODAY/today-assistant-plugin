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

package cn.taketoday.assistant.web.mvc.client.rest;

import com.intellij.microservices.jvm.cache.ScopedCacheValueHolder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

import java.util.List;

public final class TestRestTemplateModel extends RestTemplatesModel {

  public static final TestRestTemplateModel INSTANCE = new TestRestTemplateModel();

  @Override
  protected List<PsiMethod> getUrlApiMethods(Module module) {
    RestOperationsUtils restOperationsUtils = RestOperationsUtils.INSTANCE;
    Project project = module.getProject();
    return restOperationsUtils.getTestRestTemplateMethods(project);
  }

  @Override
  public List<PsiClass> getApiClasses(ScopedCacheValueHolder<?> scopedCacheValueHolder) {
    return RestOperationsUtils.INSTANCE.getTestRestTemplateApiClasses(scopedCacheValueHolder);
  }

  @Override
  public List<PsiMethod> getApiMethods(Project project) {
    return RestOperationsUtils.INSTANCE.getTestRestTemplateMethods(project);
  }

  @Override
  protected String getWebClientPackage() {
    return RestOperationsConstants.REST_testOPERATIONS_PACKAGE;
  }

}
