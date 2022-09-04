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

package cn.taketoday.assistant.model.config.autoconfigure;

import com.intellij.psi.PsiClass;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.app.InfraApplicationService;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.context.model.graph.LocalModelDependencyType;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.assistant.model.config.autoconfigure.jam.EnableAutoConfiguration;

public class EnableAutoConfigDependentModelsProvider extends AbstractAutoConfigDependentModelsProvider {

  @Override
  protected List<PsiClass> getAutoConfigClasses(LocalAnnotationModel localAnnotationModel, ConditionalOnEvaluationContext sharedContext) {
    EnableAutoConfiguration enableAutoConfiguration = EnableAutoConfiguration.META.getJamElement(localAnnotationModel.getConfig());
    return enableAutoConfiguration == null
           ? Collections.emptyList()
           : AutoConfigClassCollector.collectConfigurationClasses(enableAutoConfiguration, sharedContext);
  }

  @Override
  protected boolean acceptModel(LocalAnnotationModel model) {
    return super.acceptModel(model) && InfraApplicationService.of().isInfraApplication(model.getConfig());
  }

  @Override
  protected LocalModelDependencyType getModelDependencyType() {
    return LocalModelDependencyType.ENABLE_ANNO;
  }
}
