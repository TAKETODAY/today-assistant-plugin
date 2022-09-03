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

package cn.taketoday.assistant.web.mvc.config.anno;

import com.intellij.javaee.web.CommonServlet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.facet.InfraAutodetectedFileSet;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.service.InfraModelProvider;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.config.ServletFileSet;

public class AnnotationConfigInfraWebModelContributor implements InfraModelProvider {
  private static final String ABSTRACT_ANNOTATION_INITIALIZER = "cn.taketoday.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer";

  public String getName() {
    return getClass().getSimpleName();
  }

  public List<? extends InfraAutodetectedFileSet> getFilesets(InfraFacet springFacet) {
    Module module = springFacet.getModule();
    if (DumbService.isDumb(module.getProject()) || !InfraLibraryUtil.hasWebMvcLibrary(module)) {
      return Collections.emptyList();
    }
    PsiClass initializerClass = InfraUtils.findLibraryClass(module, ABSTRACT_ANNOTATION_INITIALIZER);
    if (initializerClass == null) {
      return Collections.emptyList();
    }
    GlobalSearchScope inheritorsScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false);
    Collection<PsiClass> initializers = ClassInheritorsSearch.search(initializerClass, inheritorsScope, true).findAll();
    SmartList<ServletFileSet> smartList = new SmartList<>();
    ContainerUtil.process(initializers, initializer -> {
      CodeConfigurationPropertiesParser parser = new CodeConfigurationPropertiesParser(initializer);
      String servletName = parser.getString("getServletName", true);
      if (StringUtil.isEmptyOrSpaces(servletName)) {
        return true;
      }
      PsiClass servletClass = InfraUtils.findLibraryClass(module, InfraMvcConstant.DISPATCHER_SERVLET_CLASS);
      PsiElement mappingDefinitionElement = ArrayUtil.getFirstElement(initializer.findMethodsByName("getServletMappings", false));
      String[] servletMappings = parser.getStringArray("getServletMappings", false);
      CommonServlet psiBasedServlet = new PsiBasedServlet(servletName, servletClass, initializer, mappingDefinitionElement, servletMappings);
      ServletFileSet servletFileSet = new ServletFileSet("initializer " + initializer.getQualifiedName() + " servlet context",
              InfraAppBundle.message("mvc.initializer.context.autodetected", servletName), psiBasedServlet, springFacet);
      List<PsiClass> servletConfigClasses = parser.getPsiClasses("getServletConfigClasses", false);
      boolean hasServletContext = !servletConfigClasses.isEmpty();
      for (PsiClass servletConfigClass : servletConfigClasses) {
        if (isConfigurationOrComponent(servletConfigClass)) {
          ServletFileSet.addInFileset(servletFileSet, servletConfigClass.getContainingFile());
        }
      }
      smartList.add(servletFileSet);
      CommonServlet rootContextImplicitServlet = hasServletContext ? null : psiBasedServlet;
      ServletFileSet servletFileSet2 = new ServletFileSet("initializer " + initializer.getQualifiedName() + " root context", "MVC " + servletName + " initializer root context",
              rootContextImplicitServlet, springFacet);
      for (PsiClass rootConfigClasses : parser.getPsiClasses("getRootConfigClasses", false)) {
        if (isConfigurationOrComponent(rootConfigClasses)) {
          ServletFileSet.addInFileset(servletFileSet2, rootConfigClasses.getContainingFile());
        }
      }
      servletFileSet.addDependency(servletFileSet2);
      smartList.add(servletFileSet2);
      return true;
    });
    return smartList;
  }

  private static boolean isConfigurationOrComponent(PsiClass psiClass) {
    return InfraUtils.isConfigurationOrMeta(psiClass) || InfraUtils.isComponentOrMeta(psiClass);
  }
}
