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

package cn.taketoday.assistant.web.mvc.client;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PointersKt;

import cn.taketoday.lang.Nullable;
import kotlin.jvm.internal.Intrinsics;

public final class WebClientHolder {
  private final SmartPsiElementPointer myElementPointer;

  public PsiElement getPsiElement() {
    PsiElement var10000 = this.myElementPointer.getElement();
    if (var10000 != null) {
      return var10000;
    }
    else {
      throw new PsiInvalidElementAccessException(null, "Pointer hasn't survive");
    }
  }

  public boolean isValid() {
    return this.myElementPointer.getElement() != null;
  }

  @Nullable
  public String getName() {
    PsiElement element = this.myElementPointer.getElement();
    String var10000;
    if (element instanceof PsiClass) {
      var10000 = ((PsiClass) element).getName();
      if (var10000 == null) {
        PsiFile var2 = element.getContainingFile();
        Intrinsics.checkNotNullExpressionValue(var2, "element.containingFile");
        var10000 = var2.getName();
      }
    }
    else {
      var10000 = element instanceof PsiFile ? ((PsiFile) element).getName() : null;
    }

    return var10000;
  }

  public WebClientHolder(PsiElement psiElement) {
    Intrinsics.checkNotNullParameter(psiElement, "psiElement");
    this.myElementPointer = PointersKt.createSmartPointer(psiElement, null);
  }
}
