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

import com.intellij.util.ArrayUtilRt;

import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectInstance;

class InfraActuatorConnector extends InfraJmxConnector {
  static final String DEFAULT_DOMAIN = "cn.taketoday.app";
  private static final String ENDPOINT_OBJECT_NAME = "%s:type=Endpoint,name=%s,*";
  private static final String CONTEXT_KEY_PROPERTY = "context";
  private static final String DATA_ATTR = "Data";

  InfraActuatorConnector(String serviceUrl, String domain, String beanName) {
    super(serviceUrl, String.format(ENDPOINT_OBJECT_NAME, domain, beanName));
  }

  Object getData(String operationName) throws Exception {
    Set<ObjectInstance> objectInstances = getJmxConnection().queryMBeans(getObjectName(), null);
    ObjectInstance objectInstance = objectInstances.stream()
            .filter(o -> o.getObjectName().getKeyProperty(CONTEXT_KEY_PROPERTY) == null)
            .findFirst()
            .orElse(null);
    if (objectInstance == null) {
      throw new InstanceNotFoundException(getObjectName().toString());
    }
    if (operationName != null) {
      return getJmxConnection().invoke(objectInstance.getObjectName(), operationName, ArrayUtilRt.EMPTY_STRING_ARRAY, ArrayUtilRt.EMPTY_STRING_ARRAY);
    }
    return getJmxConnection().getAttribute(objectInstance.getObjectName(), DATA_ATTR);
  }
}
