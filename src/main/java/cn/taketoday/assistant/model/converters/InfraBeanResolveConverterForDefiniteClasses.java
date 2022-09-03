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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.SmartList;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomJavaUtil;

import java.util.Collections;
import java.util.List;

/**
 * Do not use for "plain" references in {@link com.intellij.util.xml.Convert}, use {@link cn.taketoday.assistant.model.xml.RequiredBeanType} instead.
 */
public abstract class InfraBeanResolveConverterForDefiniteClasses extends InfraBeanResolveConverter {

  protected abstract String[] getClassNames(ConvertContext context);

  @Override

  public List<PsiClassType> getRequiredClasses(ConvertContext context) {
    String[] classNames = getClassNames(context);
    if (classNames == null || classNames.length == 0)
      return Collections.emptyList();

    List<PsiClassType> required = new SmartList<>();
    for (String className : classNames) {
      PsiClass psiClass = DomJavaUtil.findClass(className, context.getInvocationElement());
      if (psiClass != null) {
        required.add(PsiTypesUtil.getClassType(psiClass));
      }
    }
    return required;
  }
}
