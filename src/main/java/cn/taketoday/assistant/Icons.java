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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.IconManager;

import javax.swing.Icon;

/**
 * project icons
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/19 15:49
 */
public interface Icons {
  Icon Today = load("icons/today.svg");
  Icon TodayApp = load("icons/todayApp.svg");
  Icon WebMvc = load("icons/web-mvc.svg");
  Icon TodayOverlay = load("icons/SpringBoot_Overlay.svg", -1531170622, 0);

  Icon ParentContext = AllIcons.Actions.Forward;

  Icon WebOverlay = load("icons/mvc/WebOverlay.svg", 349113568, 0);

  Icon SpringBootEndpoint = load("icons/SpringBootEndpoint.svg", 1090109842, 0);

  Icon SpringBootHealth = load("icons/SpringBootHealth.svg", 1173159626, 0);

  private static Icon load(String path) {
    return IconLoader.getIcon(path, Icons.class);
  }

  private static Icon load(String path, int cacheKey, int flags) {
    return IconManager.getInstance().loadRasterizedIcon(path, Icons.class.getClassLoader(), cacheKey, flags);
  }

  /** 16x16 */
  Icon AbtractBean = load("icons/abtractBean.svg", -1569675660, 0);
  /** 16x16 */
  Icon FactoryMethodBean = load("icons/factoryMethodBean.svg", -2115933915, 0);
  /** 16x16 */
  Icon FileSet = load("icons/fileSet.svg", 1104081322, 0);

  interface Gutter {
    Icon Today = load("icons/gutter/today.svg");

    Icon LiveBean = load("icons/gutter/liveBean.svg", 1322096946, 2);

    /** 12x12 */
    Icon FactoryMethodBean = load("icons/gutter/factoryMethodBean.svg", 1828459504, 0);
    /** 12x12 */
    Icon InfrastructureBean = load("icons/gutter/infrastructureBean.svg", -1089479725, 0);
    /** 12x12 */
    Icon Listener = load("icons/gutter/listener.svg", 1264019015, 0);
    /** 12x12 */
    Icon ParentBeanGutter = load("icons/gutter/parentBeanGutter.svg", 2064196646, 2);
    /** 12x12 */
    Icon Publisher = load("icons/gutter/publisher.svg", -156122623, 0);
    /** 12x12 */
    Icon RequestMapping = load("icons/gutter/requestMapping.svg", 1848818862, 0);
    /** 12x12 */
    Icon ShowAutowiredCandidates = load("icons/gutter/showAutowiredCandidates.svg", -1406413767, 0);
    /** 12x12 */
    Icon ShowAutowiredDependencies = load("icons/gutter/showAutowiredDependencies.svg", -836623339, 0);
    /** 12x12 */
    Icon ShowCacheable = load("icons/gutter/showCacheable.svg", 1797904825, 0);
    /** 12x12 */
    Icon SpringBean = load("icons/gutter/springBean.svg", -419941764, 0);
    /** 12x12 */
    Icon SpringBeanMethod = load("icons/gutter/springBeanMethod.svg", 565249708, 0);
    /** 12x12 */
    Icon SpringConfig = load("icons/gutter/springConfig.svg", 243899357, 0);
    /** 12x12 */
    Icon SpringJavaBean = load("icons/gutter/springJavaBean.svg", -1750284083, 0);
    /** 12x12 */
    Icon SpringProperty = load("icons/gutter/springProperty.svg", -280350962, 0);
    /** 12x12 */
    Icon SpringScan = load("icons/gutter/springScan.svg", -91626889, 0);
  }

  /** 16x16 */
  Icon ImplicitBean = load("icons/implicitBean.svg", 48269941, 0);
  /** 16x16 */
  Icon InfrastructureBean = load("icons/infrastructureBean.svg", 600482799, 0);
  /** 16x16 */
  Icon Listener = load("icons/listener.svg", -662648050, 0);
  /** 16x16 */
  Icon PrototypeBean = load("icons/prototypeBean.svg", -556331492, 0);
  /** 16x16 */
  Icon RequestMapping = load("icons/RequestMapping.svg", 1981438654, 0);
  /** 16x16 */
  Icon ShowAutowiredDependencies = load("icons/showAutowiredDependencies.svg", -1569695384, 0);
  /** 16x16 */
  Icon ShowCacheable = load("icons/showCacheable.svg", -1852605178, 0);
  /** 16x16 */
  Icon SpringBean = load("icons/springBean.svg", 1732856675, 0);
  /** 16x16 */
  Icon SpringConfig = load("icons/springConfig.svg", 1717793444, 0);
  /** 16x16 */
  Icon SpringJavaBean = load("icons/springJavaBean.svg", 304558974, 0);
  /** 16x16 */
  Icon SpringJavaConfig = load("icons/springJavaConfig.svg", -1000593356, 0);
  /** 16x16 */
  Icon SpringProfile = load("icons/SpringProfile.svg", 995470584, 0);
  /** 16x16 */
  Icon SpringProperty = load("icons/springProperty.svg", -284198597, 0);
  /** 13x13 */
  Icon ToolWindowTab = load("icons/ToolWindowTab.svg");
  /** 16x16 */
  Icon SpringWeb = load("icons/SpringWeb.svg", 1145420881, 0);

}
