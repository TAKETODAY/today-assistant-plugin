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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;

import java.util.Objects;

import cn.taketoday.lang.Nullable;
import kotlin.jvm.internal.Intrinsics;

public final class WebClientUrl {
  private final SmartPsiElementPointer myElementPointer;
  private final String myUriPresentation;
  private final String myHttpMethod;

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

  public String getUriPresentation() {
    return this.myUriPresentation;
  }

  @Nullable
  public String getHttpMethod() {
    return this.myHttpMethod;
  }

  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    else if (!(other instanceof WebClientUrl)) {
      return false;
    }
    else if (!Intrinsics.areEqual(this.myUriPresentation, ((WebClientUrl) other).myUriPresentation)) {
      return false;
    }
    else if (!Intrinsics.areEqual(this.myHttpMethod, ((WebClientUrl) other).myHttpMethod)) {
      return false;
    }
    else {
      return Objects.equals(this.myElementPointer, ((WebClientUrl) other).myElementPointer);
    }
  }

  public int hashCode() {
    int result = this.myUriPresentation.hashCode();
    int var10000 = 31 * result;
    String var10001 = this.myHttpMethod;
    result = var10000 + (var10001 != null ? var10001.hashCode() : 0);
    result = 31 * result + this.myElementPointer.hashCode();
    return result;
  }

  public WebClientUrl(PsiElement psiElement, String myUriPresentation, @Nullable String myHttpMethod) {
    this.myUriPresentation = myUriPresentation;
    this.myHttpMethod = myHttpMethod;
    this.myElementPointer = SmartPointerManager.getInstance(psiElement.getProject())
            .createSmartPsiElementPointer(psiElement);
  }
}
