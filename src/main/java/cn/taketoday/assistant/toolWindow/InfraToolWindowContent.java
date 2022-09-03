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
package cn.taketoday.assistant.toolWindow;

import com.intellij.BundleBase;
import com.intellij.DynamicBundle;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.serviceContainer.BaseKeyedLazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;

import java.util.ResourceBundle;

import cn.taketoday.lang.Nullable;

/**
 * Provides content tab for toolwindow.
 *
 * @author Yann C&eacute;bron
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public final class InfraToolWindowContent extends BaseKeyedLazyInstance<InfraToolWindowContentProvider> {
  private static final Logger LOG = Logger.getInstance(InfraToolWindowContent.class);

  public static final String TOOL_WINDOW_ID = "Infrastructure";
  public static final DataKey<InfraToolWindowContentUpdater> CONTENT_UPDATER = DataKey.create("infraToolWindowContentUpdater");

  @Attribute("displayName")
  public String displayName;

  @Attribute("icon")
  public String icon;

  @Attribute("bundle")
  public String bundle;

  @Attribute("implementation")
  public String implementation;

  public String getDisplayName() {
    String baseName = bundle != null ? bundle : getPluginDescriptor().getResourceBundleBaseName();
    if (baseName == null) {
      LOG.error("No resource bundle specified for " + getPluginDescriptor());
      return displayName;
    }

    ResourceBundle bundle = DynamicBundle.getResourceBundle(getLoaderForClass(), baseName);
    return BundleBase.messageOrDefault(bundle, displayName, null);
  }

  @Override
  @Nullable
  protected String getImplementationClassName() {
    return implementation;
  }
}
