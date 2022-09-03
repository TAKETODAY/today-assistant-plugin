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

package cn.taketoday.assistant.model.utils.search;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.TypeConversionUtil;

import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.lang.Nullable;

public class BeanSearchParameters {

  private final Project myProject;
  private GlobalSearchScope myScope;
  @Nullable
  private VirtualFile myVirtualFile;

  public BeanSearchParameters(Project project) {
    this.myProject = project;
  }

  public Project getProject() {
    return this.myProject;
  }

  public GlobalSearchScope getSearchScope() {
    return this.myScope == null ? GlobalSearchScope.allScope(myProject) : this.myScope;
  }

  public void setSearchScope(GlobalSearchScope scope) {
    this.myScope = scope;
  }

  @Nullable
  public VirtualFile getVirtualFile() {
    return this.myVirtualFile;
  }

  public void setVirtualFile(@Nullable VirtualFile virtualFile) {
    this.myVirtualFile = virtualFile;
  }

  public static BeanClass byClass(Project project, ModelSearchParameters.BeanClass parameters) {
    return new BeanClass(project, parameters);
  }

  public static final class BeanClass extends BeanSearchParameters {

    private final ModelSearchParameters.BeanClass myParameters;

    private BeanClass(Project project, ModelSearchParameters.BeanClass parameters) {
      super(project);
      this.myParameters = parameters;
    }

    public boolean matchesClass(@Nullable PsiType psiType) {
      if (psiType == null || !psiType.isValid() || TypeConversionUtil.isNullType(psiType)) {
        return false;
      }
      return getPsiType().isAssignableFrom(psiType);
    }

    public PsiType getPsiType() {
      return this.myParameters.getSearchType();
    }

    public boolean isWithInheritors() {
      return this.myParameters.isWithInheritors();
    }

    public boolean isEffectiveBeanTypes() {
      return this.myParameters.isEffectiveBeanTypes();
    }
  }

  public static BeanName byName(Project project, String beanName) {
    return new BeanName(project, ModelSearchParameters.byName(beanName));
  }

  public static BeanName byName(Project project, ModelSearchParameters.BeanName parameters) {
    return new BeanName(project, parameters);
  }

  public static final class BeanName extends BeanSearchParameters {

    private final ModelSearchParameters.BeanName myParameters;

    private BeanName(Project project, ModelSearchParameters.BeanName parameters) {
      super(project);
      this.myParameters = parameters;
    }

    public String getBeanName() {
      return this.myParameters.getBeanName();
    }
  }
}
