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

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.facet.beans.CustomSetting;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/22 18:05
 */
public class InfraFacet extends Facet<InfraFacetConfiguration> {
  private final Set<InfraFileSet> fileSets = new LinkedHashSet<>();

  public static final FacetTypeId<InfraFacet> FACET_TYPE_ID = new FacetTypeId<>("today");

  public InfraFacet(FacetType<InfraFacet, InfraFacetConfiguration> facetType,
          Module module, String name, InfraFacetConfiguration configuration, Facet underlyingFacet) {
    super(facetType, module, name, configuration, underlyingFacet);

    for (InfraFileSetData fileSetData : configuration.getFileSetDescriptors()) {
      InfraFileSet fileSet = new InfraFileSetImpl(fileSetData, this);
      fileSets.add(fileSet);
      Disposer.register(this, fileSet);
    }

    Disposer.register(this, configuration);
  }

  @Nullable
  public static InfraFacet from(Module module) {
    if (module.isDisposed()) {
      return null;
    }
    return FacetManager.getInstance(module).getFacetByType(FACET_TYPE_ID);
  }

  public static FacetType<InfraFacet, InfraFacetConfiguration> getFacetType() {
    return FacetTypeRegistry.getInstance().findFacetType(FACET_TYPE_ID);
  }

  /**
   * Returns all user-configured filesets.
   *
   * @return Filesets.
   * @see InfraFileSetService#getAllSets(InfraFacet)
   */
  public Set<InfraFileSet> getFileSets() {
    return Collections.unmodifiableSet(this.fileSets);
  }

  /**
   * Adds new empty fileset.
   *
   * @param id Unique non-empty ID.
   * @param name Unique non-empty name.
   * @return Empty fileset.
   * @see InfraFileSetService#getUniqueId(Set)
   * @see InfraFileSetService#getUniqueName(String, Set)
   */
  public InfraFileSet addFileSet(String id, String name) {
    return this.addFileSet(new InfraFileSetImpl(id, name, this));
  }

  public InfraFileSet addFileSet(InfraFileSet fileSet) {
    assertValid(fileSet);
    this.fileSets.add(fileSet);
    getConfiguration().addFileSetData(fileSet.getData());
    Disposer.register(this, fileSet);
    return fileSet;
  }

  public void removeFileSets() {
    this.fileSets.clear();
    this.getConfiguration().removeFileSetDescriptors();
  }

  public boolean hasXmlMappings() {
    for (InfraFileSet fileSet : fileSets) {
      if (!fileSet.getXmlFiles().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param settingsKey Key of settings.
   * @param <S> Settings class. Usually {@link CustomSetting.BOOLEAN} or {@link CustomSetting.STRING}.
   * @return Setting or {@code null} if not set.
   */
  @Nullable
  public <S extends CustomSetting> S findSetting(Key<S> settingsKey) {
    for (CustomSetting customSetting : getConfiguration().getCustomSettings()) {
      if (customSetting.getName().equals(settingsKey.toString())) {
        return (S) customSetting;
      }
    }
    return null;
  }

  private static void assertValid(InfraFileSet fileSet) {
    String id = fileSet.getId();
    assert StringUtil.isNotEmpty(id) : "empty ID " + fileSet;
    String name = fileSet.getName();
    assert StringUtil.isNotEmpty(name) : "empty name " + fileSet;
  }
}

