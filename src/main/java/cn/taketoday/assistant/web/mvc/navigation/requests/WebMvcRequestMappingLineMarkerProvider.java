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

package cn.taketoday.assistant.web.mvc.navigation.requests;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;

import java.util.Collection;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.request.InfraMergingMvcRequestMappingLineMarkerProvider;
import kotlin.collections.CollectionsKt;

public final class WebMvcRequestMappingLineMarkerProvider extends RelatedItemLineMarkerProvider {
  private static final ExtensionPointName<InfraMergingMvcRequestMappingLineMarkerProvider> EP_NAME
          = ExtensionPointName.create("cn.taketoday.assistant.mergingMvcRequestMappingLineMarkerProvider");

  public Icon getIcon() {
    return Icons.Gutter.RequestMapping;
  }

  public String getId() {
    return "InfraMvcRequestMappingLineMarkerProvider";
  }

  public String getName() {
    return InfraAppBundle.message("request.mapping.gutter.name");
  }

  public void collectNavigationMarkers(List<? extends PsiElement> list, Collection<? super RelatedItemLineMarkerInfo<?>> collection, boolean forNavigation) {
    PsiElement first = CollectionsKt.firstOrNull(list);
    if (first == null || !InfraUtils.isEnabledModule(ModuleUtilCore.findModuleForPsiElement(first))) {
      return;
    }
    super.collectNavigationMarkers(list, collection, forNavigation);
  }

  protected void collectNavigationMarkers(PsiElement element, Collection collection) {
    for (InfraMergingMvcRequestMappingLineMarkerProvider provider : EP_NAME.getExtensionList()) {
      if (provider.collectLineMarkers(element, collection)) {
        return;
      }
    }
  }

}
