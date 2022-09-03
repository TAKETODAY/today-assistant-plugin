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

package cn.taketoday.assistant.model.converters.fixes.bean;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.util.PsiNavigateUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.converters.CreateElementQuickFixProvider;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraInjection;
import cn.taketoday.assistant.model.xml.beans.TypeHolderUtil;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

class GenericBeanResolveQuickFixProvider implements BeanResolveQuickFixProvider {

  @Override

  public List<LocalQuickFix> getQuickFixes(ConvertContext context, Beans beans, @Nullable String beanId, List<PsiClassType> requiredClasses) {
    LocalQuickFix[] fixes = new CreateGenericBeanQuickFix(beans, beanId).getQuickFixes((GenericDomValue<BeanPointer<?>>) context.getInvocationElement());
    return Arrays.asList(fixes);
  }

  private static final class CreateGenericBeanQuickFix extends CreateElementQuickFixProvider<BeanPointer<?>> {
    private final Beans beans;
    @Nullable
    private final String beanId;

    private CreateGenericBeanQuickFix(Beans beans, @Nullable String beanId) {
      super(message("model.bean.quickfix.message.family.name"));
      this.beans = beans;
      this.beanId = beanId;
    }

    @Override
    @Nullable
    public String getElementName(GenericDomValue<BeanPointer<?>> value) {
      return this.beanId != null ? this.beanId : super.getElementName(value);
    }

    @Override
    protected void apply(String elementName, GenericDomValue<BeanPointer<?>> value) {
      PsiClassType classType;
      PsiClass psiClass;
      if (!FileModificationService.getInstance().preparePsiElementForWrite(this.beans.getXmlElement())) {
        return;
      }
      InfraBean springBean = this.beans.addBean();
      springBean.setName(elementName);
      DomElement parent = value.getParent();
      if ((parent instanceof InfraInjection) && (classType = TypeHolderUtil.getRequiredClassType((InfraInjection) parent)) != null && (psiClass = classType.resolve()) != null) {
        GenericAttributeValue<PsiClass> clazzAttribute = springBean.getClazz();
        if (psiClass.isInterface()) {
          clazzAttribute.setStringValue("");
          PsiNavigateUtil.navigate(clazzAttribute.getXmlAttributeValue());
          return;
        }
        clazzAttribute.setStringValue(psiClass.getQualifiedName());
      }
    }

    @Override
    protected String getFixName(String elementName) {
      return message("model.bean.quickfix.message", elementName);
    }
  }
}
