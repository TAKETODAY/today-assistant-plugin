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

package cn.taketoday.assistant.service;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.ui.LayeredIcon;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/19 15:48
 */
public class IconService {

  // FIXME

  private static final NotNullLazyValue<Icon> ICON = NotNullLazyValue.lazy(() ->
          new LayeredIcon(Icons.Today, AllIcons.Actions.New));
  private static final NotNullLazyValue<Icon> GUTTER_ICON = NotNullLazyValue.lazy(() ->
          new LayeredIcon(Icons.Gutter.Today, AllIcons.Actions.New));

  public static IconService getInstance() {
    return ApplicationManager.getApplication().getService(IconService.class);
  }

  public Icon getFileIcon() {
    return ICON.getValue();
  }

  public Icon getGutterIcon() {
    return GUTTER_ICON.getValue();
  }
}