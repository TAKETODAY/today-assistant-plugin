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

package cn.taketoday.assistant.model.jam.testContexts.providers;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.model.jam.testContexts.ContextConfiguration;
import cn.taketoday.assistant.model.jam.testContexts.TestingImplicitContextsProvider;
import cn.taketoday.lang.Nullable;

public class InnerClassConfigurationsProvider extends TestingImplicitContextsProvider {

  @Override
  public Collection<CommonInfraModel> getModels(@Nullable Module module, ContextConfiguration configuration, Set<String> activeProfiles) {
    if (module == null) {
      return Collections.emptyList();
    }
    Function<PsiClass, CommonInfraModel> function = aClass -> {
      return LocalModelFactory.of().getOrCreateLocalAnnotationModel(aClass, module, activeProfiles);
    };
    return LocalAnnotationModel.getInnerStaticClassModels(configuration.getPsiElement(), function);
  }
}
