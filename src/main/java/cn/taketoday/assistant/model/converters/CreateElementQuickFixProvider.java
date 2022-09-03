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

package cn.taketoday.assistant.model.converters;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.CharFilter;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xml.GenericDomValue;

import cn.taketoday.lang.Nullable;

public abstract class CreateElementQuickFixProvider<T> {
  @IntentionFamilyName
  private final String myFamilyName;

  protected abstract void apply(String str, GenericDomValue<T> genericDomValue);

  @IntentionName
  protected abstract String getFixName(String str);

  public CreateElementQuickFixProvider(@IntentionFamilyName String familyName) {
    this.myFamilyName = familyName;
  }

  public LocalQuickFix[] getQuickFixes(GenericDomValue<T> value) {
    LocalQuickFix fix = getQuickFix(value);
    return fix == null ? LocalQuickFix.EMPTY_ARRAY : new LocalQuickFix[] { fix };
  }

  @Nullable
  public LocalQuickFix getQuickFix(GenericDomValue<T> value) {
    String elementName = getElementName(value);
    if (!isAvailable(elementName, value)) {
      return null;
    }
    GenericDomValue<T> copy = value.createStableCopy();
    return new LocalQuickFix() {

      public String getName() {
        String fixName = CreateElementQuickFixProvider.this.getFixName(elementName);
        return fixName;
      }

      public String getFamilyName() {
        String str = CreateElementQuickFixProvider.this.myFamilyName;
        return str;
      }

      public void applyFix(Project project, ProblemDescriptor descriptor) {
        StringUtil.strip(StringUtil.capitalizeWords(CreateElementQuickFixProvider.this.myFamilyName, true), CharFilter.NOT_WHITESPACE_FILTER);
        CreateElementQuickFixProvider.this.apply(elementName, copy);
      }
    };
  }

  protected boolean isAvailable(String elementName, GenericDomValue<T> value) {
    return elementName != null && elementName.trim().length() > 0;
  }

  @Nullable
  public String getElementName(GenericDomValue<T> value) {
    return value.getStringValue();
  }
}
