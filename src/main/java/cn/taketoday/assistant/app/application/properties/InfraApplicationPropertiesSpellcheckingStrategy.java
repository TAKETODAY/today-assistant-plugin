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

package cn.taketoday.assistant.app.application.properties;

import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.Tokenizer;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.util.InfraUtils;

public class InfraApplicationPropertiesSpellcheckingStrategy extends SpellcheckingStrategy {

  public Tokenizer getTokenizer(PsiElement element) {
    return EMPTY_TOKENIZER;
  }

  public boolean isMyContext(PsiElement element) {
    if (!(element instanceof PropertyKeyImpl)
            || !InfraUtils.hasFacets(element.getProject())
            || !InfraLibraryUtil.hasFrameworkLibrary(element.getProject())) {
      return false;
    }
    PsiFile file = element.getContainingFile();
    return (file instanceof PropertiesFile) && InfraConfigurationFileService.of().isApplicationConfigurationFile(file);
  }
}
