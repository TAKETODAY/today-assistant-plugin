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
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

import cn.taketoday.assistant.app.run.InfraApplicationRunConfig;
import cn.taketoday.assistant.app.run.lifecycle.tabs.EndpointTab;
import cn.taketoday.lang.Nullable;

/**
 * Enables retrieving and storing Spring Boot actuator endpoint data in {@link InfraApplicationInfo}
 * and provides a sub-tab for 'Endpoints' tab in Run/Debug/Run Dashboard tool window.
 *
 * @param <T> the type used to represent endpoint data
 */
public abstract class Endpoint<T> {
  public static final ExtensionPointName<Endpoint<?>> EP_NAME =
          ExtensionPointName.create("cn.taketoday.assistant.app.run.endpoint");

  private static final String ENDPOINT_BEAN_NAME_SUFFIX = "Endpoint";

  private final String id;
  private final String beanName;
  private final String operationName;

  public Endpoint(String id) {
    this(id, id + ENDPOINT_BEAN_NAME_SUFFIX);
  }

  public Endpoint(String id, String beanName) {
    this(id, beanName, id);
  }

  public Endpoint(String id, String beanName, String operationName) {
    this.id = id;
    this.beanName = beanName;
    this.operationName = operationName;
  }

  /**
   * @return the id of the endpoint
   */
  public final String getId() {
    return id;
  }

  /**
   * @return the name of the endpoint bean
   */
  public final String getBeanName() {
    return beanName;
  }

  /**
   * @return the name of the operation invoked on an endpoint MBean to retrieve data (used for Spring Boot 2.x)
   */
  public String getOperationName() {
    return operationName;
  }

  /**
   * Parses data object received from an endpoint MBean.
   */
  @Nullable
  public abstract T parseData(@Nullable Object data);

  /**
   * Creates an endpoint sub-tab for Run/Debug/Run Dashboard tool window.
   */
  @Nullable
  public EndpointTab<T> createEndpointTab(
          InfraApplicationRunConfig runConfiguration, ProcessHandler processHandler) {
    return null;
  }

  /**
   * The method is invoked when {@link InfraApplicationInfo} instance is created for running Spring Boot application.
   */
  public void infoCreated(Project project, ProcessHandler processHandler, InfraApplicationInfo info) {
    
  }

  /**
   * Checks that the endpoint is available for the given module. The method is invoked in a read action in smart mode.
   *
   * @param module the module to check
   * @return {@code true} if the endpoint is available for the given module, otherwise {@code false}
   */
  public boolean isAvailable(Module module) {
    return true;
  }
}
