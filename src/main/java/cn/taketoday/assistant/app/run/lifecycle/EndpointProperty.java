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
import static cn.taketoday.assistant.app.run.lifecycle.InfraApplicationConnector.ENDPOINTS_ENABLED_PROPERTY;
import static cn.taketoday.assistant.app.run.lifecycle.InfraApplicationConnector.ENDPOINTS_ID_ENABLED_PROPERTY;
import static cn.taketoday.assistant.app.run.lifecycle.InfraApplicationConnector.ENDPOINTS_JMX_DOMAIN_PROPERTY;
import static cn.taketoday.assistant.app.run.lifecycle.InfraApplicationConnector.ENDPOINTS_JMX_EXPOSURE_EXCLUDE_PROPERTY_2X;
import static cn.taketoday.assistant.app.run.lifecycle.InfraApplicationConnector.ENDPOINTS_JMX_EXPOSURE_INCLUDE_PROPERTY_2X;
import static cn.taketoday.assistant.app.run.lifecycle.InfraApplicationConnector.JMX_DEFAULT_DOMAIN_PROPERTY;

class EndpointProperty<T> extends AsyncApplicationProperty<T> {
  private static final String ENDPOINT_WILDCARD = "*";
  private final Endpoint<T> endpoint;
  private final Property<? extends LiveBeansModel> liveBeansModel;

  EndpointProperty(
          Endpoint<T> endpoint, Property<InfraModuleDescriptor> moduleDescriptor,
          Property<String> serviceUrl, LifecycleErrorHandler errorHandler,
          Disposable parent, Property<? extends LiveBeansModel> liveBeansModel) {
    super(moduleDescriptor, serviceUrl, errorHandler, parent);
    this.endpoint = endpoint;
    this.liveBeansModel = liveBeansModel;
  }

  @Override
  public T doCompute() throws LifecycleException {
    InfraModuleDescriptor moduleDescriptor = getModuleDescriptor();
    if (!moduleDescriptor.isActuatorsEnabled() || !moduleDescriptor.isEndpointAvailable(endpoint)) {
      return null;
    }
    String endpointDomain = InfraActuatorConnector.DEFAULT_DOMAIN;
    String endpointDisabledMessage = null;
    try {
      InfraApplicationConnector connector = getApplicationConnector();
      boolean endpointsJmxEnabled = true;

      Set<String> exposed = parseEndpointsList(connector.getProperty(ENDPOINTS_JMX_EXPOSURE_INCLUDE_PROPERTY_2X));
      if (exposed.isEmpty() || exposed.contains(ENDPOINT_WILDCARD) || exposed.contains(endpoint.getId())) {
        Set<String> excluded = parseEndpointsList(connector.getProperty(ENDPOINTS_JMX_EXPOSURE_EXCLUDE_PROPERTY_2X));
        if (!excluded.isEmpty() && (excluded.contains(ENDPOINT_WILDCARD) || excluded.contains(endpoint.getId()))) {
          endpointsJmxEnabled = false;
          endpointDisabledMessage = message(
                  "infra.application.endpoints.error.excluded", ENDPOINTS_JMX_EXPOSURE_EXCLUDE_PROPERTY_2X);
        }
      }
      else {
        endpointsJmxEnabled = false;
        endpointDisabledMessage = message(
                "infra.application.endpoints.error.not.exposed", ENDPOINTS_JMX_EXPOSURE_INCLUDE_PROPERTY_2X);
      }
      if (endpointsJmxEnabled) {
        String endpointsEnabledByDefaultProperty = ENDPOINTS_ENABLED_PROPERTY.getKey();
        boolean endpointsEnabledByDefault = connector.getBooleanProperty(endpointsEnabledByDefaultProperty, true);
        String endpointEnabledProperty = String.format(ENDPOINTS_ID_ENABLED_PROPERTY.getKey(), endpoint.getId());
        boolean endpointEnabled = connector.getBooleanProperty(endpointEnabledProperty, endpointsEnabledByDefault);
        if (!endpointEnabled) {
          String propertyName = endpointsEnabledByDefault ? endpointEnabledProperty : endpointsEnabledByDefaultProperty;
          endpointDisabledMessage = message(
                  "infra.application.endpoints.error.property.is.set.to.false", propertyName);
        }
        else {
          Object o = connector.getProperty(ENDPOINTS_JMX_DOMAIN_PROPERTY);
          if (o instanceof String) {
            endpointDomain = (String) o;
          }
          else {
            Object o2 = connector.getProperty(JMX_DEFAULT_DOMAIN_PROPERTY);
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
        LiveBeansModel model = liveBeansModel != null ? liveBeansModel.getValue() : null;
        if (model != null) {
          List<LiveBean> beans = model.getBeans();
          if (beans.stream().noneMatch(bean -> endpoint.getBeanName().equals(bean.getId()))) {
            endpointDisabledMessage = message(
                    "infra.application.endpoints.error.bean.not.initialized", endpoint.getBeanName());
          }
        }
      }
      String mbeanName = StringUtil.capitalize(endpoint.getId());
      InfraActuatorConnector actuatorConnector = new InfraActuatorConnector(getServiceUrl(), endpointDomain, mbeanName);
      try {
        try {
          T parseData = endpoint.parseData(actuatorConnector.getData(endpoint.getOperationName()));
          actuatorConnector.close();
          return parseData;
        }
        catch (Exception e) {
          if (endpointDisabledMessage != null) {
            throw new LifecycleException(null, new InfraApplicationConfigurationException(endpointDisabledMessage));
          }
          throw new LifecycleException(
                  message("infra.application.endpoints.error.failed.to.retrieve.endpoint.data", endpoint.getId(), e.getLocalizedMessage()), e);
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
      throw new LifecycleException(message(
              "infra.application.endpoints.error.failed.to.retrieve.endpoint.data",
              endpoint.getId(), e2.getLocalizedMessage()), e2);
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
