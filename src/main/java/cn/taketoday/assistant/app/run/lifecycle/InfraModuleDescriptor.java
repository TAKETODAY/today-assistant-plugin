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

import com.intellij.openapi.module.Module;
import com.intellij.util.ObjectUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraVersion;
import cn.taketoday.assistant.model.config.InfraConfigValueSearcher;
import cn.taketoday.lang.Nullable;

final class InfraModuleDescriptor {

  private static final String JMX_NAME_PROPERTY_KEY = "app.admin.jmx-name";
  private static final String APPLICATION_OBJECT_NAME = "cn.taketoday.app:type=Admin,name=InfraApplication";

  static final InfraModuleDescriptor DEFAULT_DESCRIPTOR = getDescriptor(null, true, APPLICATION_OBJECT_NAME);

  private final InfraVersion version;
  private final boolean actuatorsEnabled;
  private final String appAdminJmxName;

  private final Map<String, Boolean> endpointsAvailable;

  static InfraModuleDescriptor getDescriptor(@Nullable Module module, String activeProfiles) {
    return getDescriptor(
            module != null
            ? InfraLibraryUtil.getVersion(module)
            : null, InfraLibraryUtil.hasActuators(module), getAppAdminJmxName(module, activeProfiles));
  }

  static InfraModuleDescriptor getDescriptor(
          @Nullable InfraVersion version, boolean actuatorsEnabled, String applicationAdminJmxName) {
    return new InfraModuleDescriptor(version, actuatorsEnabled, applicationAdminJmxName);
  }

  private static String getAppAdminJmxName(@Nullable Module module, String activeProfiles) {
    if (module == null) {
      return APPLICATION_OBJECT_NAME;
    }
    Set<String> profilesSet = getProfilesSet(activeProfiles);
    var searcher = InfraConfigValueSearcher.productionForProfiles(module, JMX_NAME_PROPERTY_KEY, profilesSet);
    return ObjectUtils.chooseNotNull(searcher.findValueText(), APPLICATION_OBJECT_NAME);
  }

  @Nullable
  private static Set<String> getProfilesSet(String activeProfiles) {
    String[] profiles = activeProfiles != null ? activeProfiles.split(",") : null;
    var profilesSet = new HashSet<String>();
    if (profiles != null) {
      for (String profile : profiles) {
        profile = profile.trim();
        if (!profile.isEmpty()) {
          profilesSet.add(profile);
        }
      }
    }
    if (profilesSet.isEmpty()) {
      return null;
    }
    return profilesSet;
  }

  private InfraModuleDescriptor(@Nullable InfraVersion version,
          boolean actuatorsEnabled, String appAdminJmxName) {
    this.endpointsAvailable = new HashMap<>();
    this.version = version;
    this.actuatorsEnabled = actuatorsEnabled;
    this.appAdminJmxName = appAdminJmxName;
  }

  @Nullable
  InfraVersion getVersion() {
    return this.version;
  }

  boolean isActuatorsEnabled() {
    return this.actuatorsEnabled;
  }

  boolean isEndpointAvailable(Endpoint endpoint) {
    return this.endpointsAvailable.getOrDefault(endpoint.getId(), Boolean.TRUE);
  }

  void setEndpointAvailable(Endpoint endpoint, boolean available) {
    this.endpointsAvailable.put(endpoint.getId(), available);
  }

  String getAppAdminJmxName() {
    return this.appAdminJmxName;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof InfraModuleDescriptor descriptor)) {
      return false;
    }
    return version == descriptor.version
            && actuatorsEnabled == descriptor.actuatorsEnabled
            && appAdminJmxName.equals(descriptor.appAdminJmxName)
            && endpointsAvailable.equals(descriptor.endpointsAvailable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, actuatorsEnabled, appAdminJmxName);
  }

}
