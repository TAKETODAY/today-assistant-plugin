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

package cn.taketoday.assistant.model.utils.search.executors;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Processor;

import java.util.Collection;
import java.util.HashSet;

import cn.taketoday.assistant.index.InfraXmlBeansIndex;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.search.BeanSearchParameters;
import cn.taketoday.assistant.model.xml.beans.Alias;

public final class XmlBeanNameQueryExecutor extends QueryExecutorBase<BeanPointer<?>, BeanSearchParameters.BeanName> {
  public static final XmlBeanNameQueryExecutor INSTANCE = new XmlBeanNameQueryExecutor();

  public void processQuery(BeanSearchParameters.BeanName params, Processor<? super BeanPointer<?>> consumer) {
    processBeans(params, consumer, new HashSet<>(1));
  }

  private static boolean processBeans(BeanSearchParameters.BeanName params, Processor<? super BeanPointer<?>> processor, Collection<String> processed) {
    String beanName = params.getBeanName();
    if (processed.contains(beanName)) {
      return true;
    }
    processed.add(beanName);
    if (!InfraXmlBeansIndex.processBeansByName(params, processor)) {
      return false;
    }
    Processor<Alias> aliasProcessor = alias -> {
      String aliasedBeanName = alias.getAliasedBean().getStringValue();
      return !StringUtil.isNotEmpty(aliasedBeanName)
              || processBeans(copyFrom(params, aliasedBeanName), processor, processed);
    };
    return InfraXmlBeansIndex.processAliases(params, aliasProcessor);
  }

  private static BeanSearchParameters.BeanName copyFrom(BeanSearchParameters.BeanName params, String aliasedBeanName) {
    BeanSearchParameters.BeanName wrappedParams = BeanSearchParameters.byName(params.getProject(), aliasedBeanName);
    wrappedParams.setSearchScope(params.getSearchScope());
    wrappedParams.setVirtualFile(params.getVirtualFile());
    return wrappedParams;
  }
}
