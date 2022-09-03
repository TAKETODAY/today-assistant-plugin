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

package cn.taketoday.assistant.web.mvc.model;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;

import java.util.List;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.lang.Nullable;

public abstract class CodeConfigurationParserBase {
  protected final CommonInfraModel myServletModel;
  protected final Module myModule;
  private final BeanPointer<?> myConfigBeanPointer;

  protected abstract boolean parseConfigurationClass(PsiClass psiClass);

  protected CodeConfigurationParserBase(CommonInfraModel model, BeanPointer<?> pointer) {
    this.myServletModel = model;
    this.myModule = model.getModule();
    this.myConfigBeanPointer = pointer;
  }

  public final boolean collect() {
    PsiClass configClass = this.myConfigBeanPointer.getBeanClass();
    if (!isRelevantConfigClass(configClass)) {
      return false;
    }
    if (InfraMvcConstant.DELEGATING_WEB_MVC_CONFIGURATION.equals(configClass.getQualifiedName())) {
      return parseDelegatingWebMvcConfiguration();
    }
    return parseConfigurationClass(configClass);
  }

  private boolean parseDelegatingWebMvcConfiguration() {
    PsiClass webMvcConfigurerClass = InfraUtils.findLibraryClass(this.myModule, InfraMvcConstant.WEB_MVC_CONFIGURER);
    if (webMvcConfigurerClass == null) {
      return false;
    }
    ModelSearchParameters.BeanClass webMvcConfigurersSearchParams = ModelSearchParameters.byClass(webMvcConfigurerClass).withInheritors();
    List<BeanPointer<?>> configurers = InfraModelSearchers.findBeans(this.myServletModel, webMvcConfigurersSearchParams);
    for (BeanPointer configurer : configurers) {
      PsiClass configurerBeanClass = configurer.getBeanClass();
      if (isRelevantConfigClass(configurerBeanClass)) {
        parseConfigurationClass(configurerBeanClass);
      }
    }
    return true;
  }

  private static boolean isRelevantConfigClass(@Nullable PsiClass configClass) {
    return configClass != null && !configClass.hasModifierProperty("abstract");
  }
}
