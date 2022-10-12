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
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;

import java.util.Collections;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.xml.beans.RefBase;
import cn.taketoday.lang.Nullable;

public class PropertyLocalInfraBeanResolveConverterImpl extends PropertyLocalInfraBeanResolveConverter {

  @Override
  @Nullable
  public CommonInfraModel getInfraModel(ConvertContext context) {
    if (context.getModule() == null) {
      return null;
    }
    return LocalModelFactory.of().getOrCreateLocalXmlModel(context.getFile(), context.getModule(), Collections.emptySet());
  }

  public void bindReference(GenericDomValue<BeanPointer<?>> genericValue, ConvertContext context, PsiElement newTarget) {
    if (newTarget.getContainingFile() != context.getFile()) {
      RefBase ref = (RefBase) genericValue.getParent();
      ref.getBean().setStringValue(genericValue.getStringValue());
      genericValue.undefine();
    }
  }
}
