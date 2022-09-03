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
package cn.taketoday.assistant.references.property;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixProvider;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.beanProperties.BeanProperty;
import com.intellij.psi.impl.beanProperties.CreateBeanPropertyFixes;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.lang.Nullable;

/**
 * @author Yann C&eacute;bron
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class BindingPropertyReference extends PsiReferenceBase<PsiElement> implements LocalQuickFixProvider, EmptyResolveMessageProvider {

  private final BindingPropertyReferenceSet mySet;
  private final int myIndex;
  private final boolean myHasIndexAccess;

  BindingPropertyReference(BindingPropertyReferenceSet set, TextRange range, int index) {
    super(set.getElement(), range);

    mySet = set;
    myIndex = index;

    String value = getValue();
    int indexStartIdx = value.indexOf('[');
    myHasIndexAccess = indexStartIdx != -1 && value.endsWith("]");
    if (myHasIndexAccess) {
      setRangeInElement(TextRange.from(range.getStartOffset(), indexStartIdx));
    }
  }

  @Override
  public boolean isSoft() {
    return mySet.isSoft();
  }

  @Nullable
  private PsiClass getPsiClass() {
    if (isFirst()) {
      return mySet.getModelClass();
    }

    BindingPropertyReference reference = mySet.getReference(myIndex - 1);
    PsiMethod psiMethod = (PsiMethod) reference.resolve();
    if (psiMethod == null) {
      return null;
    }

    PsiType type = psiMethod.getReturnType();
    if (type == null) {
      return null;
    }

    boolean usingIndexAccess = reference.hasIndexAccess();
    if (usingIndexAccess) {
      type = type.getDeepComponentType();
    }
    if (type instanceof final PsiClassType psiClassType) {
      PsiClass psiClass = psiClassType.resolve();

      if (usingIndexAccess &&
              psiClassType.getParameterCount() == 1 &&
              InheritanceUtil.isInheritor(psiClass, CommonClassNames.JAVA_UTIL_LIST) ||
              InheritanceUtil.isInheritor(psiClass, CommonClassNames.JAVA_UTIL_SET)) {
        PsiType collectionElementType = psiClassType.getParameters()[0];
        return collectionElementType instanceof PsiClassType ? ((PsiClassType) collectionElementType).resolve() : null;
      }

      return psiClass;
    }
    return null;
  }

  private boolean isFirst() {
    return myIndex == 0;
  }

  private boolean hasIndexAccess() {
    return myHasIndexAccess;
  }

  @Override
  public PsiElement resolve() {
    PsiClass psiClass = getPsiClass();
    String propertyName = getValue();

    return PropertyUtilBase.findPropertyGetter(psiClass, propertyName, false, true);
  }

  @Override
  public Object[] getVariants() {
    return ContainerUtil.map2Array(resolveProperties(), PsiNamedElement.class, BeanProperty::getPsiElement);
  }

  private List<BeanProperty> resolveProperties() {
    PsiClass psiClass = getPsiClass();
    if (psiClass == null) {
      return Collections.emptyList();
    }

    Map<String, PsiMethod> properties = PropertyUtilBase.getAllProperties(psiClass, false, true, true);
    return ContainerUtil.mapNotNull(properties.values(),
            (NullableFunction<PsiMethod, BeanProperty>) BeanProperty::createBeanProperty);
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    String name = PropertyUtilBase.getPropertyName(newElementName);
    return super.handleElementRename(name == null ? newElementName : name);
  }

  @Override
  public PsiElement bindToElement(PsiElement element) throws IncorrectOperationException {
    if (element instanceof PsiMethod) {
      String propertyName = PropertyUtilBase.getPropertyName((PsiMember) element);
      if (propertyName != null) {
        return super.handleElementRename(propertyName);
      }
    }
    return getElement();
  }

  @Override
  public LocalQuickFix[] getQuickFixes() {
    if (resolve() != null) {
      return LocalQuickFix.EMPTY_ARRAY;
    }
    PsiClass psiClass = getPsiClass();
    if (psiClass == null) {
      return LocalQuickFix.EMPTY_ARRAY;
    }
    return CreateBeanPropertyFixes.createFixes(getValue(), psiClass, null, false);
  }

  @Override
  public String getUnresolvedMessagePattern() {
    return InfraBundle.message("BindingPropertyReference.unresolved.message.pattern", getValue());
  }
}