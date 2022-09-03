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

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.spi.psi.SPIClassProviderReferenceElement;
import com.intellij.spi.psi.SPIFile;
import com.intellij.util.ProcessingContext;

public final class InfraImportsCompletionContributor extends CompletionContributor {

  public static final PatternCondition<PsiElement> FILE_CONDITION = new PatternCondition<>("isInfraImportsFile") {
    public boolean accepts(PsiElement element, ProcessingContext context) {
      PsiFile containingFile = element.getContainingFile();
      if (containingFile != null) {
        PsiFile originalFile = containingFile.getOriginalFile();
        if (originalFile instanceof SPIFile) {
          FileTypeRegistry fileTypeRegistry = FileTypeRegistry.getInstance();
          VirtualFile virtualFile = originalFile.getVirtualFile();
          return virtualFile != null
                  && fileTypeRegistry.isFileOfType(virtualFile, InfraImportsFileType.FILE_TYPE);
        }
        return false;
      }
      return false;
    }
  };

  public InfraImportsCompletionContributor() {
    var psiElementCapture = PlatformPatterns.psiElement(LeafPsiElement.class).withParent(SPIClassProviderReferenceElement.class);
    extend(CompletionType.BASIC, psiElementCapture.with(FILE_CONDITION), new InfraImportsCompletionProvider());
  }
}
