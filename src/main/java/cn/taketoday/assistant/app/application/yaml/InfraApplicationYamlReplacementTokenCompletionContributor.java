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

import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Key;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;

import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.YAMLTokenTypes;

import java.util.List;

import cn.taketoday.assistant.app.application.config.InfraReplacementTokenCompletionContributor;

public class InfraApplicationYamlReplacementTokenCompletionContributor extends InfraReplacementTokenCompletionContributor {

  private static final Key<Couple<String>> KEY = Key.create("InfraApplicationYamlReplacementTokenCompletionContributor");

  static final PsiElementPattern.Capture<LeafPsiElement> VALUE_PATTERN = PlatformPatterns.psiElement(LeafPsiElement.class)
          .andOr(
                  PlatformPatterns.psiElement()
                          .withElementType(YAMLElementTypes.SCALAR_VALUES)
                          .andNot(PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(YAMLTokenTypes.INDENT))),
                  PlatformPatterns.psiElement()
                          .afterLeaf(PlatformPatterns.psiElement(YAMLTokenTypes.COLON))
          )
          .with(InfraApplicationYamlReferenceContributor.APPLICATION_YAML_SB_1_2_OR_HIGHER);

  public InfraApplicationYamlReplacementTokenCompletionContributor() {
    super(VALUE_PATTERN, KEY, InfraApplicationYamlValueReferenceProvider.INSPECTION_ID);
  }

  @Override
  protected List<Couple<String>> getReplacementTokens(PsiElement place) {
    return InfraApplicationYamlValueReferenceProvider.getReplacementTokens(place);
  }
}
