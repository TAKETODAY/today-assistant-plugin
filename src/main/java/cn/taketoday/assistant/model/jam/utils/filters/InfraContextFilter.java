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
package cn.taketoday.assistant.model.jam.utils.filters;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.lang.Nullable;

public abstract class InfraContextFilter {

  public abstract static class Exclude extends InfraContextFilter {

    public static final Exclude EMPTY_EXCLUDE = new Exclude() {
      @Override
      public boolean exclude(PsiClass psiClass) {
        return false;
      }
    };

    public abstract boolean exclude(PsiClass psiClass);
  }

  public abstract static class ExcludeExpression extends Exclude {
    @Nullable
    private final String myExpression;

    protected ExcludeExpression(@Nullable String expression) {
      myExpression = expression;
    }

    @Nullable
    public String getExpression() {
      return myExpression;
    }
  }

  public abstract static class ExcludeClasses extends Exclude {
    private final Collection<PsiClass> myClasses;

    protected ExcludeClasses(Collection<PsiClass> classes) {
      myClasses = classes;
    }

    public Collection<PsiClass> getClasses() {
      return myClasses;
    }
  }

  public abstract static class Include extends InfraContextFilter {
    public static final Include EMPTY_INCLUDE = new Include() {

      @Override
      public Set<InfraStereotypeElement> includeStereotypes(Module module, Set<PsiPackage> packages) {
        return Collections.emptySet();
      }
    };

    public abstract Set<InfraStereotypeElement> includeStereotypes(Module module, Set<PsiPackage> packages);
  }

  public abstract static class IncludeExpression extends Include {
    @Nullable
    private final String myExpression;

    protected IncludeExpression(@Nullable String expression) {
      myExpression = expression;
    }

    @Nullable
    public String getExpression() {
      return myExpression;
    }
  }

  public abstract static class IncludeClasses extends Include {
    private final Collection<PsiClass> myClasses;

    protected IncludeClasses(Collection<PsiClass> classes) {
      myClasses = classes;
    }

    public Collection<PsiClass> getClasses() {
      return myClasses;
    }
  }
}
