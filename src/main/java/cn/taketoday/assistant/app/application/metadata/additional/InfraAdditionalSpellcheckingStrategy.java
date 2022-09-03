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

package cn.taketoday.assistant.app.application.metadata.additional;

import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.Tokenizer;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.util.InfraUtils;

public class InfraAdditionalSpellcheckingStrategy extends SpellcheckingStrategy {

  public Tokenizer getTokenizer(PsiElement element) {
    return EMPTY_TOKENIZER;
  }

  public boolean isMyContext(PsiElement element) {
    JsonProperty jsonProperty;
    if (!(element instanceof JsonStringLiteral)
            || !InfraUtils.hasFacets(element.getProject())
            || !InfraLibraryUtil.hasFrameworkLibrary(element.getProject())) {
      return false;
    }
    PsiFile file = element.getContainingFile();
    if (!InfraAdditionalConfigUtils.isAdditionalMetadataFile(file)
            || (jsonProperty = PsiTreeUtil.getParentOfType(element, JsonProperty.class)) == null) {
      return false;
    }
    String propertyName = jsonProperty.getName();
    return propertyName.equals(InfraMetadataConstant.NAME) || propertyName.equals(InfraMetadataConstant.REPLACEMENT);
  }
}
