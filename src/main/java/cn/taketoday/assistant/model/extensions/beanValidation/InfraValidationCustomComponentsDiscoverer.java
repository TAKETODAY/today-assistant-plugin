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

package cn.taketoday.assistant.model.extensions.beanValidation;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.CommonProcessors;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import cn.taketoday.assistant.JavaeeConstant;
import cn.taketoday.assistant.context.model.CacheableCommonInfraModel;
import cn.taketoday.assistant.context.model.LocalModel;
import cn.taketoday.assistant.context.model.LocalXmlModelImpl;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.custom.CustomLocalComponentsDiscoverer;
import cn.taketoday.assistant.model.jam.stereotype.CustomInfraComponent;
import cn.taketoday.assistant.util.InfraUtils;

public class InfraValidationCustomComponentsDiscoverer extends CustomLocalComponentsDiscoverer {

  @Override
  public Collection<CommonInfraBean> getCustomComponents(LocalModel infraModel) {
    Module module = infraModel.getModule();
    if (module == null || DumbService.isDumb(module.getProject())) {
      return Collections.emptyList();
    }
    Collection<CommonInfraBean> myValidators = new HashSet<>();
    myValidators.addAll(getConstraintValidatorBeans(infraModel, module, JavaeeConstant.JAVAX_CONSTRAINT_VALIDATOR, JavaeeConstant.JAVAX_VALIDATOR_FACTORY));
    myValidators.addAll(getConstraintValidatorBeans(infraModel, module, JavaeeConstant.JAKARTA_CONSTRAINT_VALIDATOR, JavaeeConstant.JAKARTA_VALIDATOR_FACTORY));
    return myValidators;
  }

  private static Collection<CommonInfraBean> getConstraintValidatorBeans(LocalModel<?> infraModel, Module module, String validatorClass, String factoryClass) {
    PsiModifierList modifierList;
    PsiClass constraintValidator = InfraUtils.findLibraryClass(module, validatorClass);
    if (constraintValidator == null) {
      return Collections.emptyList();
    }
    PsiClass validatorFactory = InfraUtils.findLibraryClass(module, factoryClass);
    if (validatorFactory == null || !doesBeanExist(infraModel, validatorFactory)) {
      return Collections.emptyList();
    }
    Collection<CommonInfraBean> validators = new HashSet<>();
    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
    for (PsiClass aClass : ClassInheritorsSearch.search(constraintValidator, scope, true).findAll()) {
      if (!aClass.isInterface() && (modifierList = aClass.getModifierList()) != null && !modifierList.hasModifierProperty("abstract")) {
        validators.add(new CustomInfraComponent(aClass));
      }
    }
    return validators;
  }

  public static boolean doesBeanExist(LocalModel infraModel, PsiClass validatorFactory) {
    CommonProcessors.FindFirstProcessor<BeanPointer<?>> findFirstProcessor = new CommonProcessors.FindFirstProcessor<>();
    ModelSearchParameters.BeanClass params = ModelSearchParameters.byClass(validatorFactory);
    RecursionManager.doPreventingRecursion(infraModel, false, () -> {
      if (infraModel instanceof LocalXmlModelImpl) {
        ((LocalXmlModelImpl) infraModel).processLocalBeansByClass(params, findFirstProcessor, true);
        return null;
      }
      else if (infraModel instanceof CacheableCommonInfraModel) {
        ((CacheableCommonInfraModel) infraModel).processLocalBeansByClass(params, findFirstProcessor);
        return null;
      }
      else {
        return null;
      }
    });
    return findFirstProcessor.isFound();
  }
}
