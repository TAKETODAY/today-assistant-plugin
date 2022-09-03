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

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.SmartList;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.lang.Nullable;

public class UseExistingBeanQuickFixProvider implements BeanResolveQuickFixProvider {

  @Override

  public List<LocalQuickFix> getQuickFixes(ConvertContext context, Beans beans, @Nullable String beanId, List<PsiClassType> requiredClasses) {
    BeanPointer<?> beanByName;
    PsiClass beanClass;
    if (requiredClasses.isEmpty()) {
      return Collections.emptyList();
    }
    GenericDomValue<BeanPointer<?>> beanPointer = (GenericDomValue<BeanPointer<?>>) context.getInvocationElement();
    String parseBeanId = beanId != null ? beanId : beanPointer.getStringValue();
    if (StringUtil.isEmptyOrSpaces(parseBeanId)) {
      return Collections.emptyList();
    }
    PsiFile file = context.getFile();
    InfraManager infraManager = InfraManager.from(file.getProject());
    InfraModel currentFileModel = infraManager.getInfraModelByFile(file);
    if (currentFileModel == null || currentFileModel.getFileSet() == null) {
      return Collections.emptyList();
    }
    Module module = context.getModule();
    if (module == null) {
      return Collections.emptyList();
    }
    SmartList smartList = new SmartList();
    Set<InfraModel> infraModels = infraManager.getAllModels(module);
    for (InfraModel model : infraModels) {
      if (model.getFileSet() != null && (beanByName = InfraModelSearchers.findBean(model, parseBeanId)) != null && (beanClass = beanByName.getBeanClass()) != null) {
        Iterator<PsiClassType> it = requiredClasses.iterator();
        while (true) {
          if (it.hasNext()) {
            PsiClassType requiredClass = it.next();
            if (InheritanceUtil.isInheritorOrSelf(beanClass, requiredClass.resolve(), true)) {
              smartList.add(new UseExistingBeanFromOtherContextQuickFix(parseBeanId, beanByName.getContainingFile(), model.getFileSet()));
              break;
            }
          }
        }
      }
    }
    return smartList;
  }
}
