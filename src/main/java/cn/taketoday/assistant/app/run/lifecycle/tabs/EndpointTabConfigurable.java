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
package cn.taketoday.assistant.app.run.lifecycle.tabs;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.ConfigurableBase;
import com.intellij.openapi.options.ConfigurableUi;

import cn.taketoday.lang.Nullable;

/**
 * Provides configuration for tabs in "Endpoints" run toolwindow.
 *
 * @param <UI> Must return {@link javax.swing.JPanel} from {@link ConfigurableUi#getComponent()}. Header with {@link #getDisplayName()} will
 * be added automatically.
 * @param <S> Settings.
 */
public abstract class EndpointTabConfigurable<UI extends ConfigurableUi<S>, S> extends ConfigurableBase<UI, S> {

  public static final ExtensionPointName<EndpointTabConfigurable> EP_NAME =
          ExtensionPointName.create("cn.taketoday.assistant.app.run.endpointTabConfigurable");

  protected EndpointTabConfigurable(String id, String displayName, @Nullable String helpTopic) {
    super(id, displayName, helpTopic);
  }
}
