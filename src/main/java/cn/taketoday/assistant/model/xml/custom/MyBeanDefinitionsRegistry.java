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

package cn.taketoday.assistant.model.xml.custom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceConsumer;
import cn.taketoday.core.io.ResourceLoader;

public class MyBeanDefinitionsRegistry extends StandardBeanFactory implements ResourceLoader, PatternResourceLoader {
  private final ArrayList<String> myResult = new ArrayList<>();
  private final ResourceLoader myDelegate = new DefaultResourceLoader(getClass().getClassLoader());
  private final PatternResourceLoader myResourcePatternResolver = new PathMatchingPatternResourceLoader(getClass().getClassLoader());

  public MyBeanDefinitionsRegistry() {
    this.myResult.add("no_infrastructures");
  }

  public List<String> getResult() {
    return this.myResult;
  }

  public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
    super.registerBeanDefinition(beanName, beanDefinition);
    if (beanDefinition.getRole() == 2) {
      this.myResult.set(0, "has_infrastructures");
      return;
    }
    ArrayList arrayList = new ArrayList();
    appendTag(arrayList, beanName, "beanName");
    appendTag(arrayList, beanDefinition.getBeanClassName(), "beanClassName");
    arrayList.add("constructorArgumentCount");
    arrayList.add(String.valueOf(beanDefinition.getConstructorArgumentValues().getArgumentCount()));
    if (beanDefinition instanceof AbstractBeanDefinition definition) {
      appendTag(arrayList, definition.getFactoryMethodName(), "factoryMethodName");
      appendTag(arrayList, definition.getFactoryBeanName(), "factoryBeanName");
    }
    Object source = beanDefinition.getSource();
    if (source instanceof int[] ints) {
      arrayList.add("path");
      StringBuilder path = new StringBuilder("x");
      for (int i = 0; i < ints.length; i++) {
        if (i > 0) {
          path.append(";");
        }
        path.append(ints[i]);
      }
      arrayList.add(path.toString());
    }
    this.myResult.addAll(arrayList);
  }

  private static void appendTag(List<String> info, String value, String tagName) {
    if (value == null) {
      return;
    }
    info.add(tagName);
    info.add(CustomBeanParser.encode(value));
  }

  public ClassLoader getClassLoader() {
    return getClass().getClassLoader();
  }

  public Resource getResource(String location) {
    return this.myDelegate.getResource(location);
  }

  public Set<Resource> getResources(String locationPattern) throws IOException {
    return this.myResourcePatternResolver.getResources(locationPattern);
  }

  @Override
  public void scan(String path, ResourceConsumer resourceConsumer) throws IOException {
    myResourcePatternResolver.scan(path, resourceConsumer);
  }

}
