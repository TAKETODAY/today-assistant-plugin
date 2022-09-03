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
package cn.taketoday.assistant.profiles;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;

import cn.taketoday.assistant.model.jam.profile.InfraJamProfile;

/**
 * @author Sergey Vasiliev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public final class InfraProfilesReferenceProvider extends PsiReferenceProvider {

  private final InfraJamProfile.InfraProfileConverter myDelegate = new InfraJamProfile.InfraProfileConverter("", false);

  @Override
  public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
    return element instanceof PsiLanguageInjectionHost ?
           myDelegate.createReferences((PsiLanguageInjectionHost) element) :
           PsiReference.EMPTY_ARRAY;
  }
}