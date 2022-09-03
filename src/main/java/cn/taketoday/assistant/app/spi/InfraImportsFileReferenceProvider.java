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

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.DelimitedListProcessor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.util.ClassUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ProcessingContext;

import java.util.ArrayList;
import java.util.Objects;

import cn.taketoday.lang.Nullable;
import kotlin.text.StringsKt;

public final class InfraImportsFileReferenceProvider extends PsiReferenceProvider {

  @Override
  public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
    PsiFile spiFile = (PsiFile) element;

    String text = FileUtil.getNameWithoutExtension(spiFile.getName());
    ArrayList<PsiReference> references = new ArrayList<>();

    new DelimitedListProcessor(".$") {
      protected void processToken(int start, int end, boolean delimitersOnly) {
        references.add(new FilePackageOrClassReference(spiFile, start, end));
      }
    }.processText(text);
    return references.toArray(PsiReference.EMPTY_ARRAY);
  }

  private static final class FilePackageOrClassReference extends PsiReferenceBase<PsiFile> {
    private final PsiFile psiFile;
    private final int start;
    private final int end;

    public FilePackageOrClassReference(PsiFile psiFile, int start, int end) {
      super(psiFile, new TextRange(0, 0), false);
      this.psiFile = psiFile;
      this.start = start;
      this.end = end;
    }

    public String getCanonicalText() {
      String name = this.psiFile.getName();
      return name.substring(0, this.end);
    }

    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
      TextRange create = TextRange.create(this.start, this.end);
      return handleElementRename(newElementName, create);
    }

    @Nullable
    public PsiElement resolve() {
      String text = getCanonicalText();
      PsiElement findPackage = JavaPsiFacade.getInstance(this.psiFile.getProject()).findPackage(text);
      return findPackage != null ? findPackage : ClassUtil.findPsiClass(this.psiFile.getManager(), text, null, true, this.psiFile.getResolveScope());
    }

    public PsiElement bindToElement(PsiElement psiElement) throws IncorrectOperationException {
      String qualifiedName;
      if (psiElement instanceof PsiPackage) {
        String qualifiedName2 = ((PsiPackage) psiElement).getQualifiedName();
        TextRange create = TextRange.create(0, this.end);
        return handleElementRename(qualifiedName2, create);
      }
      else if (psiElement instanceof PsiClass) {
        String name = this.psiFile.getName();
        if (StringsKt.contains(name, '$', false)) {
          qualifiedName = ClassUtil.getJVMClassName((PsiClass) psiElement);
        }
        else {
          qualifiedName = ((PsiClass) psiElement).getQualifiedName();
        }
        if (qualifiedName != null) {
          TextRange create2 = TextRange.create(0, this.end);
          return handleElementRename(qualifiedName, create2);
        }
        return getElement();
      }
      else {
        return getElement();
      }
    }

    public boolean isReferenceTo(PsiElement element) {
      if (element instanceof PsiPackage psiPackage) {
        return Objects.equals(getCanonicalText(), psiPackage.getQualifiedName());
      }
      if (element instanceof PsiClass psiClass) {
        String text = getCanonicalText();
        return Objects.equals(text, psiClass.getQualifiedName())
                || Objects.equals(text, ClassUtil.getJVMClassName((PsiClass) element));
      }
      return false;
    }

    private PsiElement handleElementRename(String newElementName, TextRange textRange) throws IncorrectOperationException {
      String fileName = this.psiFile.getName();
      String name = FileUtil.getNameWithoutExtension(fileName);
      String newName = StringUtil.replaceSubstring(name, textRange, newElementName);
      this.psiFile.setName(newName + "." + FileUtilRt.getExtension(fileName));
      return getElement();
    }
  }
}
