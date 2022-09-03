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

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamFieldMeta;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiField;
import com.intellij.psi.util.PsiTypesUtil;

import cn.taketoday.assistant.app.InfraClassesConstants;
import cn.taketoday.lang.Nullable;

public class NestedConfigurationProperty extends JamBaseElement<PsiField> {
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraClassesConstants.NESTED_CONFIGURATION_PROPERTY);
  public static final JamFieldMeta<NestedConfigurationProperty> FIELD_META = new JamFieldMeta<>(NestedConfigurationProperty.class)
          .addAnnotation(ANNOTATION_META);

  public NestedConfigurationProperty(PsiElementRef<?> ref) {
    super(ref);
  }

  public boolean typeMatches(PsiClass psiClass) {
    PsiField field = getPsiElement();
    PsiClass fieldClass = PsiTypesUtil.getPsiClass(field.getType());
    return psiClass.isEquivalentTo(fieldClass);
  }

  @Nullable
  public ConfigurationProperties getEnclosingConfigurationProperties() {
    PsiClass containingClass = getPsiElement().getContainingClass();
    if (containingClass == null) {
      return null;
    }
    return ConfigurationProperties.CLASS_META.getJamElement(containingClass);
  }
}
