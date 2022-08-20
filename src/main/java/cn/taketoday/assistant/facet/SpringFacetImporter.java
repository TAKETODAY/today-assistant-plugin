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

package cn.taketoday.assistant.facet;

import com.intellij.facet.FacetType;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
import com.intellij.spring.facet.SpringFacet;
import com.intellij.spring.facet.SpringFacetConfiguration;
import com.intellij.spring.facet.SpringFacetType;

import org.jetbrains.idea.maven.importing.FacetImporter;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 01:19
 */
public class SpringFacetImporter extends FacetImporter<SpringFacet, SpringFacetConfiguration, SpringFacetType> {

  public SpringFacetImporter() {
    super("dummy", "dummy", FacetType.findInstance(SpringFacetType.class));
  }

  @Override
  public boolean isApplicable(MavenProject mavenProject) {
    if (mavenProject.isAggregator()) {
      return false;
    }
    else {
      List<MavenArtifact> dependencies = mavenProject.findDependencies("cn.taketoday", "today-context");
      return !dependencies.isEmpty();
    }
  }

  @Override
  protected boolean isDisableFacetAutodetection(Module module) {
    return true;
  }

  @Override
  protected void setupFacet(SpringFacet facet, MavenProject mavenProject) {
    System.out.println("setupFacet: " + "facet: " + facet + " mavenProject: " + mavenProject);
  }

  @Override
  protected void reimportFacet(IdeModifiableModelsProvider modelsProvider,
          Module module, MavenRootModelAdapter rootModel, SpringFacet facet,
          MavenProjectsTree mavenTree, MavenProject mavenProject,
          MavenProjectChanges changes, Map<MavenProject, String> mavenProjectToModuleName,
          List<MavenProjectsProcessorTask> postTasks) {

  }

}
