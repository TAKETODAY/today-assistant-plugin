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

import com.intellij.jam.reflect.JamChildrenQuery;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;

import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;

public class JavaConfigConfiguration extends InfraJavaConfiguration {
  public static final JamClassMeta<JavaConfigConfiguration> META = new JamClassMeta<>(JavaConfigConfiguration.class);

  private static final JamChildrenQuery<JavaConfigJavaBean> BEANS_QUERY =
          JamChildrenQuery.annotatedMethods(JavaConfigJavaBean.META, JavaConfigJavaBean.class);

  private static final JamChildrenQuery<InfraJavaExternalBean> EXTERNAL_BEANS_QUERY =
          JamChildrenQuery.annotatedMethods(InfraJavaExternalBean.META, InfraJavaExternalBean.class);

  public JavaConfigConfiguration(PsiClass psiClass) {
    super(psiClass, AnnotationConstant.CONFIGURATION);
  }

  static {
    META.addChildrenQuery(BEANS_QUERY);
    META.addChildrenQuery(EXTERNAL_BEANS_QUERY);
  }

  @Override
  public List<? extends InfraJavaBean> getBeans() {
    return BEANS_QUERY.findChildren(PsiElementRef.real(getPsiElement()));
  }

  public List<? extends InfraJavaExternalBean> getExternalBeans() {
    return EXTERNAL_BEANS_QUERY.findChildren(PsiElementRef.real(getPsiElement()));
  }
}