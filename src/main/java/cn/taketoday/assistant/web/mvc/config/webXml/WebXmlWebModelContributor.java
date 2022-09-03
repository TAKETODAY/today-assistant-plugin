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

package cn.taketoday.assistant.web.mvc.config.webXml;

import com.intellij.javaee.model.xml.ParamValue;
import com.intellij.javaee.web.CommonServlet;
import com.intellij.javaee.web.WebDirectoryElement;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.model.xml.Servlet;
import com.intellij.javaee.web.model.xml.WebApp;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PackageScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.SmartList;
import com.intellij.util.text.StringTokenizer;
import com.intellij.util.xml.DomUtil;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.beans.stereotype.InfraJamModel;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.facet.InfraAutodetectedFileSet;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;
import cn.taketoday.assistant.service.InfraModelProvider;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.config.ServletFileSet;
import cn.taketoday.lang.Nullable;

public class WebXmlWebModelContributor implements InfraModelProvider {
  @NonNls
  private static final String APPLICATION_CONTEXT_FILESET = "web application context";

  public String getName() {
    return getClass().getSimpleName();
  }

  public List<? extends InfraAutodetectedFileSet> getFilesets(InfraFacet springFacet) {
    VirtualFile webInf;
    InfraAutodetectedFileSet servletContextFileSet;
    Module module = springFacet.getModule();
    if (DumbService.isDumb(module.getProject()) || !InfraLibraryUtil.hasWebMvcLibrary(module)) {
      return Collections.emptyList();
    }
    List<InfraAutodetectedFileSet> result = new ArrayList<>();
    for (WebFacet webFacet : WebFacet.getInstances(module)) {
      WebApp webApp = webFacet.getRoot();
      if (webApp != null && (webInf = getWebInf(webFacet)) != null) {
        SmartList<InfraFileSet> smartList = new SmartList<>();
        for (Servlet servlet : webApp.getServlets()) {
          String servletClass = servlet.getServletClass().getStringValue();
          String servletName = servlet.getServletName().getValue();
          if (servletClass != null && servletName != null && InheritanceUtil.isInheritor(servlet.getServletClass().getValue(), InfraMvcConstant.DISPATCHER_SERVLET_CLASS)) {
            ParamValue param = DomUtil.findByName(servlet.getInitParams(), InfraMvcConstant.CONTEXT_CONFIG_LOCATION);
            String id = "web " + servletName + " servlet context";
            String name = InfraAppBundle.message("mvc.servlet.context.autodetected", servletName);
            if (param != null) {
              servletContextFileSet = createFileSet(param, id, name, springFacet, servlet, isAnnotationConfig(servlet.getInitParams()));
            }
            else {
              servletContextFileSet = createServletFileSet(id, name, springFacet, servlet, false);
              servletContextFileSet.addFile(webInf.getUrl() + "/" + getServletContextFileName(servletName));
            }
            smartList.add(servletContextFileSet);
          }
        }
        if (!smartList.isEmpty()) {
          InfraAutodetectedFileSet appContext = null;
          String filesetName = InfraAppBundle.message("mvc.application.context.autodetected");
          ParamValue param2 = DomUtil.findByName(webApp.getContextParams(), InfraMvcConstant.CONTEXT_CONFIG_LOCATION);
          if (param2 != null) {
            boolean isAnnotationConfig = isAnnotationConfig(webApp.getContextParams());
            appContext = createFileSet(param2, APPLICATION_CONTEXT_FILESET, filesetName, springFacet, null, isAnnotationConfig);
          }
          else {
            VirtualFile file = webInf.findChild(InfraMvcConstant.APPLICATION_CONTEXT_XML);
            if (file != null) {
              appContext = createServletFileSet(APPLICATION_CONTEXT_FILESET, filesetName, springFacet, null, false);
              appContext.addFile(file);
            }
          }
          if (appContext != null) {
            result.add(appContext);
            for (InfraFileSet springFileSet : smartList) {
              springFileSet.addDependency(appContext);
            }
          }
          result.addAll((Collection) smartList);
        }
      }
    }
    return result;
  }

  @NonNls
  private static String getServletContextFileName(String servletName) {
    return servletName + "-servlet.xml";
  }

  @Nullable
  private static VirtualFile getWebInf(WebFacet facet) {
    WebDirectoryElement webInf;
    if (!facet.isDisposed() && (webInf = WebUtil.getWebUtil().findWebDirectoryElement(InfraMvcConstant.WEB_INF, facet)) != null) {
      return webInf.getVirtualFile();
    }
    return null;
  }

  private static boolean isAnnotationConfig(List<? extends ParamValue> paramValues) {
    ParamValue param = DomUtil.findByName(paramValues, InfraMvcConstant.CONTEXT_CLASS_PARAM_NAME);
    if (param != null) {
      return InfraMvcConstant.ANNOTATION_CONFIG_CLASS.equals(param.getParamValue().getStringValue());
    }
    return false;
  }

  private static InfraAutodetectedFileSet createFileSet(ParamValue param, String id, String name, InfraFacet springFacet, @Nullable Servlet servlet, boolean isAnnotationConfig) {
    InfraAutodetectedFileSet fileSet = createServletFileSet(id, name, springFacet, servlet, isAnnotationConfig);
    if (isAnnotationConfig) {
      String paramValue = param.getParamValue().getStringValue();
      if (StringUtil.isEmptyOrSpaces(paramValue)) {
        return fileSet;
      }
      Module module = springFacet.getModule();
      JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(module.getProject());
      GlobalSearchScope moduleScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false);
      StringTokenizer tokenizer = new StringTokenizer(paramValue, " ,;");
      while (tokenizer.hasMoreElements()) {
        String configText = tokenizer.nextElement().trim();
        if (!StringUtil.isEmptyOrSpaces(configText)) {
          PsiPackage psiPackage = javaPsiFacade.findPackage(configText);
          if (psiPackage != null) {
            GlobalSearchScope searchScope = moduleScope.intersectWith(PackageScope.packageScope(psiPackage, true));
            List<Configuration> springConfigurations = InfraJamModel.from(module).getConfigurations(searchScope);
            for (Configuration springConfiguration : springConfigurations) {
              ServletFileSet.addInFileset(fileSet, springConfiguration.getContainingFile());
            }
            ((AnnotationServletFileSet) fileSet).addComponentScanPackage(psiPackage.getQualifiedName());
          }
          else {
            PsiClass psiClass = javaPsiFacade.findClass(configText, moduleScope);
            if (psiClass != null) {
              ServletFileSet.addInFileset(fileSet, psiClass.getContainingFile());
            }
          }
        }
      }
    }
    else {
      XmlElement tag = param.getParamValue().getXmlElement();
      if (tag != null) {
        Stream<PsiReference> stream = Arrays.stream(tag.getReferences());
        PsiReference[] references = stream.filter(FileReference.class::isInstance).toArray(PsiReference[]::new);
        Collection<PsiFileSystemItem> files = ResourcesUtil.of().getResourceItems(references, InfraDomUtils.XML_CONDITION);
        for (PsiFileSystemItem file : files) {
          fileSet.addFile(file.getVirtualFile());
        }
      }
    }
    return fileSet;
  }

  protected static InfraAutodetectedFileSet createServletFileSet(@NonNls String id, String name, InfraFacet springFacet, @Nullable CommonServlet servlet, boolean isAnnotated) {
    if (isAnnotated) {
      return new AnnotationServletFileSet(id, name, servlet, springFacet);
    }
    return new ServletFileSet(id, name, servlet, springFacet);
  }
}
