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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.Topic;
import com.intellij.util.text.UniqueNameGenerator;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.service.InfraModelProvider;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Yann C&eacute;bron
 * @since 1.0 2022/8/24 22:49
 */
public class InfraFileSetService {
  public static final Topic<InfraFileSetListener> TOPIC = new Topic<>("File set topic",
          InfraFileSetListener.class);

  public static final ExtensionPointName<InfraModelProvider> MODEL_PROVIDER_EP_NAME
          = new ExtensionPointName<>("cn.taketoday.assistant.infraModelProvider");

  public static InfraFileSetService of() {
    return ApplicationManager.getApplication().getService(InfraFileSetService.class);
  }

  public interface InfraFileSetListener {

    void activeProfilesChanged();
  }

  private static final String ID_PREFIX = "fileset";
  private static final String DEPENDENCY_DELIMITER = "==";
  private static final Function<InfraFileSet, String> FILESET_ID = InfraFileSet::getId;

  public InfraFileSetService() {
    MODEL_PROVIDER_EP_NAME.addChangeListener(InfraAutodetectedFileSet::refreshAutodetectedFileSets, null);
  }

  public String getUniqueId(Set<InfraFileSet> existing) {
    return UniqueNameGenerator.generateUniqueName(ID_PREFIX, ContainerUtil.map(existing, FILESET_ID));
  }

  public String getUniqueName(String prefix, Set<InfraFileSet> existing) {
    UniqueNameGenerator generator = new UniqueNameGenerator(existing, InfraFileSet::getName);
    return generator.generateUniqueName(prefix, "", "", " (", ")");
  }

  /**
   * Returns all configured and provided file sets.
   *
   * @param facet Facet instance.
   * @return all working file sets for the facet.
   * @see InfraFacet#getFileSets()
   * @see #getModelProviderSets(InfraFacet)
   */
  public Set<InfraFileSet> getAllSets(InfraFacet facet) {
    Set<InfraFileSet> fileSets = getFileSets(facet);
    Set<String> existing = ContainerUtil.map2Set(fileSets, FILESET_ID);
    for (InfraFileSet provided : getModelProviderSets(facet)) {
      if (!existing.contains(provided.getId())) {
        fileSets.add(provided);
      }
    }
    return fileSets;
  }

  /**
   * Returns filesets provided by all {@link InfraModelProvider}s.
   *
   * @param facet Facet instance.
   * @return filesets provided by {@link InfraModelProvider}.
   * @see InfraModelProvider
   */
  public List<InfraFileSet> getModelProviderSets(InfraFacet facet) {
    if (facet.getModule().isDisposed() || DumbService.isDumb(facet.getModule().getProject())) {
      return Collections.emptyList();
    }
    SmartList<InfraFileSet> smartList = new SmartList<>();
    for (InfraModelProvider infraModelProvider : MODEL_PROVIDER_EP_NAME.getExtensionList()) {
      List<? extends InfraAutodetectedFileSet> modelProviderFilesets = infraModelProvider.getFilesets(facet);
      smartList.addAll(modelProviderFilesets);
    }
    return smartList;
  }

  /**
   * @param module Module to search usage in.
   * @param psiFile Configuration file.
   * @return {@code null} if given file not configured.
   */
  @Nullable
  public InfraFileSet findFileSet(Module module, PsiFile psiFile) {
    InfraFacet facet = InfraFacet.from(module);
    if (facet == null) {
      return null;
    }
    VirtualFile virtualFile = psiFile.getVirtualFile();
    for (InfraFileSet fileSet : getAllSets(facet)) {
      if (fileSet.hasFile(virtualFile)) {
        return fileSet;
      }
    }
    for (InfraModel model : InfraManager.from(module.getProject()).getAllModels(module)) {
      if (model.getFileSet() == null) {
        return null;
      }
      if (InfraModelVisitorUtils.hasConfigFile(model, psiFile)) {
        return model.getFileSet();
      }
    }
    return null;
  }

  public String getQualifiedName(InfraFileSet fileset) {
    return fileset.getId() + "==" + fileset.getFacet().getModule().getName();
  }

  public String getDependencyIdFor(InfraFileSet current, InfraFileSet otherFileSet) {
    boolean isCurrentModule = current.getFacet().getModule().equals(otherFileSet.getFacet().getModule());
    if (isCurrentModule) {
      return otherFileSet.getId();
    }
    return getQualifiedName(otherFileSet);
  }

  @Nullable
  public InfraFileSet findDependencyFileSet(InfraFileSet current, String dependencyId) {
    InfraFacet facetToSearch = getFacetFor(current, dependencyId);
    if (facetToSearch == null) {
      return null;
    }
    Condition<InfraFileSet> fileSetCondition = springFileSet -> getDependencyIdFor(current, springFileSet).equals(dependencyId);
    InfraFileSet fileSet = ContainerUtil.find(getFileSets(facetToSearch), fileSetCondition);
    if (fileSet != null) {
      return fileSet;
    }
    return ContainerUtil.find(getModelProviderSets(facetToSearch), fileSetCondition);
  }

  private static Set<InfraFileSet> getFileSets(InfraFacet facet) {
    return new LinkedHashSet<>(facet.getFileSets());
  }

  @Nullable
  private static InfraFacet getFacetFor(InfraFileSet current, String dependencyId) {
    if (!dependencyId.contains(DEPENDENCY_DELIMITER)) {
      return current.getFacet();
    }
    String moduleName = StringUtil.substringAfter(dependencyId, DEPENDENCY_DELIMITER);
    Module moduleByName = ModuleManager.getInstance(current.getFacet().getModule().getProject()).findModuleByName(moduleName);
    if (moduleByName != null) {
      return InfraFacet.from(moduleByName);
    }
    return null;
  }

}
