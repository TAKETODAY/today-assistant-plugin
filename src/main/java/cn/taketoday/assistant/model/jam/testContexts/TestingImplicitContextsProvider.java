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
package cn.taketoday.assistant.model.jam.testContexts;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.util.JamAnnotationTypeUtil;
import cn.taketoday.lang.Nullable;

public abstract class TestingImplicitContextsProvider {
  public static final ExtensionPointName<TestingImplicitContextsProvider> EP_NAME =
          new ExtensionPointName<>("cn.taketoday.assistant.testingImplicitContextsProvider");

  public abstract Collection<CommonInfraModel> getModels(
          @Nullable Module module, ContextConfiguration configuration, Set<String> activeProfiles);

  protected static boolean isAnnotated(
          ContextConfiguration configuration, @Nullable Module module, String annotation) {
    if (module == null)
      return false;

    List<String> annotations = ContainerUtil.mapNotNull(
            JamAnnotationTypeUtil.getAnnotationTypesWithChildrenIncludingTests(module, annotation),
            PsiClass::getQualifiedName);

    return AnnotationUtil.isAnnotated(configuration.getPsiElement(), annotations, 0);
  }
}