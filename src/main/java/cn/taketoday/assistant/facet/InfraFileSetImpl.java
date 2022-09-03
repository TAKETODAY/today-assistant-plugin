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

package cn.taketoday.assistant.facet;

import org.jetbrains.annotations.ApiStatus;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;

@ApiStatus.Internal
public class InfraFileSetImpl extends InfraFileSet {
  private Icon myIcon;

  public InfraFileSetImpl(String id, String name, InfraFacet facet) {
    this(InfraFileSetData.create(id, name), facet);
  }

  public InfraFileSetImpl(InfraFileSetData data, InfraFacet facet) {
    super(data, facet);
    this.myIcon = Icons.FileSet;
  }

  public InfraFileSetImpl(InfraFileSet original) {
    super(original);
    this.myIcon = Icons.FileSet;
    this.myIcon = original.getIcon();
  }

  @Override
  public Icon getIcon() {
    return this.myIcon;
  }

  public String toString() {
    return getName();
  }

}
