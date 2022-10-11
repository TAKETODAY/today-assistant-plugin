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

package cn.taketoday.assistant.app.application.metadata;

import com.intellij.ide.presentation.Presentation;
import com.intellij.microservices.jvm.config.ConfigKeyDeclarationPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;

import cn.taketoday.lang.Nullable;

@Presentation(typeName = InfraConfigKeyDeclarationPsiElement.CONFIGURATION_KEY)
public final class InfraConfigKeyDeclarationPsiElement extends ConfigKeyDeclarationPsiElement {

  public static final String CONFIGURATION_KEY = "Infra Configuration Key";

  private final String libraryName;
  private final String configKeyName;
  private final String sourceTypeText;

  private final PsiElement navigationParent;
  private final PsiElement navigationTarget;

  InfraConfigKeyDeclarationPsiElement(String libraryName,
          PsiElement navigationParent, PsiElement navigationTarget,
          String configKeyName, String sourceTypeText, @Nullable PsiType type) {
    super(type);
    this.navigationParent = navigationParent;
    this.libraryName = libraryName;
    this.navigationTarget = navigationTarget;
    this.configKeyName = configKeyName;
    this.sourceTypeText = sourceTypeText;
  }

  public PsiElement getParent() {
    return this.navigationParent;
  }

  public PsiElement getNavigationElement() {
    return this.navigationTarget;
  }

  public String getPresentableText() {
    return this.sourceTypeText;
  }

  public String getName() {
    return this.configKeyName;
  }

  @Nullable
  public String getLocationString() {
    return this.libraryName;
  }
}
