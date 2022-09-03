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

package cn.taketoday.assistant.model.actions.generate;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.jvm.JvmValue;
import com.intellij.lang.jvm.actions.AnnotationRequest;
import com.intellij.lang.jvm.actions.CreateFieldRequest;
import com.intellij.lang.jvm.actions.ExpectedType;
import com.intellij.lang.jvm.actions.ExpectedTypesKt;
import com.intellij.lang.jvm.types.JvmSubstitutor;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJvmSubstitutor;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiTypesUtil;

import java.util.Collection;
import java.util.List;

import cn.taketoday.lang.Nullable;
import kotlin.collections.CollectionsKt;

final class AutowiredFieldInfo implements CreateFieldRequest {
  private final SmartPsiElementPointer<PsiClass> psiClassPointer;
  private final PsiClass candidateBeanClass;
  private final String name;
  private final Project project;

  public AutowiredFieldInfo(PsiClass psiClass, PsiClass candidateBeanClass, String name, Project project) {
    this.candidateBeanClass = candidateBeanClass;
    this.name = name;
    this.project = project;
    this.psiClassPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(psiClass);
  }

  public JvmSubstitutor getTargetSubstitutor() {
    return new PsiJvmSubstitutor(this.project, PsiSubstitutor.EMPTY);
  }

  public Collection<AnnotationRequest> getAnnotations() {
    return CollectionsKt.emptyList();
  }

  public Collection<JvmModifier> getModifiers() {
    return CollectionsKt.listOf(JvmModifier.PRIVATE);
  }

  public boolean isConstant() {
    return false;
  }

  public List<ExpectedType> getFieldType() {
    JvmType classType = PsiTypesUtil.getClassType(this.candidateBeanClass);
    // FIXME ExpectedType.Kind
    return ExpectedTypesKt.expectedTypes(classType, ExpectedType.Kind.EXACT);
  }

  public String getFieldName() {
    return this.name;
  }

  public boolean isValid() {
    PsiClass element = this.psiClassPointer.getElement();
    if (element != null) {
      return element.isValid();
    }
    return false;
  }

  @Nullable
  public JvmValue getInitializer() {
    return null;
  }
}
