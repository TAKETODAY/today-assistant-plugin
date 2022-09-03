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

package cn.taketoday.assistant.references.injector;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.injection.ReferenceInjector;
import com.intellij.util.ProcessingContext;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.model.values.PlaceholderUtils;

public class InfraPlaceholderReferenceInjector extends ReferenceInjector {

  public PsiReference[] getReferences(PsiElement element, ProcessingContext context, TextRange range) {
    PsiReference[] psiReferenceArr;
    if (element instanceof PsiLiteralExpression) {
      psiReferenceArr = PlaceholderUtils.getInstance().createPlaceholderPropertiesReferences(element);
    }
    else {
      psiReferenceArr = PsiReference.EMPTY_ARRAY;
    }
    return psiReferenceArr;
  }

  public String getId() {
    return "InfraPlaceholderReferenceInjector";
  }

  public String getDisplayName() {
    return InfraBundle.message("reference.injector.placeholder");
  }

  public Icon getIcon() {
    return Icons.SpringBean;
  }
}
