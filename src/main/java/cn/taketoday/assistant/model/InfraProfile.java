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
package cn.taketoday.assistant.model;

import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.Set;

import cn.taketoday.lang.Nullable;

public interface InfraProfile {
  String DEFAULT_PROFILE_NAME = "_DEFAULT_PROFILE_NAME_";
  String DEFAULT_TEST_PROFILE_NAME = "_DEFAULT_TEST_PROFILE_NAME_";
  Set<String> DEFAULT_PROFILE_NAMES = ContainerUtil.set(DEFAULT_PROFILE_NAME, DEFAULT_TEST_PROFILE_NAME);

  InfraProfile DEFAULT = new InfraProfile() {

    private final Set<String> ourDefaultNames = Collections.singleton(DEFAULT_PROFILE_NAME);

    @Override
    public PsiElement getIdentifyingPsiElement() {
      return null;
    }

    @Override
    public Set<String> getNames() {
      return ourDefaultNames;
    }

    @Override
    public Set<String> getExpressions() {
      return ourDefaultNames;
    }

    @Override
    public boolean matches(Set<String> activeProfiles) {
      return true;
    }
  };

  @Nullable
  PsiElement getIdentifyingPsiElement();

  Set<String> getNames();

  Set<String> getExpressions();

  boolean matches(Set<String> activeProfiles);
}
