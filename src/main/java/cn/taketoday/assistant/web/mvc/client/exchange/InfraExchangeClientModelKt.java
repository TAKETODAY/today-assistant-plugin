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

import com.intellij.jam.JamService;
import com.intellij.jam.JavaLibraryUtils;
import com.intellij.microservices.utils.UrlMappingBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import cn.taketoday.lang.Nullable;

public final class InfraExchangeClientModelKt {

  public static boolean isExchangeClientAvailable(Module module) {
    return JavaLibraryUtils.hasLibraryClass(module, InfraExchangeConstant.SPRING_HTTP_EXCHANGE);
  }

  public static boolean isExchangeClientAvailable(Project project) {
    return JavaLibraryUtils.hasLibraryClass(project, InfraExchangeConstant.SPRING_HTTP_EXCHANGE);
  }

  public static Collection<InfraExchangeClient> findExchangeClients(Module module, GlobalSearchScope scope) {
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(module.getProject());
    GlobalSearchScope apiSearchScope = module.getModuleWithDependenciesAndLibrariesScope(false);

    LinkedHashSet<PsiClass> hosts = new LinkedHashSet<>();
    for (String annotation : InfraExchangeConstant.SPRING_EXCHANGE_METHOD_ANNOTATIONS) {
      for (PsiClass annotationClass : javaPsiFacade.findClasses(annotation, apiSearchScope)) {
        for (PsiMethod method : AnnotatedElementsSearch.searchPsiMethods(annotationClass, scope)) {
          PsiClass containingClass = method.getContainingClass();
          if (containingClass != null && containingClass.isInterface()) {
            hosts.add(containingClass);
          }
        }
      }
    }
    ArrayList<InfraExchangeClient> ret = new ArrayList<>();
    for (PsiClass element : hosts) {
      InfraExchangeClient jamElement = InfraExchangeClient.META.getJamElement(element);
      if (jamElement != null) {
        ret.add(jamElement);
      }
    }
    return ret;
  }

  public static List<InfraExchangeMapping<?>> getExchangeClientEndpoints(InfraExchangeClient group) {
    PsiClass psiElement = group.getPsiElement();
    JamService jamService = JamService.getJamService(psiElement.getProject());

    return jamService.getAnnotatedMembersList(
            psiElement, InfraExchangeMapping.MAPPING_JAM_KEY, JamService.CHECK_METHOD | JamService.CHECK_DEEP);
  }

  @Nullable
  public static String getFullUrlPath(@Nullable InfraExchangeClient group, @Nullable InfraExchangeMapping<?> infraExchangeMapping) {
    PsiClass psiElement;
    if (infraExchangeMapping == null) {
      return null;
    }
    HttpExchange.ClassMapping httpExchange =
            (group == null || (psiElement = group.getPsiElement()) == null)
            ? null : HttpExchange.ClassMapping.META.getJamElement(psiElement);
    return new UrlMappingBuilder(httpExchange != null ? httpExchange.getResourceValue() : null)
            .appendSegment(infraExchangeMapping.getResourceValue())
            .buildOrNull();
  }
}
