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

package cn.taketoday.assistant.app.application.yaml;

import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;

import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLScalar;

import cn.taketoday.assistant.ReferencePatternConditions;

public class InfraApplicationYamlReferenceContributor extends PsiReferenceContributor {
  static final PatternCondition<PsiElement> APPLICATION_YAML_SB_1_2_OR_HIGHER = new PatternCondition<PsiElement>("isApplicationYamlAndSB_1_2") {

    public boolean accepts(PsiElement element, ProcessingContext context) {
      return ReferencePatternConditions.PROJECT_HAS_FACETS_CONDITION.accepts(element, context)
              && InfraApplicationYamlUtil.isInsideApplicationYamlFile(element);
    }
  };

  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    registerKeyProviders(registrar);
    registerValueProviders(registrar);
  }

  private static void registerKeyProviders(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(YAMLKeyValue.class).with(APPLICATION_YAML_SB_1_2_OR_HIGHER),
            new InfraApplicationYamlKeyReferenceProvider());
  }

  private static void registerValueProviders(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(YAMLScalar.class).with(APPLICATION_YAML_SB_1_2_OR_HIGHER), new InfraApplicationYamlValueReferenceProvider());
  }
}
