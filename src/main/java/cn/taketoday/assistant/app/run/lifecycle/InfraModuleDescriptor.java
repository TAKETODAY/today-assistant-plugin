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
import java.util.Set;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraLibraryUtil.TodayVersion;
import cn.taketoday.assistant.model.config.InfraConfigValueSearcher;
import cn.taketoday.lang.Nullable;

final class InfraModuleDescriptor {

  private static final String JMX_NAME_PROPERTY_KEY = "application.admin.jmx-name";
  private static final String APPLICATION_OBJECT_NAME = "cn.taketoday.today-infrastructure:type=Admin,name=Application";
  static final InfraModuleDescriptor DEFAULT_DESCRIPTOR = getDescriptor(null, true, APPLICATION_OBJECT_NAME);
  private final TodayVersion myVersion;
  private final boolean myActuatorsEnabled;
  private final String myApplicationAdminJmxName;
  private final Map<String, Boolean> myEndpointsAvailable;

  static InfraModuleDescriptor getDescriptor(@Nullable Module module, String activeProfiles) {
    return getDescriptor(module != null ? InfraLibraryUtil.getVersion(module)
                                        : null, InfraLibraryUtil.hasActuators(module), getApplicationAdminJmxName(module, activeProfiles));
  }

  static InfraModuleDescriptor getDescriptor(@Nullable TodayVersion version, boolean actuatorsEnabled, String applicationAdminJmxName) {
    return new InfraModuleDescriptor(version, actuatorsEnabled, applicationAdminJmxName);
  }

  private static String getApplicationAdminJmxName(@Nullable Module module, String activeProfiles) {
    if (module == null) {
      return APPLICATION_OBJECT_NAME;
    }
    Set<String> profilesSet = getProfilesSet(activeProfiles);
    InfraConfigValueSearcher searcher = InfraConfigValueSearcher.productionForProfiles(module, JMX_NAME_PROPERTY_KEY, profilesSet);
    return ObjectUtils.chooseNotNull(searcher.findValueText(), APPLICATION_OBJECT_NAME);
  }

  @Nullable
  private static Set<String> getProfilesSet(String activeProfiles) {
    String[] profiles = activeProfiles != null ? activeProfiles.split(",") : null;
    Set<String> profilesSet = new HashSet<>();
    if (profiles != null) {
      for (String profile : profiles) {
        String profile2 = profile.trim();
        if (!profile2.isEmpty()) {
          profilesSet.add(profile2);
        }
      }
    }
    if (profilesSet.isEmpty()) {
      return null;
    }
    return profilesSet;
  }

  private InfraModuleDescriptor(@Nullable TodayVersion version, boolean actuatorsEnabled, String applicationAdminJmxName) {
    this.myEndpointsAvailable = new HashMap<>();
    this.myVersion = version;
    this.myActuatorsEnabled = actuatorsEnabled;
    this.myApplicationAdminJmxName = applicationAdminJmxName;
  }

  @Nullable
  TodayVersion getVersion() {
    return this.myVersion;
  }

  boolean isActuatorsEnabled() {
    return this.myActuatorsEnabled;
  }

  boolean isEndpointAvailable(Endpoint endpoint) {
    return this.myEndpointsAvailable.getOrDefault(endpoint.getId(), Boolean.TRUE).booleanValue();
  }

  void setEndpointAvailable(Endpoint endpoint, boolean available) {
    this.myEndpointsAvailable.put(endpoint.getId(), Boolean.valueOf(available));
  }

  String getApplicationAdminJmxName() {
    return this.myApplicationAdminJmxName;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof InfraModuleDescriptor descriptor)) {
      return false;
    }
    return this.myVersion == descriptor.myVersion && this.myActuatorsEnabled == descriptor.myActuatorsEnabled && this.myApplicationAdminJmxName.equals(
            descriptor.myApplicationAdminJmxName) && this.myEndpointsAvailable.equals(descriptor.myEndpointsAvailable);
  }

  public int hashCode() {
    int result = (31 * 17) + (this.myVersion != null ? this.myVersion.hashCode() : 0);
    return (31 * ((31 * ((31 * result) + (this.myActuatorsEnabled ? 1 : 0))) + this.myApplicationAdminJmxName.hashCode())) + this.myEndpointsAvailable.hashCode();
  }
}
