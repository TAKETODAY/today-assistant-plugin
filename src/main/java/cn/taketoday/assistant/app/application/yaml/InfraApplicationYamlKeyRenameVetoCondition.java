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

import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;

import org.jetbrains.yaml.psi.YAMLKeyValue;

public class InfraApplicationYamlKeyRenameVetoCondition implements Condition<PsiElement> {

  public boolean value(PsiElement psiElement) {
    return (isKeyDefinition(psiElement) || isKeyReference(psiElement)) && InfraApplicationYamlUtil.isInsideApplicationYamlFile(psiElement);
  }

  private static boolean isKeyDefinition(PsiElement psiElement) {
    return (psiElement instanceof LeafPsiElement) && (psiElement.getParent() instanceof YAMLKeyValue);
  }

  private static boolean isKeyReference(PsiElement psiElement) {
    return psiElement instanceof YAMLKeyValue;
  }
}
