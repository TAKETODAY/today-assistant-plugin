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

package cn.taketoday.assistant;

import com.intellij.facet.FacetFinder;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.ArrayUtil;

import java.util.ArrayList;

import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.impl.InfraEndpointsModificationTracker;
import cn.taketoday.assistant.impl.InfraKtOutOfCodeBlockModificationTracker;
import cn.taketoday.assistant.impl.InfraOuterModelsModificationTracker;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/25 12:29
 */
public class InfraModificationTrackersManager implements Disposable {

  private final Project myProject;
  private final SimpleModificationTracker myProfilesModificationTracker;
  private final SimpleModificationTracker myMultipleContextsModificationTracker;
  private final SimpleModificationTracker myCustomBeanParserModificationTracker;
  private final ModificationTracker myOuterModelsModificationTracker;
  @Nullable
  private final InfraKtOutOfCodeBlockModificationTracker myKtModificationTracker;
  private final InfraEndpointsModificationTracker myEndpointsModificationTracker;

  public static InfraModificationTrackersManager from(Project project) {
    return project.getService(InfraModificationTrackersManager.class);
  }

  public InfraModificationTrackersManager(Project project) {
    this.myProfilesModificationTracker = new SimpleModificationTracker();
    this.myMultipleContextsModificationTracker = new SimpleModificationTracker();
    this.myCustomBeanParserModificationTracker = new SimpleModificationTracker();
    this.myProject = project;
    if (Registry.is("infra.use.uast.modification.tracker")) {
      this.myOuterModelsModificationTracker = new InfraOuterModelsModificationTracker(project, this, true);
      this.myKtModificationTracker = null;
    }
    else {
      this.myOuterModelsModificationTracker = new InfraOuterModelsModificationTracker(project, this, false);
      this.myKtModificationTracker = new InfraKtOutOfCodeBlockModificationTracker(project, this);
    }

    this.myEndpointsModificationTracker = new InfraEndpointsModificationTracker(this.myProject, this.myOuterModelsModificationTracker);
  }

  public ModificationTracker getProfilesModificationTracker() {
    return this.myProfilesModificationTracker;
  }

  public ModificationTracker getMultipleContextsModificationTracker() {
    return this.myMultipleContextsModificationTracker;
  }

  public ModificationTracker getOuterModelsModificationTracker() {
    return this.myOuterModelsModificationTracker;
  }

  public ModificationTracker getCustomBeanParserModificationTracker() {
    return this.myCustomBeanParserModificationTracker;
  }

  public ModificationTracker getEndpointsModificationTracker() {
    return this.myEndpointsModificationTracker;
  }

  public void fireActiveProfilesChanged() {
    this.myProfilesModificationTracker.incModificationCount();
  }

  public void fireMultipleContextsChanged() {
    this.myMultipleContextsModificationTracker.incModificationCount();
  }

  public void fireCustomBeanParserChanged() {
    this.myCustomBeanParserModificationTracker.incModificationCount();
  }

  public Object[] getOuterModelsDependencies() {
    var dependencies = new ArrayList<>();
    dependencies.add(this.myOuterModelsModificationTracker);
    if (this.myKtModificationTracker != null) {
      dependencies.add(this.myKtModificationTracker);
    }

    dependencies.add(getProfilesModificationTracker());
    dependencies.add(getMultipleContextsModificationTracker());
    dependencies.add(ProjectRootManager.getInstance(this.myProject));
    dependencies.add(FacetFinder.getInstance(this.myProject).getAllFacetsOfTypeModificationTracker(InfraFacet.FACET_TYPE_ID));
    dependencies.add(DumbService.getInstance(this.myProject).getModificationTracker());
    return ArrayUtil.toObjectArray(dependencies);
  }

  @Override
  public void dispose() {

  }
}
