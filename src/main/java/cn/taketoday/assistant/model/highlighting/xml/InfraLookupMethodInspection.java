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

package cn.taketoday.assistant.model.highlighting.xml;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import java.util.Collections;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.LookupMethod;
import cn.taketoday.lang.Nullable;

public final class InfraLookupMethodInspection extends InfraBeanInspectionBase {

  private static void checkLookupMethodReturnType(LookupMethod lookupMethod, PsiMethod method, DomElementAnnotationHolder holder) {
    PsiType returnType = method.getReturnType();
    if (returnType == null) {
      holder.createProblem(lookupMethod.getName(), InfraBundle.message("bean.lookup.method.constructor.not.allowed"));
    }
    else if (!(returnType instanceof PsiClassType type) || type.resolve() == null) {
      holder.createProblem(lookupMethod.getName(), InfraBundle.message("bean.lookup.method.incorrect.return.type"));
    }
    else {
      GenericAttributeValue<BeanPointer<?>> bean = lookupMethod.getBean();
      BeanPointer<?> beanPointer = bean.getValue();
      if (beanPointer != null) {
        PsiClass beanClass = beanPointer.getBeanClass();
        if (beanClass == null) {
          String beanName = InfraPresentationProvider.getBeanName(beanPointer);
          holder.createProblem(bean, InfraBundle.message("bean.lookup.method.bean.has.no.class", beanName));
          return;
        }
        PsiClass returnClass = type.resolve();
        if (!BeanCoreUtils.isEffectiveClassType(Collections.singletonList(returnType), beanPointer.getBean())
                && !InheritanceUtil.isInheritorOrSelf(beanClass, returnClass, true)) {
          String beanName2 = InfraPresentationProvider.getBeanName(beanPointer);
          String message = InfraBundle.message("bean.lookup.method.return.type.mismatch", beanName2);
          holder.createProblem(lookupMethod.getName(), message);
          holder.createProblem(bean, message);
        }
      }
    }
  }

  private static void checkLookupMethodIdentifiers(LookupMethod lookupMethod, DomElementAnnotationHolder holder, PsiMethod method) {
    if (!method.hasModifierProperty("public") && !method.hasModifierProperty("protected")) {
      holder.createProblem(lookupMethod.getName(), InfraBundle.message("bean.lookup.method.must.be.public.or.protected"));
    }
    if (method.hasModifierProperty("static")) {
      holder.createProblem(lookupMethod.getName(), InfraBundle.message("bean.lookup.method.must.be.not.static"));
    }
    if (method.getParameterList().getParametersCount() > 0) {
      holder.createProblem(lookupMethod.getName(), InfraBundle.message("bean.lookup.method.must.have.no.parameters"));
    }
  }

  @Override
  protected void checkBean(InfraBean infraBean, Beans beans, DomElementAnnotationHolder holder, @Nullable CommonInfraModel infraModel) {
    for (LookupMethod lookupMethod : infraBean.getLookupMethods()) {
      PsiMethod method = lookupMethod.getName().getValue();
      if (method != null) {
        checkLookupMethodIdentifiers(lookupMethod, holder, method);
        checkLookupMethodReturnType(lookupMethod, method, holder);
      }
    }
  }
}
