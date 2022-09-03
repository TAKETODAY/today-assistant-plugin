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
package cn.taketoday.assistant.model.custom;

import com.intellij.openapi.module.Module;

import cn.taketoday.assistant.context.model.CacheableCommonInfraModel;
import cn.taketoday.lang.Nullable;

public abstract class CustomComponentDiscovererBeansModel extends CacheableCommonInfraModel {

  private final Module myModule;
  private final String myProviderName;

  public CustomComponentDiscovererBeansModel(@Nullable Module module, String providerName) {
    myModule = module;
    myProviderName = providerName;
  }

  @Override
  public String toString() {
    return myProviderName + ": " + super.toString();
  }

  @Override
  @Nullable
  public Module getModule() {
    return myModule;
  }

}
