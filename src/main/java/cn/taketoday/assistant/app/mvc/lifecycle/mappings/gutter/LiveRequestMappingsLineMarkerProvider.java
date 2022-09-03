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

package cn.taketoday.assistant.app.mvc.lifecycle.mappings.gutter;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.SmartList;

import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UIdentifier;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.RequestMappingsEndpoint;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.gutter.LiveRequestMappingsNavigationHandler.MethodNavigationItem;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMapping;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMappingsModel;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationServerConfiguration;
import cn.taketoday.assistant.gutter.GutterIconBuilder;
import cn.taketoday.assistant.settings.InfraGeneralSettings;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.request.InfraMergingMvcRequestMappingLineMarkerProvider;
import cn.taketoday.lang.Nullable;

public class LiveRequestMappingsLineMarkerProvider implements InfraMergingMvcRequestMappingLineMarkerProvider {

  public boolean collectLineMarkers(PsiElement psiElement, Collection<RelatedItemLineMarkerInfo<PsiElement>> result) {
    PsiMethod psiMethod;
    String applicationUrl;
    if (!hasNotLiveMappingInProject(psiElement.getProject()) && (psiMethod = getRequestMappingMethod(psiElement)) != null && !psiMethod.isConstructor()) {
      PsiClass psiClass = psiMethod.getContainingClass();
      if (!InfraUtils.isBeanCandidateClass(psiClass)) {
        return false;
      }
      Project project = psiMethod.getProject();
      SmartList<MethodNavigationItem> smartList = new SmartList<>();
      Collection<InfraApplicationInfo> infos = InfraApplicationLifecycleManager.from(project).getInfraApplicationInfos();
      for (InfraApplicationInfo info : infos) {
        LiveRequestMappingsModel mappingsModel = info.getEndpointData(RequestMappingsEndpoint.getInstance()).getValue();
        if (mappingsModel != null && (applicationUrl = info.getApplicationUrl().getValue()) != null) {
          InfraApplicationServerConfiguration applicationServerConfiguration = info.getServerConfiguration().getValue();
          String servletPath = applicationServerConfiguration == null ? null : applicationServerConfiguration.getServletPath();
          List<LiveRequestMapping> mappings = mappingsModel.getRequestMappingsByMethod(psiMethod);
          if (!mappings.isEmpty()) {
            smartList.add(new MethodNavigationItem(project, info, applicationUrl, servletPath, mappings, "ICON_NAVIGATION"));
          }
        }
      }
      if (smartList.isEmpty()) {
        return false;
      }
      LiveRequestMappingsNavigationHandler navigationHandler = new LiveRequestMappingsNavigationHandler(smartList);
      GutterIconBuilder.CustomNavigationHandlerBuilder<PsiElement> builder = GutterIconBuilder.CustomNavigationHandlerBuilder.createBuilder(Icons.Gutter.RequestMapping,
              navigationHandler.getLiveMarkerInfoTooltipText(), navigationHandler, null);
      result.add(builder.withElementPresentation(InfraAppBundle.message("infra.live.request.mapping.gutter.name"))
              .withAdditionalGotoRelatedItems(navigationHandler.getRelatedItems(psiMethod))
              .createRelatedMergeableLineMarkerInfo(psiElement));
      return true;
    }
    return false;
  }

  @Nullable
  private static PsiMethod getRequestMappingMethod(PsiElement psiElement) {
    UIdentifier uIdentifier = UastContextKt.toUElement(psiElement, UIdentifier.class);
    if (uIdentifier == null) {
      return null;
    }
    UElement uParentForIdentifier = UastUtils.getUParentForIdentifier(psiElement);
    if (!(uParentForIdentifier instanceof UMethod uMethod)) {
      return null;
    }
    return UElementKt.getAsJavaPsiElement(uMethod, PsiMethod.class);
  }

  private static boolean hasNotLiveMappingInProject(Project project) {
    if ((!InfraUtils.hasFacets(project)
            && !InfraGeneralSettings.from(project).isAllowAutoConfigurationMode())
            || !InfraLibraryUtil.hasFrameworkLibrary(project)
            || !InfraLibraryUtil.hasWebMvcLibrary(project)) {
      return true;
    }
    Collection<InfraApplicationInfo> infos = InfraApplicationLifecycleManager.from(project).getInfraApplicationInfos();
    return infos.stream().noneMatch(info -> {
      return info.getEndpointData(RequestMappingsEndpoint.getInstance()).getValue() != null;
    });
  }
}
