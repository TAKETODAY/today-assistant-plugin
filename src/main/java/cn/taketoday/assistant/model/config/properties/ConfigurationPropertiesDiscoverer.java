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

package cn.taketoday.assistant.model.config.properties;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.util.SmartList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.InfraClassesConstants;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.context.model.LocalModel;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.custom.CustomLocalComponentsDiscoverer;
import cn.taketoday.assistant.model.jam.stereotype.CustomInfraComponent;

public class ConfigurationPropertiesDiscoverer extends CustomLocalComponentsDiscoverer {

  @Override
  public Collection<CommonInfraBean> getCustomComponents(LocalModel localModel) {
    Module module = localModel.getModule();
    if (module == null || module.isDisposed() || !(localModel instanceof LocalAnnotationModel localAnnotationModel)) {
      return Collections.emptyList();
    }
    else if (!InfraLibraryUtil.hasFrameworkLibrary(module)) {
      return Collections.emptyList();
    }
    else {
      PsiClass psiClass = localAnnotationModel.getConfig();
      EnableConfigurationProperties configurationProperties = EnableConfigurationProperties.CLASS_META.getJamElement(psiClass);
      if (configurationProperties == null) {
        return Collections.emptyList();
      }
      return findConfigurationProperties(module, configurationProperties);
    }
  }

  private static List<CommonInfraBean> findConfigurationProperties(Module module, EnableConfigurationProperties configurationProperties) {
    SmartList<CommonInfraBean> smartList = new SmartList<>();
    for (PsiClass configBeanClass : configurationProperties.getValue()) {
      ConfigurationProperties element = ConfigurationProperties.CLASS_META.getJamElement(configBeanClass);
      if (element != null) {
        smartList.add(createConfigurationPropertiesBean(element, configBeanClass));
      }
    }
    return smartList;
  }

  public static CommonInfraBean createConfigurationPropertiesBean(ConfigurationProperties configurationProperties, PsiClass configBeanClass) {
    return new CustomInfraComponent(InfraClassesConstants.CONFIGURATION_PROPERTIES, configBeanClass) {
      public String getBeanName() {
        String prefix = configurationProperties.getValueOrPrefix();
        boolean hasPrefix = StringUtil.isNotEmpty(prefix);
        String qualifiedName = configBeanClass.getQualifiedName();
        return hasPrefix ? prefix + "-" + qualifiedName : qualifiedName;
      }
    };
  }
}
