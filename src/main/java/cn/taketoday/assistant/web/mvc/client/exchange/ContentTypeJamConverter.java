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

package cn.taketoday.assistant.web.mvc.client.exchange;

import com.intellij.jam.JamConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.microservices.mime.MimeTypeReference;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;

import cn.taketoday.lang.Nullable;

public final class ContentTypeJamConverter extends JamConverter<String> {

  @Nullable
  public String fromString(@Nullable String s, @Nullable JamStringAttributeElement<String> jamStringAttributeElement) {
    return s;
  }

  public PsiReference[] createReferences(JamStringAttributeElement<String> jamStringAttributeElement, PsiLanguageInjectionHost injectionHost) {
    return MimeTypeReference.Companion.forElement(injectionHost);
  }
}
