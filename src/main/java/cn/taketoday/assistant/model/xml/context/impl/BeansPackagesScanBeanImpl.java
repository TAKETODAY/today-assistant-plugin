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

package cn.taketoday.assistant.model.xml.context.impl;

import com.intellij.psi.PsiPackage;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.GenericAttributeValue;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.model.jam.utils.filters.ContextExpressionFilterFactory;
import cn.taketoday.assistant.model.jam.utils.filters.InfraContextFilter;
import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.assistant.model.xml.DomInfraBeanImpl;
import cn.taketoday.assistant.model.xml.context.BeansPackagesScanBean;
import cn.taketoday.assistant.model.xml.context.InfraBeansPackagesScan;

public abstract class BeansPackagesScanBeanImpl extends DomInfraBeanImpl implements BeansPackagesScanBean, InfraBeansPackagesScan {

  @Override
  public Set<PsiPackage> getPsiPackages() {
    GenericAttributeValue<Collection<PsiPackage>> basePackage = getBasePackage();
    if (PlaceholderUtils.getInstance().isDefaultPlaceholder(basePackage.getRawText())) {
      return Collections.emptySet();
    }
    Collection<PsiPackage> packages = basePackage.getValue();
    return packages == null ? Collections.emptySet() : new LinkedHashSet<>(packages);
  }

  @Override
  public Set<InfraContextFilter.Exclude> getExcludeContextFilters() {
    return ContainerUtil.map2Set(getExcludeFilters(), ContextExpressionFilterFactory::createExcludeFilter);
  }

  @Override
  public Set<InfraContextFilter.Include> getIncludeContextFilters() {
    return ContainerUtil.map2Set(getIncludeFilters(), ContextExpressionFilterFactory::createIncludeFilter);
  }
}
