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

package cn.taketoday.assistant.beans;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.spring.boot.model.properties.jam.ConfigurationProperties;
import com.intellij.spring.boot.model.properties.jam.EnableConfigurationProperties;
import com.intellij.spring.contexts.model.LocalAnnotationModel;
import com.intellij.spring.contexts.model.LocalModel;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.custom.CustomLocalComponentsDiscoverer;
import com.intellij.spring.model.jam.stereotype.CustomSpringComponent;
import com.intellij.util.SmartList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.InfraLibraryUtil;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 15:20
 */
public class ConfigurationPropertiesDiscoverer extends CustomLocalComponentsDiscoverer {

  @Override
  public Collection<CommonSpringBean> getCustomComponents(LocalModel localModel) {
    System.out.println("getCustomComponents");
    Module module = localModel.getModule();
    if (module != null && !module.isDisposed() && localModel instanceof LocalAnnotationModel) {
      if (!InfraLibraryUtil.hasLibrary(module)) {
        return Collections.emptyList();
      }
      else {
        PsiClass psiClass = ((LocalAnnotationModel) localModel).getConfig();
        EnableConfigurationProperties enableConfigurationProperties = EnableConfigurationProperties.CLASS_META.getJamElement(psiClass);
        if (enableConfigurationProperties == null) {
          return Collections.emptyList();
        }
        else {
          return findConfigurationProperties(module, enableConfigurationProperties);
        }
      }
    }
    else {
      return Collections.emptyList();
    }
  }

  private static List<CommonSpringBean> findConfigurationProperties(
          Module module, EnableConfigurationProperties enableConfigurationProperties) {
    List<CommonSpringBean> beans = new SmartList<>();
    for (PsiClass configBeanClass : enableConfigurationProperties.getValue()) {
      ConfigurationProperties configurationProperties = ConfigurationProperties.CLASS_META.getJamElement(configBeanClass);
      if (configurationProperties != null) {
        beans.add(createConfigurationPropertiesBean(configurationProperties, configBeanClass));
      }
    }

    return beans;
  }

  public static CommonSpringBean createConfigurationPropertiesBean(
          ConfigurationProperties configurationProperties, PsiClass configBeanClass) {
    return new CustomSpringComponent("cn.taketoday.context.properties.ConfigurationProperties", configBeanClass) {
      public String getBeanName() {
        String prefix = configurationProperties.getValueOrPrefix();
        boolean hasPrefix = StringUtil.isNotEmpty(prefix);
        String qualifiedName = configBeanClass.getQualifiedName();
        return hasPrefix ? prefix + "-" + qualifiedName : qualifiedName;
      }
    };
  }
}
