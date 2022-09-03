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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CustomBeanInfo {
  public String beanName;
  public String beanClassName;
  public String factoryBeanName;
  public String factoryMethodName;

  public String idAttribute;
  public List<Integer> path;
  public int constructorArgumentCount;

  public CustomBeanInfo() {
    this.path = new ArrayList();
  }

  public CustomBeanInfo(CustomBeanInfo info) {
    this.beanName = info.beanName;
    this.beanClassName = info.beanClassName;
    this.factoryBeanName = info.factoryBeanName;
    this.factoryMethodName = info.factoryMethodName;
    this.idAttribute = info.idAttribute;
    this.path = info.path;
    this.constructorArgumentCount = info.constructorArgumentCount;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CustomBeanInfo info = (CustomBeanInfo) o;
    return this.constructorArgumentCount == info.constructorArgumentCount && Objects.equals(this.beanName, info.beanName) && Objects.equals(this.beanClassName, info.beanClassName) && Objects.equals(
            this.factoryBeanName, info.factoryBeanName) && Objects.equals(this.factoryMethodName, info.factoryMethodName) && Objects.equals(this.idAttribute, info.idAttribute) && Objects.equals(
            this.path, info.path);
  }

  public int hashCode() {
    return Objects.hash(this.beanName, this.beanClassName, this.factoryBeanName, this.factoryMethodName, this.idAttribute, this.path, this.constructorArgumentCount);
  }
}
