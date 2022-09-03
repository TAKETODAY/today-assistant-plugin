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

package cn.taketoday.assistant.web.mvc.el;

import com.intellij.javaee.el.ELElementProcessor;
import com.intellij.javaee.el.ELExpressionHolder;
import com.intellij.javaee.el.providers.ElVariablesProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiVariable;
import com.intellij.util.Processor;

import java.util.Objects;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.utils.light.InfraImplicitVariable;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.services.WebMvcUtils;

public class WebMvcElVariablesProvider extends ElVariablesProvider {

  public boolean processImplicitVariables(PsiElement element, ELExpressionHolder holder, ELElementProcessor processor) {
    Module module = ModuleUtilCore.findModuleForPsiElement(holder);
    if (!InfraUtils.isEnabledModule(module)) {
      return true;
    }
    if (!WebMvcElServletShortcutVariableKt.processShortcuts(element, processor)) {
      return false;
    }
    Objects.requireNonNull(processor);
    if (WebMvcUtils.processVariables(holder, (Processor<? super PsiVariable>) processor::processVariable, processor.getNameHint())) {
      return processConfigurationVariables(holder, processor, module);
    }
    return false;
  }

  private static boolean processConfigurationVariables(ELExpressionHolder holder, ELElementProcessor elElementProcessor, Module module) {
    PsiClass internalViewResolver = InfraUtils.findLibraryClass(module, InfraMvcConstant.INTERNAL_RESOURCE_VIEW_RESOLVER);
    if (internalViewResolver == null) {
      return true;
    }
    Processor<BeanPointer<?>> processor = pointer -> {
      CommonInfraBean bean = pointer.getBean();
      String contextAttributeName = InfraPropertyUtils.getPropertyStringValue(bean, "requestContextAttribute");
      if (StringUtil.isEmpty(contextAttributeName)) {
        return true;
      }
      PsiClassType type = JavaPsiFacade.getElementFactory(holder.getProject()).createTypeByFQClassName(InfraMvcConstant.REQUEST_CONTEXT);
      InfraImplicitVariable variable = new InfraImplicitVariable(contextAttributeName, type, bean.getIdentifyingPsiElement());
      return elElementProcessor.processVariable(variable);
    };
    ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byClass(internalViewResolver).withInheritors();
    for (InfraModel infraModel : InfraManager.from(module.getProject()).getAllModels(module)) {
      if (!infraModel.processByClass(searchParameters, processor)) {
        return false;
      }
    }
    return true;
  }
}
