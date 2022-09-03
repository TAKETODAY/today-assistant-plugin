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
package cn.taketoday.assistant.app.application.metadata;

public interface InfraMetadataConstant {

  String PROPERTIES = "properties";
  String GROUPS = "groups";
  String HINTS = "hints";

  String NAME = "name";
  String SOURCE_TYPE = "sourceType";
  String TYPE = "type";
  String DESCRIPTION = "description";
  String DEFAULT_VALUE = "defaultValue";
  String DEPRECATED = "deprecated";
  String DEPRECATION = "deprecation";
  String REPLACEMENT = "replacement";
  String REASON = "reason";
  String LEVEL = "level";

  String SOURCE_METHOD = "sourceMethod";

  String VALUES = "values";
  String PROVIDERS = "providers";

  String VALUE = "value";
  String PARAMETERS = "parameters";
  String TARGET = "target";
  String CONCRETE = "concrete";
  String GROUP = "group";

  String INFRA_PROFILES_KEY = "context.profiles";
  String INFRA_CONFIG_ACTIVE_ON_PROFILE_KEY = "context.config.activate.on-profile";
}
