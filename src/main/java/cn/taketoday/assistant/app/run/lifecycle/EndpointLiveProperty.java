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
import com.intellij.openapi.util.text.StringUtil;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBean;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBeansModel;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;

class EndpointLiveProperty<T> extends AsyncApplicationLiveProperty<T> {
  private static final String ENDPOINT_WILDCARD = "*";
  private final Endpoint<T> endpoint;
  private final LiveProperty<? extends LiveBeansModel> liveBeansModel;

  EndpointLiveProperty(
          Endpoint<T> endpoint, LiveProperty<InfraModuleDescriptor> moduleDescriptor,
          LiveProperty<String> serviceUrl, LifecycleErrorHandler errorHandler, Disposable parent, LiveProperty<? extends LiveBeansModel> liveBeansModel) {
    super(moduleDescriptor, serviceUrl, errorHandler, parent);
    this.endpoint = endpoint;
    this.liveBeansModel = liveBeansModel;
  }

  @Override
  public T doCompute() throws LifecycleException {
    InfraModuleDescriptor moduleDescriptor = getModuleDescriptor();
    if (!moduleDescriptor.isActuatorsEnabled() || !moduleDescriptor.isEndpointAvailable(this.endpoint)) {
      return null;
    }
    String endpointDomain = "cn.taketoday";
    String endpointDisabledMessage = null;
    try {
      InfraApplicationConnector connector = getApplicationConnector();
      boolean endpointsJmxEnabled = true;
      Set<String> exposed = parseEndpointsList(connector.getProperty("management.endpoints.jmx.exposure.include"));
      if (exposed.isEmpty() || exposed.contains(ENDPOINT_WILDCARD) || exposed.contains(this.endpoint.getId())) {
        Set<String> excluded = parseEndpointsList(connector.getProperty("management.endpoints.jmx.exposure.exclude"));
        if (!excluded.isEmpty() && (excluded.contains(ENDPOINT_WILDCARD) || excluded.contains(this.endpoint.getId()))) {
          endpointsJmxEnabled = false;
          endpointDisabledMessage = message("infra.application.endpoints.error.excluded", "management.endpoints.jmx.exposure.exclude");
        }
      }
      else {
        endpointsJmxEnabled = false;
        endpointDisabledMessage = message("infra.application.endpoints.error.not.exposed", "management.endpoints.jmx.exposure.include");
      }
      if (endpointsJmxEnabled) {
        String endpointsEnabledByDefaultProperty = InfraApplicationConnector.ENDPOINTS_ENABLED_PROPERTY.getKey();
        boolean endpointsEnabledByDefault = connector.getBooleanProperty(endpointsEnabledByDefaultProperty, true);
        String endpointEnabledProperty = String.format(InfraApplicationConnector.ENDPOINTS_ID_ENABLED_PROPERTY.getKey(), this.endpoint.getId());
        boolean endpointEnabled = connector.getBooleanProperty(endpointEnabledProperty, endpointsEnabledByDefault);
        if (!endpointEnabled) {
          String propertyName = endpointsEnabledByDefault ? endpointEnabledProperty : endpointsEnabledByDefaultProperty;
          endpointDisabledMessage = message("infra.application.endpoints.error.property.is.set.to.false", propertyName);
        }
        else {
          Object o = connector.getProperty(InfraApplicationConnector.ENDPOINTS_JMX_DOMAIN_PROPERTY);
          if (o instanceof String) {
            endpointDomain = (String) o;
          }
          else {
            Object o2 = connector.getProperty("jmx.default-domain");
            if (o2 instanceof String) {
              endpointDomain = (String) o2;
            }
          }
        }
      }
      if (connector != null) {
        connector.close();
      }
      if (endpointDisabledMessage == null) {
        LiveBeansModel model = this.liveBeansModel != null ? this.liveBeansModel.getValue() : null;
        if (model != null) {
          List<LiveBean> beans = model.getBeans();
          if (beans.stream().noneMatch(bean -> {
            return this.endpoint.getBeanName().equals(bean.getId());
          })) {
            endpointDisabledMessage = message("infra.application.endpoints.error.bean.not.initialized", this.endpoint.getBeanName());
          }
        }
      }
      String mbeanName = StringUtil.capitalize(this.endpoint.getId());
      InfraActuatorConnector actuatorConnector = new InfraActuatorConnector(getServiceUrl(), endpointDomain, mbeanName);
      try {
        try {
          T parseData = this.endpoint.parseData(actuatorConnector.getData(this.endpoint.getOperationName()));
          actuatorConnector.close();
          return parseData;
        }
        catch (Exception e) {
          if (endpointDisabledMessage != null) {
            throw new LifecycleException(null, new InfraApplicationConfigurationException(endpointDisabledMessage));
          }
          throw new LifecycleException(
                  message("infra.application.endpoints.error.failed.to.retrieve.endpoint.data", this.endpoint.getId(), e.getLocalizedMessage()), e);
        }
      }
      catch (Throwable th) {
        try {
          actuatorConnector.close();
        }
        catch (Throwable th2) {
          th.addSuppressed(th2);
        }
        throw th;
      }
    }
    catch (Exception e2) {
      throw new LifecycleException(message("infra.application.endpoints.error.failed.to.retrieve.endpoint.data", this.endpoint.getId(), e2.getLocalizedMessage()), e2);
    }
  }

  private static Set<String> parseEndpointsList(@Nullable Object value) {
    if (value == null) {
      return Collections.emptySet();
    }
    List<String> endpoints = StringUtil.split(value.toString(), ",");
    return endpoints.stream().map(String::trim).collect(Collectors.toSet());
  }
}
