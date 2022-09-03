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

package cn.taketoday.assistant.web.mvc;

import com.intellij.javaee.web.CustomServletReferenceAdapter;
import com.intellij.javaee.web.ServletMappingInfo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.web.mvc.services.WebMvcService;
import cn.taketoday.lang.Nullable;

public class WebMvcCustomServletReferenceProvider extends CustomServletReferenceAdapter {

  protected PsiReference[] createReferences(PsiElement element, int offset, String text, @Nullable ServletMappingInfo info, boolean soft) {
    TextRange nameRange;
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null || !InfraLibraryUtil.hasWebMvcLibrary(module)) {
      return PsiReference.EMPTY_ARRAY;
    }
    if (WebMvcService.getServletModels(module).isEmpty()) {
      return PsiReference.EMPTY_ARRAY;
    }
    if (info != null) {
      TextRange infoNameRange = info.getNameRange(text);
      if (infoNameRange == null) {
        return PsiReference.EMPTY_ARRAY;
      }
      nameRange = infoNameRange.shiftRight(offset);
    }
    else {
      nameRange = TextRange.from(offset, text.length());
    }
    return new PsiReference[] { new WebMVCReference(element, nameRange, text, soft) };
  }

  public PathReference createWebPath(String path, PsiElement element, ServletMappingInfo info) {
    return null;
  }
}
