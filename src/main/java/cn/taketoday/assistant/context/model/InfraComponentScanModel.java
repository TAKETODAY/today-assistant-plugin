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

package cn.taketoday.assistant.context.model;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.assistant.model.xml.context.InfraBeansPackagesScan;

public class InfraComponentScanModel<T extends InfraBeansPackagesScan> extends ComponentScanPackagesModel {
  private final Set<String> myActiveProfiles;
  private volatile CachedValue<Collection<BeanPointer<?>>> myScannedBeans;
  private final T myComponentScan;

  public InfraComponentScanModel(Module module, T componentScan, Set<String> activeProfiles) {
    super(NotNullLazyValue.lazy(componentScan::getPsiPackages), module);
    this.myComponentScan = componentScan;
    this.myActiveProfiles = activeProfiles;
  }

  @Override
  public Collection<BeanPointer<?>> getLocalBeans() {
    PsiElement psiElement = this.myComponentScan.getIdentifyingPsiElement();
    if (psiElement == null) {
      return Collections.emptySet();
    }
    if (this.myScannedBeans == null) {
      this.myScannedBeans = CachedValuesManager.getManager(getModule().getProject()).createCachedValue(() -> {
        Collection<BeanPointer<?>> pointers = calculateLocalBeans();
        return CachedValueProvider.Result.create(pointers,
                getDependencies(pointers.stream().map(BeanPointer::getContainingFile).collect(Collectors.toSet()))
        );
      }, false);
    }
    return this.myScannedBeans.getValue();
  }

  @Override
  protected Collection<BeanPointer<?>> calculateScannedBeans() {
    Set<CommonInfraBean> elements = this.myComponentScan.getScannedElements(getModule());
    List<CommonInfraBean> inActiveProfiles = ProfileUtils.filterBeansInActiveProfiles(elements, this.myActiveProfiles);
    return InfraBeanService.of().mapBeans(inActiveProfiles);
  }

  @Override
  public Set<String> getActiveProfiles() {
    return myActiveProfiles;
  }

}
