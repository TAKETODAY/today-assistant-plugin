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

package cn.taketoday.assistant.model.converters;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.assistant.model.xml.beans.Alias;

public class AliasNameConverterImpl extends AliasNameConverter {

  public PsiReference[] createReferences(GenericDomValue<String> genericDomValue, PsiElement element, ConvertContext context) {
    if (PlaceholderUtils.getInstance().isRawTextPlaceholder(genericDomValue)) {
      return PlaceholderUtils.getInstance().createPlaceholderPropertiesReferences(genericDomValue);
    }
    PsiReference[] psiReferenceArr = { new PsiReferenceBase<>(element, true) {

      public PsiElement resolve() {
        return getElement().getParent().getParent();
      }

      public Object[] getVariants() {
        BeanPointer<?> beanPointer;
        Alias alias = genericDomValue.getParentOfType(Alias.class, false);
        if (alias != null && (beanPointer = alias.getAliasedBean().getValue()) != null) {
          return BeanCoreUtils.suggestBeanNames(beanPointer.getBean());
        }
        return PsiReference.EMPTY_ARRAY;
      }
    } };
    return psiReferenceArr;
  }
}
