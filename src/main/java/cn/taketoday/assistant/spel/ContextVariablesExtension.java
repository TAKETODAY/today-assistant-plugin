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


package cn.taketoday.assistant.spel;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiVariable;
import com.intellij.spring.el.contextProviders.SpringElContextsExtension;
import com.intellij.util.SmartList;

import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 01:19
 */
final class ContextVariablesExtension extends SpringElContextsExtension {

  @Override
  public Collection<? extends PsiVariable> getContextVariables(PsiElement contextElement) {
    List<PsiVariable> variables = new SmartList<>();
    InfraBeansAsElVariableUtil.addVariables(variables, getModel(contextElement));
    return variables;
  }

  @Nullable
  @Override
  public PsiVariable findContextVariable(PsiElement contextElement, String nameHint) {
    BeanPointer<?> bean = InfraModelSearchers.findBean(getModel(contextElement), nameHint);
    return bean != null ? InfraBeansAsElVariableUtil.createVariable(bean, nameHint) : null;
  }

  private static CommonInfraModel getModel(PsiElement contextElement) {
    return InfraModelService.of().getModel(contextElement);
  }
}
