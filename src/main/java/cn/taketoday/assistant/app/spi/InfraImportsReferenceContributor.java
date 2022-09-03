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

package cn.taketoday.assistant.app.spi;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.spi.psi.SPIFile;
import com.intellij.spi.psi.SPIPackageOrClassReferenceElement;

public final class InfraImportsReferenceContributor extends PsiReferenceContributor {

  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    var psiElementCapture = PlatformPatterns.psiElement(SPIPackageOrClassReferenceElement.class);

    registrar.registerReferenceProvider(
            psiElementCapture.with(InfraImportsCompletionContributor.FILE_CONDITION),
            new InfraImportsReferenceProvider()
    );

    var psiFileCapture = PlatformPatterns.psiElement(SPIFile.class);
    registrar.registerReferenceProvider(
            psiFileCapture.with(InfraImportsCompletionContributor.FILE_CONDITION),
            new InfraImportsFileReferenceProvider()
    );
  }
}
