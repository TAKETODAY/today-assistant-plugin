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

package cn.taketoday.assistant.impl;

import com.intellij.facet.FacetFinder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.uast.UastModificationTracker;

import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.lang.Nullable;
import kotlin.jvm.internal.Intrinsics;

public final class InfraEndpointsModificationTracker implements ModificationTracker {
  private final ModificationTracker uastModificationTracker;
  private final ModificationTracker facetModificationTracker;
  private final Project project;
  private final ModificationTracker outerModelsModificationTracker;

  public InfraEndpointsModificationTracker(Project project, ModificationTracker outerModelsModificationTracker) {
    this.project = project;
    this.outerModelsModificationTracker = outerModelsModificationTracker;
    this.uastModificationTracker = UastModificationTracker.Companion.getInstance(this.project);
    this.facetModificationTracker = FacetFinder.getInstance(this.project).getAllFacetsOfTypeModificationTracker(InfraFacet.FACET_TYPE_ID);
  }

  public long getModificationCount() {
    return this.outerModelsModificationTracker.getModificationCount() + this.facetModificationTracker.getModificationCount() + this.uastModificationTracker.getModificationCount();
  }

  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!Intrinsics.areEqual(getClass(), other != null ? other.getClass() : null)) {
      return false;
    }
    if (other == null) {
      throw new NullPointerException("null cannot be cast to non-null type cn.taketoday.assistant.impl.SpringEndpointsModificationTracker");
    }
    return Intrinsics.areEqual(this.project, ((InfraEndpointsModificationTracker) other).project);
  }

  public int hashCode() {
    return this.project.hashCode();
  }
}
