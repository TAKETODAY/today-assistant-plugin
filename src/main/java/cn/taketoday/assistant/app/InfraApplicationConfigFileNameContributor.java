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

package cn.taketoday.assistant.app;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.beans.CustomSetting;

import static cn.taketoday.assistant.InfraBundle.message;

public class InfraApplicationConfigFileNameContributor extends InfraModelConfigFileNameContributor {
  public static final Key<CustomSetting.STRING> CONFIG_NAME_CUSTOM_SETTING_ID = Key.create("infra_app_infra__config_name");
  public static final Key<CustomSetting.STRING> CONFIG_CUSTOM_FILES_ID = Key.create("infra_app_infra_config_custom_files");
  private static final String CONFIG_NAME_DEFAULT = "application";
  private static final String CONFIG_FILE_KEY = "context.config.name";

  InfraApplicationConfigFileNameContributor() {
    super(new InfraModelConfigFileNameContributor.CustomSettingDescriptor(CONFIG_NAME_CUSTOM_SETTING_ID,
                    message("app.config.files.name.setting", CONFIG_FILE_KEY), CONFIG_NAME_DEFAULT),
            new InfraModelConfigFileNameContributor.CustomSettingDescriptor(CONFIG_CUSTOM_FILES_ID,
                    message("app.custom.config.files.locations"), ""),
            new InfraModelConfigFileNameContributor.CustomizationPresentation(CONFIG_FILE_KEY,
                    message("app.config.files.section.title")), Icons.TodayApp);
  }

  public boolean accept(InfraFileSet fileSet) {
    return fileSet.isAutodetected() && fileSet.getId().startsWith("infra_app_");
  }

  public boolean accept(Module module) {
    return true;
  }

}
