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

package cn.taketoday.assistant.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.uast.UastModificationTracker;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.lang.Nullable;

public abstract class BaseBeanPointer<T extends CommonInfraBean> implements BeanPointer<T> {
  private final String myName;
  private final Project myProject;
  private CachedValue<PsiType[]> effectiveTypesValue;
  private final NotNullLazyValue<String[]> myAliases;

  public BaseBeanPointer(@Nullable String name, Project project) {
    this.myAliases = NotNullLazyValue.volatileLazy(() -> getBean().getAliases());
    this.myName = name;
    this.myProject = project;
  }

  @Override
  @Nullable
  public String getName() {
    return this.myName;
  }

  @Override
  public PsiType[] getEffectiveBeanTypes() {
    if (this.effectiveTypesValue == null) {
      this.effectiveTypesValue = CachedValuesManager.getManager(this.myProject).createCachedValue(() -> {
        T bean = getBean();
        PsiType[] types = InfraBeanService.of().getEffectiveBeanTypes(bean);
        return CachedValueProvider.Result.create(types, getDependencies(this.myProject));
      }, false);
    }
    return this.effectiveTypesValue.getValue();
  }

  private static Object[] getDependencies(Project project) {
    Object[] outerModelsDependencies = InfraModificationTrackersManager.from(project).getOuterModelsDependencies();
    List<Object> deps = new ArrayList<>(outerModelsDependencies.length + 1);
    ContainerUtil.addAll(deps, outerModelsDependencies);
    deps.add(UastModificationTracker.getInstance(project));
    return ArrayUtil.toObjectArray(deps);
  }

  @Override
  public String[] getAliases() {
    return this.myAliases.getValue();
  }

  @Override
  public BeanPointer<?> getBasePointer() {
    return this;
  }

  @Override
  public BeanPointer<?> derive(String name) {
    return Objects.equals(name, getName()) ? this : new DerivedBeanPointer(this, name);
  }

  @Override
  public boolean isReferenceTo(@Nullable CommonInfraBean springBean) {
    if (springBean == null) {
      return false;
    }
    PsiFile file = springBean.getContainingFile();
    return Objects.equals(file, getContainingFile()) && springBean.equals(getBean());
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  @Nullable
  public BeanPointer<?> getParentPointer() {
    return null;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BeanPointer)) {
      return false;
    }
    BeanPointer<?> that = (BeanPointer) o;
    return Objects.equals(this.myName, that.getName()) && Comparing.equal(getPsiElement(), that.getPsiElement());
  }

  public int hashCode() {
    if (this.myName != null) {
      return this.myName.hashCode();
    }
    return 0;
  }

  public String toString() {
    return getClass().getName() + "[" + this.myName + "]";
  }

  public Project getProject() {
    return this.myProject;
  }
}
