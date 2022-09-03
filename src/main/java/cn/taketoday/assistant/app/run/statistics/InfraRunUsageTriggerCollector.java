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

package cn.taketoday.assistant.app.run.statistics;

import com.intellij.internal.statistic.eventLog.EventLogGroup;
import com.intellij.internal.statistic.eventLog.events.ClassEventField;
import com.intellij.internal.statistic.eventLog.events.EventFields;
import com.intellij.internal.statistic.eventLog.events.EventId;
import com.intellij.internal.statistic.eventLog.events.EventId1;
import com.intellij.internal.statistic.eventLog.events.EventId2;
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.openapi.project.Project;

public final class InfraRunUsageTriggerCollector extends CounterUsagesCollector {
  private static final EventLogGroup SPRING_BOOT_RUN_USAGES_GROUP = new EventLogGroup("infra.run.usages", 4);
  private static final EventId1<String> EDIT_RUNTIME_BEAN = SPRING_BOOT_RUN_USAGES_GROUP.registerEvent("edit.runtime.bean", EventFields.ActionPlace);
  private static final EventId1<String> EDIT_RUNTIME_RESOURCE = SPRING_BOOT_RUN_USAGES_GROUP.registerEvent("edit.runtime.resource", EventFields.ActionPlace);
  private static final EventId1<String> RUNTIME_BEANS_NAVIGATION_HANDLER = SPRING_BOOT_RUN_USAGES_GROUP.registerEvent("runtime.beans.navigation.handler", EventFields.ActionPlace);
  private static final EventId RUNTIME_BEAN_SELECTED = SPRING_BOOT_RUN_USAGES_GROUP.registerEvent("runtime.bean.selected");
  private static final EventId RUNTIME_RESOURCE_SELECTED = SPRING_BOOT_RUN_USAGES_GROUP.registerEvent("runtime.resource.selected");
  private static final ClassEventField ACTUATOR_FIELD = EventFields.Class("actuator");
  private static final EventId2<Class<?>, PluginInfo> ACTUATOR_TAB_SELECTED = SPRING_BOOT_RUN_USAGES_GROUP.registerEvent("actuator.tab.selected", ACTUATOR_FIELD, EventFields.PluginInfo);

  public static void logEditRuntimeBean(Project project, String place) {
    EDIT_RUNTIME_BEAN.log(project, place);
  }

  public static void logEditRuntimeResource(Project project, String place) {
    EDIT_RUNTIME_RESOURCE.log(project, place);
  }

  public static void logRuntimeBeansNavigationHandler(Project project, String place) {
    RUNTIME_BEANS_NAVIGATION_HANDLER.log(project, place);
  }

  public static void logRuntimeBeanSelected(Project project) {
    RUNTIME_BEAN_SELECTED.log(project);
  }

  public static void logRuntimeResourceSelected(Project project) {
    RUNTIME_RESOURCE_SELECTED.log(project);
  }

  public static void logActuatorTabSelected(Project project, Class<?> endpointClass) {
    ACTUATOR_TAB_SELECTED.log(project, endpointClass, PluginInfoDetectorKt.getPluginInfo(endpointClass));
  }

  public EventLogGroup getGroup() {
    return SPRING_BOOT_RUN_USAGES_GROUP;
  }
}
