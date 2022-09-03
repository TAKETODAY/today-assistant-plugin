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

import com.intellij.lang.ASTNode;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.intellij.lang.regexp.RegExpLanguage;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.util.InfraUtils;

public class InfraApplicationPropertiesValueRegexInjector implements MultiHostInjector {
  private static final String[] REGEX_PARTS = { "*", "$", "^", "+" };
  private static final Set<String> REGEX_CONFIG_KEYS = Set.of(
          "endpoints.configprops.keys-to-sanitize", "endpoints.env.keys-to-sanitize"
  );

  public void getLanguagesToInject(MultiHostRegistrar registrar, PsiElement context) {
    ASTNode valueNode;
    if (!InfraUtils.hasFacets(context.getProject())) {
      return;
    }
    PsiFile file = context.getContainingFile();
    if (!(file instanceof PropertiesFile)) {
      return;
    }
    PropertyImpl property = (PropertyImpl) context;
    String key = property.getKey();
    if (!REGEX_CONFIG_KEYS.contains(key) || !InfraConfigurationFileService.of().isApplicationConfigurationFile(file)) {
      return;
    }
    String text = property.getValue();
    if (StringUtil.isEmptyOrSpaces(text) || !isRegEx(text) || (valueNode = property.getValueNode()) == null) {
      return;
    }
    registrar.startInjecting(RegExpLanguage.INSTANCE).addPlace(null, null, property, TextRange.from(valueNode.getPsi().getStartOffsetInParent(), text.length())).doneInjecting();
  }

  private static boolean isRegEx(String value) {
    for (String part : REGEX_PARTS) {
      if (value.contains(part)) {
        return true;
      }
    }
    return false;
  }

  public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Collections.singletonList(PropertyImpl.class);
  }
}
