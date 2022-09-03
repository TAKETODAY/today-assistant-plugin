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

package cn.taketoday.assistant.gutter;

import com.intellij.icons.AllIcons;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.scale.JBUIScale;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/27 13:21
 */
public interface InfraGroupLineMarker {

  default Icon getActionGroupIcon() {
    LayeredIcon icon = JBUIScale.scaleIcon(new LayeredIcon(2));
    icon.setIcon(Icons.Gutter.SpringJavaBean, 0, 0, 0);
    icon.setIcon(AllIcons.General.DropdownGutter, 1, 0, 0);
    return icon;
  }
}
