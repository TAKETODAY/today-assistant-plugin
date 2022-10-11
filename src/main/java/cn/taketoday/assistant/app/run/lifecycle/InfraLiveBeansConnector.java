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

import com.intellij.util.containers.ContainerUtil;

import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectInstance;

class InfraLiveBeansConnector extends InfraJmxConnector {
  private static final String LIVE_BEANS_OBJECT_NAME = ":application=*";
  private static final String SNAPSHOT_ATTR = "SnapshotAsJson";

  InfraLiveBeansConnector(String serviceUrl) {
    super(serviceUrl, LIVE_BEANS_OBJECT_NAME);
  }

  String getSnapshot() throws Exception {
    Set<ObjectInstance> objectInstances = getJmxConnection().queryMBeans(getObjectName(), null);
    ObjectInstance objectInstance = ContainerUtil.getFirstItem(objectInstances);
    if (objectInstance == null) {
      throw new InstanceNotFoundException(getObjectName().toString());
    }
    return (String) getJmxConnection().getAttribute(objectInstance.getObjectName(), SNAPSHOT_ATTR);
  }
}
