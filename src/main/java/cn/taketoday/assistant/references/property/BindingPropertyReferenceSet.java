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

package cn.taketoday.assistant.references.property;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.ReferenceSetBase;

import cn.taketoday.lang.Nullable;

/**
 * Reference to bean property path ({@code property.nestedProperty}).
 * <p/>
 * Override {@link #isSoft()} to build non-soft references.
 *
 * @author Yann C&eacute;bron
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class BindingPropertyReferenceSet extends ReferenceSetBase<BindingPropertyReference> {

  @Nullable
  private final PsiClass myModelClass;

  public BindingPropertyReferenceSet(PsiElement element,
          @Nullable PsiClass modelClass) {
    super(element);

    myModelClass = modelClass;
  }

  @Nullable
  @Override
  protected BindingPropertyReference createReference(TextRange range, int index) {
    return new BindingPropertyReference(this, range, index);
  }

  @Override
  public PsiReference[] getPsiReferences() {
    return getReferences().toArray(new BindingPropertyReference[0]);
  }

  @Nullable
  protected PsiClass getModelClass() {
    return myModelClass;
  }
}
