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

package cn.taketoday.assistant.app.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.project.Project;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraLibraryUtil;

public final class InfraApplicationConfigurationType implements ConfigurationType {

  private static final String ID = "InfraApplicationConfigurationType";

  public static InfraApplicationConfigurationType of() {
    return ConfigurationTypeUtil.findConfigurationType(InfraApplicationConfigurationType.class);
  }

  private final ConfigurationFactory myFactory = new ConfigurationFactory(this) {

    public RunConfiguration createTemplateConfiguration(Project project) {
      return new InfraApplicationRunConfiguration(project, this, "");
    }

    public boolean isApplicable(Project project) {
      return InfraLibraryUtil.hasFrameworkLibrary(project);
    }

    public String getId() {
      return "Infra";
    }

    public boolean isEditableInDumbMode() {
      return true;
    }

    public Class<? extends BaseState> getOptionsClass() {
      return InfraApplicationRunConfigurationOptions.class;
    }
  };

  public String getDisplayName() {
    return InfraRunBundle.message("infra.run.config.type.name");
  }

  public String getConfigurationTypeDescription() {
    return InfraRunBundle.message("infra.run.config.type.description");
  }

  public Icon getIcon() {
    return Icons.Today;
  }

  public String getId() {
    return ID;
  }

  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[] { this.myFactory };
  }

  public String getHelpTopic() {
    return "reference.dialogs.rundebug.InfraApplicationConfigurationType";
  }

  public ConfigurationFactory getDefaultConfigurationFactory() {
    return this.myFactory;
  }
}
