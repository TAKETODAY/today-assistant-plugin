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
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;

import java.util.List;

import cn.taketoday.assistant.index.InfraXmlBeansIndex;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.utils.search.BeanSearchParameters;
import cn.taketoday.assistant.model.xml.CustomBean;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;

public abstract class CustomBeanWrappersQueryExecutor<Params extends BeanSearchParameters> extends QueryExecutorBase<BeanPointer<?>, Params> {
  protected abstract boolean accept(Params params, CustomBean customBean);

  protected abstract boolean acceptWrapper(Params params, CustomBeanWrapper customBeanWrapper);

  public final void processQuery(Params params, Processor<? super BeanPointer<?>> consumer) {
    CommonProcessors.CollectProcessor<CustomBeanWrapper> collectProcessor = new CommonProcessors.CollectProcessor<>();
    InfraXmlBeansIndex.processCustomBeans(params, collectProcessor);
    for (CustomBeanWrapper customBeanWrapper : collectProcessor.getResults()) {
      if (!processCustomBeanWrapper(params, consumer, customBeanWrapper)) {
        return;
      }
    }
  }

  private boolean processCustomBeanWrapper(Params params, Processor<? super BeanPointer<?>> consumer, CustomBeanWrapper wrapper) {
    List<CustomBean> customBeans = wrapper.getCustomBeans();
    for (CustomBean bean : customBeans) {
      if (accept(params, bean) && bean.isValid() && !consumer.process(InfraBeanService.of().createBeanPointer(bean))) {
        return false;
      }
    }
    return !acceptWrapper(params, wrapper) || consumer.process(InfraBeanService.of().createBeanPointer(wrapper));
  }

  public static class BeanClass extends CustomBeanWrappersQueryExecutor<BeanSearchParameters.BeanClass> {
    public static final BeanClass INSTANCE = new BeanClass();

    @Override
    public boolean accept(BeanSearchParameters.BeanClass parameters, CustomBean bean) {
      return parameters.matchesClass(bean.getBeanType());
    }

    @Override
    public boolean acceptWrapper(BeanSearchParameters.BeanClass aClass, CustomBeanWrapper bean) {
      return false;
    }
  }

  public static class AllWrappers extends CustomBeanWrappersQueryExecutor<BeanSearchParameters.BeanName> {
    public static final AllWrappers INSTANCE = new AllWrappers();

    @Override
    public boolean accept(BeanSearchParameters.BeanName name, CustomBean bean) {
      return false;
    }

    @Override
    public boolean acceptWrapper(BeanSearchParameters.BeanName name, CustomBeanWrapper bean) {
      return true;
    }
  }

  public static class BeanName extends CustomBeanWrappersQueryExecutor<BeanSearchParameters.BeanName> {
    public static final BeanName INSTANCE = new BeanName();

    @Override
    public boolean accept(BeanSearchParameters.BeanName parameters, CustomBean bean) {
      return parameters.getBeanName().equals(bean.getBeanName());
    }

    @Override
    public boolean acceptWrapper(BeanSearchParameters.BeanName parameters, CustomBeanWrapper bean) {
      return parameters.getBeanName().equals(bean.getBeanName());
    }
  }
}
