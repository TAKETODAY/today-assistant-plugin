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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.spi.SPIFileType;
import com.intellij.spi.psi.SPIClassProvidersElementList;
import com.intellij.spi.psi.SPIPackageOrClassReferenceElement;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ProcessingContext;

import cn.taketoday.lang.Nullable;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

public final class InfraImportsReferenceProvider extends PsiReferenceProvider {

  public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
    return new PsiReference[] {
            new ImportPackageOrClassReference((SPIPackageOrClassReferenceElement) element)
    };
  }

  private static final class ImportPackageOrClassReference implements PsiReference {
    private final SPIPackageOrClassReferenceElement psiElement;

    public ImportPackageOrClassReference(SPIPackageOrClassReferenceElement psiElement) {
      this.psiElement = psiElement;
    }

    public PsiElement getElement() {
      return this.psiElement;
    }

    public TextRange getRangeInElement() {
      PsiElement last = PsiTreeUtil.getDeepestLast(this.psiElement);
      return new TextRange(last.getStartOffsetInParent(), this.psiElement.getTextLength());
    }

    public String getCanonicalText() {
      return this.psiElement.getCanonicalText();
    }

    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
      return this.psiElement.handleElementRename(newElementName);
    }

    @Nullable
    public PsiElement resolve() {
      return this.psiElement.resolve();
    }

    @Nullable
    public PsiElement bindToElement(PsiElement element) throws IncorrectOperationException {
      String qualifiedName;
      if (element instanceof PsiPackage) {
        qualifiedName = ((PsiPackage) element).getQualifiedName();
      }
      else if (element instanceof PsiClass psiClass) {
        String text = this.psiElement.getText();
        if (StringsKt.contains(text, '$', false)) {
          qualifiedName = ClassUtil.getJVMClassName(psiClass);
        }
        else {
          qualifiedName = psiClass.getQualifiedName();
        }
      }
      else {
        return null;
      }
      String newName = qualifiedName;
      if (newName == null) {
        return null;
      }
      PsiFile createFileFromText = PsiFileFactory.getInstance(this.psiElement.getProject()).createFileFromText("spi_dummy", SPIFileType.INSTANCE, newName);
      SPIClassProvidersElementList firstChild = (SPIClassProvidersElementList) createFileFromText.getFirstChild();
      return this.psiElement.replace(firstChild.getElements().get(0));
    }

    public boolean isReferenceTo(PsiElement element) {
      if (element instanceof PsiPackage) {
        return Intrinsics.areEqual(this.psiElement.getText(), ((PsiPackage) element).getQualifiedName());
      }
      if (element instanceof PsiClass) {
        String text = this.psiElement.getText();
        return Intrinsics.areEqual(text, ((PsiClass) element).getQualifiedName()) || Intrinsics.areEqual(text, ClassUtil.getJVMClassName((PsiClass) element));
      }
      return false;
    }

    public boolean isSoft() {
      return this.psiElement.isSoft();
    }
  }
}
