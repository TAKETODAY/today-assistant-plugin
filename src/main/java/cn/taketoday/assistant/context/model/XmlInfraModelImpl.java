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
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.CommonProcessors;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomFileElement;

import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitorContext;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitors;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.lang.Nullable;

public class XmlInfraModelImpl extends XmlInfraModel {

  private final Set<XmlFile> myConfigFiles;

  public XmlInfraModelImpl(Set<XmlFile> configFiles, Module module, @Nullable InfraFileSet fileSet) {
    super(module, fileSet);
    this.myConfigFiles = configFiles;
  }

  @Override

  public Set<CommonInfraModel> getRelatedModels(boolean checkActiveProfiles) {
    Set<CommonInfraModel> models = new LinkedHashSet<>();
    models.addAll(getLocalSpringModels());
    models.addAll(getDependencies());
    return models;
  }

  @Override
  public Set<LocalXmlModel> getLocalSpringModels() {
    InfraFileSet fileSet = getFileSet();
    Set<String> activeProfiles = fileSet == null ? getActiveProfiles() : fileSet.getActiveProfiles();
    return new LinkedHashSet<>(ContainerUtil.mapNotNull(myConfigFiles, xmlFile -> {
      return LocalModelFactory.of().getOrCreateLocalXmlModel(xmlFile, getModule(), activeProfiles);
    }));
  }

  @Override
  public Set<XmlFile> getXmlConfigFiles() {
    return this.myConfigFiles;
  }

  private boolean hasImportedConfigs(final XmlFile file) {
    CommonProcessors.FindProcessor<CommonInfraModel> findProcessor = new CommonProcessors.FindProcessor<>() {
      public boolean accept(CommonInfraModel model) {
        return (model instanceof cn.taketoday.assistant.context.model.LocalXmlModel) && ((LocalXmlModel) model).getConfig().equals(file);
      }
    };
    for (cn.taketoday.assistant.context.model.LocalXmlModel model : getLocalSpringModels()) {
      if (!InfraModelVisitors.visitRecursionAwareRelatedModels(
              model, InfraModelVisitorContext.context(findProcessor, (m, p) -> p.process(m)))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Set<DomFileElement<Beans>> getLocalModelsRoots() {
    Set<LocalXmlModel> localModels = getLocalSpringModels();
    Set<DomFileElement<Beans>> set = new LinkedHashSet<>(localModels.size());
    for (LocalXmlModel localXmlModel : localModels) {
      DomFileElement<Beans> root = localXmlModel.getRoot();
      ContainerUtil.addIfNotNull(set, root);
    }
    return set;
  }
}
