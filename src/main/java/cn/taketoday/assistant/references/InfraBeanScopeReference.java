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

package cn.taketoday.assistant.references;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.ide.TypePresentationService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UExpression;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.assistant.model.scope.BeanScope;
import cn.taketoday.assistant.model.scope.InfraBeanScopeManager;

public class InfraBeanScopeReference extends PsiReferenceBase<PsiLanguageInjectionHost> {

  private static final List<String> DEFAULT_SCOPE_NAMES = ContainerUtil.map(BeanScope.getDefaultScopes(), BeanScope::getValue);
  private final UExpression myLiteral;

  public InfraBeanScopeReference(UExpression uLiteral, PsiLanguageInjectionHost element) {
    super(element);
    this.myLiteral = uLiteral;
  }

  public PsiElement resolve() {
    Object value = this.myLiteral.evaluate();
    if (value instanceof String) {
      if (DEFAULT_SCOPE_NAMES.contains(value)) {
        return getElement();
      }
      for (BeanScope beanScope : InfraBeanScopeManager.getCustomBeanScopes(getElement())) {
        if (value.equals(beanScope.getValue())) {
          return getElement();
        }
      }
      return null;
    }
    return null;
  }

  public Object[] getVariants() {
    List<LookupElement> variants = new ArrayList<>(DEFAULT_SCOPE_NAMES.size());
    for (String scope : DEFAULT_SCOPE_NAMES) {
      variants.add(getLookupElement(scope));
    }
    for (BeanScope beanScope : InfraBeanScopeManager.getCustomBeanScopes(getElement())) {
      variants.add(getLookupElement(beanScope.getValue()));
    }
    return variants.toArray(LookupElement.EMPTY_ARRAY);
  }

  private static LookupElementBuilder getLookupElement(String scope) {
    return LookupElementBuilder.create(scope).withIcon(TypePresentationService.getService().getTypeIcon(BeanScope.class));
  }
}
