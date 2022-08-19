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

package cn.taketoday.assistant;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/19 15:49
 */
public interface Icons {
  Icon AbstractBean = load("icons/abstractBean.svg");
  Icon FactoryMethodBean = load("icons/factoryMethodBean.svg");
  Icon FileSet = load("icons/fileSet.svg");
  Icon ImplicitBean = load("icons/implicitBean.svg");
  Icon InfrastructureBean = load("icons/infrastructureBean.svg");
  Icon Listener = load("icons/listener.svg");
  Icon PrototypeBean = load("icons/prototypeBean.svg");
  Icon RequestMapping = load("icons/RequestMapping.svg");
  Icon ShowAutowiredDependencies = load("icons/showAutowiredDependencies.svg");
  Icon ShowCacheable = load("icons/showCacheable.svg");
  Icon Today = load("icons/today.svg");
  Icon Bean = load("icons/Bean.svg");
  Icon Config = load("icons/Config.svg");
  Icon JavaBean = load("icons/JavaBean.svg");
  Icon JavaConfig = load("icons/JavaConfig.svg");
  Icon Profile = load("icons/Profile.svg");
  Icon Property = load("icons/Property.svg");
  Icon ToolWindow = load("icons/ToolWindow.svg");
  Icon Web = load("icons/Web.svg");

  private static Icon load(String path) {
    return IconLoader.getIcon(path, Icons.class);
  }

  interface Gutter {
    Icon FactoryMethodBean = Icons.load("icons/gutter/factoryMethodBean.svg");
    Icon InfrastructureBean = Icons.load("icons/gutter/infrastructureBean.svg");
    Icon Listener = Icons.load("icons/gutter/listener.svg");
    Icon ParentBeanGutter = Icons.load("icons/gutter/parentBeanGutter.svg");
    Icon Publisher = Icons.load("icons/gutter/publisher.svg");
    Icon RequestMapping = Icons.load("icons/gutter/requestMapping.svg");
    Icon ShowAutowiredCandidates = Icons.load("icons/gutter/showAutowiredCandidates.svg");
    Icon ShowAutowiredDependencies = Icons.load("icons/gutter/showAutowiredDependencies.svg");
    Icon ShowCacheable = Icons.load("icons/gutter/showCacheable.svg");
    Icon Today = Icons.load("icons/gutter/today.svg");
    Icon Bean = Icons.load("icons/gutter/Bean.svg");
    Icon BeanMethod = Icons.load("icons/gutter/BeanMethod.svg");
    Icon Config = Icons.load("icons/gutter/Config.svg");
    Icon JavaBean = Icons.load("icons/gutter/JavaBean.svg");
    Icon Property = Icons.load("icons/gutter/Property.svg");
    Icon Scan = Icons.load("icons/gutter/Scan.svg");

  }
}
