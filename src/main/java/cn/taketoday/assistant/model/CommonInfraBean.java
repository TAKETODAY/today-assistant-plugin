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
package cn.taketoday.assistant.model;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;

import java.util.Collection;

import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.context.model.CacheableCommonInfraModel;
import cn.taketoday.lang.Nullable;

@Presentation(provider = InfraPresentationProvider.class)
public interface CommonInfraBean extends CommonModelElement {

  @Nullable
  String getBeanName();

  /**
   * Return aliases defined in the bean definition, not including the bean name.
   *
   * @return bean aliases
   * @see CacheableCommonInfraModel#getAllBeanNames(BeanPointer)
   */
  String[] getAliases();

  @Nullable
  PsiType getBeanType(boolean considerFactories);

  @Nullable
  PsiType getBeanType();

  Collection<InfraQualifier> getInfraQualifiers();

  InfraProfile getProfile();

  boolean isPrimary();

  @Override
  PsiFile getContainingFile();
}
