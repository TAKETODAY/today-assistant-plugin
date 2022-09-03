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
package cn.taketoday.assistant.model.xml.beans;

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.model.InfraProfile;
import cn.taketoday.assistant.model.converters.InfraProfileConverter;

/**
 * @author Sergey Vasiliev
 */
@Convert(InfraProfileConverter.class)
public abstract class InfraDomProfile implements GenericAttributeValue<List<String>>, InfraProfile, CommonModelElement {

  @Override
  public PsiElement getIdentifyingPsiElement() {
    return getXmlElement();
  }

  @Override
  public Set<String> getExpressions() {
    List<String> profileNames = getValue();
    if (ContainerUtil.isEmpty(profileNames)) {
      return Collections.singleton(DEFAULT_PROFILE_NAME);
    }

    return new LinkedHashSet<>(profileNames);
  }

  @Override
  public Set<String> getNames() {
    Set<String> result = new LinkedHashSet<>();
    for (String expression : getExpressions()) {
      expression = StringUtil.trimStart(expression, "!").trim();
      if (!expression.isEmpty()) {
        result.add(expression);
      }
    }
    return result;
  }

  @Override
  public boolean matches(Set<String> activeProfiles) {
    Set<String> profiles = getExpressions();
    if (InfraProfile.DEFAULT_PROFILE_NAME.equals(ContainerUtil.getOnlyItem(profiles)))
      return true;

    for (String profile : profiles) {
      if (profile.startsWith("!")) {
        String notProfile = profile.substring(1);
        if (!activeProfiles.contains(notProfile)) {
          return true;
        }
      }
      else if (activeProfiles.contains(profile)) {
        return true;
      }
    }
    return false;
  }
}
