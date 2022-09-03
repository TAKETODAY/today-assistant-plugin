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

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.util.InfraUtils;

public class InfraBeanNamesConverterImpl extends InfraBeanNamesConverter {

  public String toString(List<String> strings, ConvertContext context) {
    return StringUtil.join(strings, ",");
  }

  public List<String> fromString(String s, ConvertContext context) {
    if (s == null) {
      return null;
    }
    return InfraUtils.tokenize(s);
  }

  public PsiReference[] createReferences(final GenericDomValue<List<String>> genericDomValue, final PsiElement element, final ConvertContext context) {
    List<String> strings = genericDomValue.getValue();
    if (strings != null) {
      List<PsiReference> references = new ArrayList<>(strings.size());
      for (String string : strings) {
        int offset = element.getText().indexOf(string);
        if (offset >= 0) {
          references.add(new PsiReferenceBase<>(element, TextRange.from(offset, string.length())) {

            public PsiElement resolve() {
              DomElement parent = genericDomValue.getParent();
              if (parent instanceof CommonInfraBean bean) {
                return InfraBeanService.of().createBeanPointer(bean).getPsiElement();
              }
              return null;
            }

            public boolean isSoft() {
              return true;
            }

            public Object[] getVariants() {
              return BeanCoreUtils.suggestBeanNames(InfraConverterUtil.getCurrentBean(context));
            }

            public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
              InfraBean bean = (InfraBean) genericDomValue.getParent();
              bean.setName(newElementName);
              return element;
            }
          });
        }
      }
      return references.toArray(PsiReference.EMPTY_ARRAY);
    }
    return PsiReference.EMPTY_ARRAY;
  }
}
