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

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.app.spi.InfraImportsManager;
import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.assistant.model.config.autoconfigure.jam.EnableAutoConfiguration;
import cn.taketoday.assistant.model.config.autoconfigure.jam.ImportAutoConfiguration;
import cn.taketoday.lang.Nullable;

public class AutoConfigClassCollector {
  private static final String CONTEXT_AUTOCONFIGURE_EXCLUDE = "context.autoconfigure.exclude";

  public static List<PsiClass> collectConfigurationClasses(
          @Nullable EnableAutoConfiguration enableAutoConfiguration, ConditionalOnEvaluationContext context) {
    Module module = context.getModule();
    if (module.isDisposed()) {
      return Collections.emptyList();
    }
    List<PsiClass> allAutoConfigurationClasses = InfraImportsManager.getInstance(module).getAutoConfigurationClasses(false);
    List<PsiClass> excludedViaAnnotation = enableAutoConfiguration == null ? Collections.emptyList() : enableAutoConfiguration.getExcludes();
    allAutoConfigurationClasses.removeAll(excludedViaAnnotation);
    excludeByConfigProperty(context, allAutoConfigurationClasses);
    AutoConfigClassSorter sorter = new AutoConfigClassSorter(module, allAutoConfigurationClasses);
    return sorter.getSortedConfigs();
  }

  private static void excludeByConfigProperty(ConditionalOnEvaluationContext context, List<PsiClass> classes) {
    Processor<List<ConfigurationValueResult>> autoConfigExcludedProcessor = results -> {
      ConfigurationValueResult item = ContainerUtil.getFirstItem(results);
      if (item != null && item.getValueElement() != null) {
        for (PsiReference reference : item.getValueElement().getReferences()) {
          PsiElement resolve = reference.resolve();
          if (resolve instanceof PsiClass psiClass) {
            classes.remove(psiClass);
          }
        }
        return false;
      }
      return false;
    };
    context.processConfigurationValues(autoConfigExcludedProcessor, true, CONTEXT_AUTOCONFIGURE_EXCLUDE);
  }

  public static List<PsiClass> collectConfigurationClasses(ImportAutoConfiguration importAutoConfiguration, ConditionalOnEvaluationContext context) {
    String fqn;
    Module module = context.getModule();
    if (module.isDisposed()) {
      return Collections.emptyList();
    }
    List<PsiClass> classes = importAutoConfiguration.getClasses();
    if (classes.isEmpty() && (fqn = importAutoConfiguration.getSourceFqn()) != null) {
      classes = InfraImportsManager.getInstance(module).getClasses(false, fqn);
    }
    List<PsiClass> excludedViaAnnotation = importAutoConfiguration.getExcludes();
    classes.removeAll(excludedViaAnnotation);
    excludeByConfigProperty(context, classes);
    AutoConfigClassSorter sorter = new AutoConfigClassSorter(module, classes);
    return sorter.getSortedConfigs();
  }
}
