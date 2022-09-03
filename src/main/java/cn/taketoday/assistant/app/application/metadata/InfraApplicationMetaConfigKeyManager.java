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

import com.intellij.microservices.jvm.config.MetaConfigKeyManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Key;

public abstract class InfraApplicationMetaConfigKeyManager extends MetaConfigKeyManager {
  /**
   * Set the flag to {@code true} for Psi file in order to mark its content as acceptable by config key documentation provider.
   */
  public static final Key<Boolean> ELEMENT_IN_EXTERNAL_CONTEXT = Key.create("ElementInExternalContext");

  public static InfraApplicationMetaConfigKeyManager getInstance() {
    return ApplicationManager.getApplication().getService(InfraApplicationMetaConfigKeyManager.class);
  }
}
