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

package cn.taketoday.assistant.app.run;

import com.intellij.ui.IconManager;

import javax.swing.Icon;

public final class InfraRunIcons {

  public static final Icon SpringBootEndpoint = load("icons/SpringBootEndpoint.svg", 1090109842, 0);

  public static final Icon SpringBootHealth = load("icons/SpringBootHealth.svg", 1173159626, 0);

  public static final class Gutter {

    public static final Icon LiveBean = load("icons/gutter/liveBean.svg", 1322096946, 2);
  }

  private static Icon load(String path, int cacheKey, int flags) {
    return IconManager.getInstance().loadRasterizedIcon(path, InfraRunIcons.class.getClassLoader(), cacheKey, flags);
  }
}
