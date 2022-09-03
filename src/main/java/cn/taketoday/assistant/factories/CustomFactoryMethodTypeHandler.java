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

package cn.taketoday.assistant.factories;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;

import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.lang.Nullable;

public abstract class CustomFactoryMethodTypeHandler {

  public static final ExtensionPointName<CustomFactoryMethodTypeHandler> EP_NAME =
          ExtensionPointName.create("cn.taketoday.assistant.factoryMethodTypeHandler");

  @Nullable
  public abstract PsiType getFactoryMethodType(PsiMethod psiMethod, @Nullable CommonInfraBean contextBean);
}
