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

package cn.taketoday.assistant.app.run.lifecycle;

import com.intellij.openapi.Disposable;

import java.util.Objects;

abstract class AsyncApplicationLiveProperty<T> extends AsyncLiveProperty<T> {
  private final LiveProperty<InfraModuleDescriptor> myModuleDescriptor;
  private final LiveProperty<String> myServiceUrl;

  protected AsyncApplicationLiveProperty(
          LiveProperty<InfraModuleDescriptor> moduleDescriptor,
          LiveProperty<String> serviceUrl, LifecycleErrorHandler errorHandler, Disposable parent) {
    this(moduleDescriptor, serviceUrl, errorHandler, parent, null);
  }

  protected AsyncApplicationLiveProperty(
          LiveProperty<InfraModuleDescriptor> moduleDescriptor, LiveProperty<String> serviceUrl,
          LifecycleErrorHandler errorHandler, Disposable parent, T defaultValue) {
    super(errorHandler, parent, defaultValue);
    this.myModuleDescriptor = moduleDescriptor;
    this.myServiceUrl = serviceUrl;
  }

  protected InfraApplicationConnector getApplicationConnector() {
    return new InfraApplicationConnector(this.myServiceUrl.getValue(), getModuleDescriptor());
  }

  protected String getServiceUrl() {
    return this.myServiceUrl.getValue();
  }

  protected InfraModuleDescriptor getModuleDescriptor() {
    return Objects.requireNonNull(
            this.myModuleDescriptor.getValue());
  }
}
