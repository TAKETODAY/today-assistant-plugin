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
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;
import com.intellij.util.Function;
import com.intellij.xml.util.PsiElementPointer;

import java.util.Comparator;

import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

/**
 * Lightweight reference to Infra bean.
 *
 * @author peter
 * @see InfraBeanService#createBeanPointer(CommonInfraBean)
 */
@Presentation(provider = InfraPresentationProvider.class)
public interface BeanPointer<T extends CommonInfraBean> extends PsiElementPointer {

  Function<BeanPointer<?>, CommonInfraBean> TO_BEAN = BeanPointer::getBean;

  /**
   * Sort beans for UI purposes.
   */
  Comparator<BeanPointer<?>> DISPLAY_COMPARATOR =
          (o1, o2) -> Comparing.compare(InfraPresentationProvider.getBeanName(o1), InfraPresentationProvider.getBeanName(o2));

  /**
   * Defined bean name.
   * <p/>
   * For presentation purposes use {@link InfraPresentationProvider#getBeanName(BeanPointer)}.
   *
   * @return {@code null} if no name could be determined.
   */
  @Nullable
  String getName();

  String[] getAliases();

  T getBean();

  boolean isValid();

  boolean isReferenceTo(@Nullable CommonInfraBean infraBean);

  BeanPointer<?> derive(String name);

  /**
   * @return {@code this} or underlying pointer (e.g. for derived pointers).
   */
  BeanPointer<?> getBasePointer();

  @Nullable
  PsiClass getBeanClass();

  PsiType[] getEffectiveBeanTypes();

  PsiFile getContainingFile();

  /**
   * Only for {@link InfraBean}.
   *
   * @return {@code true} if bean is abstract.
   */
  boolean isAbstract();

  /**
   * Only for {@link InfraBean}.
   *
   * @return Pointer to parent bean if defined.
   */
  @Nullable
  BeanPointer<?> getParentPointer();
}