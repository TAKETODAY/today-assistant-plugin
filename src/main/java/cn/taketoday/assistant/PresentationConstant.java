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

package cn.taketoday.assistant;

public interface PresentationConstant {
  String CONFIGURATION = "@Configuration";
  String CONTROLLER = "@Controller";
  String SERVICE = "@Service";
  String COMPONENT = "@Component";
  String QUALIFIER = "@Qualifier";
  String ACTIVE_PROFILES = "@ActiveProfiles";
  String PROFILE = "@Profile";
  String DEPENDS_ON = "@DependsOn";
  String SPRING_BEAN = "Infra Bean";
  String SPRING_PROPERTY = "Infra Property";
  String SPRING_CONSTANT = "Infra Constant";
  String SPRING_PROFILE = "Infra Profile";
  String CACHE = "Cache";

  String NAMED = "@Named";
  String REPOSITORY = "@Repository";
  String COMPONENT_SCAN = "Component Scan";
  String TASK_EXECUTOR = "Infra Task Executor";
  String TASK_SCHEDULER = "Infra Task Scheduler";
  String SPRING_LIST = "Infra List";
  String SPRING_MAP = "Infra Map";
  String SPRING_SET = "Infra Set";
}
