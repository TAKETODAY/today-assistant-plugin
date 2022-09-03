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

package cn.taketoday.assistant.model.xml.context.impl;

import com.intellij.openapi.module.Module;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.beans.stereotype.InfraJamModel;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.jam.stereotype.AbstractComponentScan;
import cn.taketoday.assistant.model.xml.context.ComponentScan;
import cn.taketoday.assistant.service.InfraJamService;

public abstract class ComponentScanImpl extends BeansPackagesScanBeanImpl implements ComponentScan {
  private final CachedValue<Map<Module, Set<CommonInfraBean>>> myScanned = CachedValuesManager.getManager(getXmlTag().getProject())
          .createCachedValue(
                  () -> CachedValueProvider.Result.create(ConcurrentFactoryMap.createMap(this::getScannedBeans),
                          AbstractComponentScan.getScannedElementsDependencies(getContainingFile())), false);

  protected ComponentScanImpl() {
  }

  @Override
  public String getProviderName() {
    return "Component";
  }

  @Override
  public Set<CommonInfraBean> getScannedElements(Module module) {
    if (module.isDisposed()) {
      return Collections.emptySet();
    }
    return (this.myScanned.getValue()).get(module);
  }

  private Set<CommonInfraBean> getScannedBeans(Module key) {
    return InfraJamService.of().filterComponentScannedStereotypes(key, this, InfraJamModel.from(key).getStereotypeComponents());
  }

  @Override
  public boolean useDefaultFilters() {
    Boolean value;
    GenericAttributeValue<Boolean> useDefaultFilters = getUseDefaultFilters();
    if (DomUtil.hasXml(useDefaultFilters) && (value = useDefaultFilters.getValue()) != null) {
      return value;
    }
    return true;
  }
}
