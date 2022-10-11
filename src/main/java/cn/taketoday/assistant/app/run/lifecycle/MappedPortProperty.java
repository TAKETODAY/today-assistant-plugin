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

import com.intellij.execution.process.ProcessHandler;
import com.intellij.microservices.jvm.config.ConfigTunnelPortMapping;
import com.intellij.openapi.util.Disposer;

import java.util.function.Consumer;

class MappedPortProperty extends AbstractProperty<Integer> implements Property.PropertyListener {
  private final Property<Integer> remoteProperty;
  private final ConfigTunnelPortMapping portMapping;

  public static Property<Integer> withMappedPorts(Property<Integer> remoteProperty, ProcessHandler processHandler) {
    var portMapping = ConfigTunnelPortMapping.MAPPING_KEY.get(processHandler);
    return portMapping == null ? remoteProperty : new MappedPortProperty(remoteProperty, portMapping);
  }

  private MappedPortProperty(Property<Integer> remoteProperty, ConfigTunnelPortMapping portMapping) {
    super(null);
    this.remoteProperty = remoteProperty;
    this.portMapping = portMapping;
    this.remoteProperty.addPropertyListener(this);
    Disposer.register(remoteProperty, this);
  }

  @Override
  public void compute() {
    this.remoteProperty.compute();
  }

  public void dispose() {
    this.remoteProperty.removePropertyListener(this);
  }

  @Override
  public void computationFailed(Exception e) {
    dispatchComputationFailed(e);
  }

  @Override
  public void computationFinished() {
    dispatchComputationFinished();
  }

  @Override
  public void propertyChanged() {
    Integer remoteValue = remoteProperty.getValue();
    Integer mappedValue = remoteValue == null ? null : portMapping.getLocalPort(remoteValue);
    setValue(mappedValue);
    dispatchPropertyChanged();
  }

}
