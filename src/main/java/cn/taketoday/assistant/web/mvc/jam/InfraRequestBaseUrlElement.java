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

package cn.taketoday.assistant.web.mvc.jam;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.util.PartiallyKnownString;
import com.intellij.semantic.SemKey;

import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.lang.Nullable;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

public abstract class InfraRequestBaseUrlElement extends InfraStereotypeElement {

  private static final SemKey<InfraRequestBaseUrlElement> JAM_ELEMENT_KEY
          = PSI_MEMBERINFRA_BEAN_JAM_KEY.subKey("InfraRequestBaseUrl");

  public static final Companion Companion = new Companion(null);

  @Nullable
  public abstract PartiallyKnownString getUrl();

  public InfraRequestBaseUrlElement(@Nullable String anno, PsiElementRef<PsiClass> psiElementRef) {
    super(anno, psiElementRef);
    Intrinsics.checkNotNullParameter(psiElementRef, "ref");
  }

  public static final class Companion {
    private Companion() {
    }

    public Companion(DefaultConstructorMarker $constructor_marker) {
      this();
    }

    public SemKey<InfraRequestBaseUrlElement> getJAM_ELEMENT_KEY() {
      return JAM_ELEMENT_KEY;
    }
  }

}
