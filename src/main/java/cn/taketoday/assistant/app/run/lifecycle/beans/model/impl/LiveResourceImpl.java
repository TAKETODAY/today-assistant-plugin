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

package cn.taketoday.assistant.app.run.lifecycle.beans.model.impl;

import com.intellij.execution.JavaExecutionUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.navigation.AnonymousElementProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FilePathReferenceProvider;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.reference.SoftReference;
import com.intellij.util.PathUtil;
import com.intellij.util.SmartList;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.app.run.InfraRunBundle;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBean;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveContext;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveResource;
import cn.taketoday.lang.Nullable;
import icons.JavaUltimateIcons;

class LiveResourceImpl implements LiveResource {
  static final String AUTO_CONFIGURATION_RESOURCE = "null";
  private static final String CLASS_PATH_RESOURCE_DESCRIPTION = "class path resource";
  private static final String FILE_RESOURCE_DESCRIPTION = "file";
  private static final String URL_RESOURCE_DESCRIPTION = "URL";
  private static final String CLASS_DOT_EXTENSION = ".class";
  private static final String XML_DOT_EXTENSION = ".xml";

  private final String myDescription;

  private final LiveContext myContext;

  private final List<LiveBean> myBeans;
  private volatile String myDisplayName;
  private volatile WeakReference<Navigatable> myNavigatableRef;

  LiveResourceImpl(String description, LiveContext context) {
    this.myBeans = new SmartList();
    this.myDescription = description;
    this.myContext = context;
  }

  @Override

  public String getDescription() {
    String str = this.myDescription;
    return str;
  }

  @Override

  public LiveContext getContext() {
    LiveContext liveContext = this.myContext;
    return liveContext;
  }

  @Override

  public List<LiveBean> getBeans() {
    SmartList smartList = new SmartList();
    smartList.addAll(this.myBeans);
    return smartList;
  }

  @Override
  public boolean hasDescription() {
    return !this.myDescription.isEmpty() && !this.myDescription.equalsIgnoreCase(AUTO_CONFIGURATION_RESOURCE);
  }

  @Override

  public String getDisplayName() {
    String displayName = this.myDisplayName;
    if (displayName != null) {
      return displayName;
    }
    else if (!hasDescription()) {
      this.myDisplayName = InfraRunBundle.message("infra.application.endpoints.bean.auto.configuration");
      String str = this.myDisplayName;
      return str;
    }
    else {
      String displayName2 = this.myDescription;
      int indexStart = displayName2.lastIndexOf('/');
      int indexEnd = displayName2.lastIndexOf(']');
      if (indexStart > -1 && indexEnd > -1 && indexStart < indexEnd) {
        displayName2 = displayName2.substring(indexStart + 1, indexEnd);
      }
      if (isXmlResource(displayName2)) {
        this.myDisplayName = displayName2;
        String str2 = this.myDisplayName;
        return str2;
      }
      String displayName3 = StringUtil.trimEnd(displayName2, CLASS_DOT_EXTENSION);
      int index = displayName3.lastIndexOf('.');
      if (index > -1) {
        displayName3 = displayName3.substring(index + 1);
      }
      String[] names = displayName3.split("\\$");
      StringBuilder sb = new StringBuilder();
      sb.append(names[names.length - 1]);
      for (int i = names.length - 2; i >= 0; i--) {
        sb.append(" in ");
        sb.append(names[i]);
      }
      this.myDisplayName = sb.toString();
      String str3 = this.myDisplayName;
      return str3;
    }
  }

  @Override
  @Nullable
  public Navigatable findResourceNavigatable(Project project, @Nullable Module module, @Nullable PsiElement runConfigurationElement, GlobalSearchScope searchScope) {
    Navigatable navigatable;
    if (!hasDescription()) {
      return null;
    }
    Navigatable navigatable2 = getCachedValue();
    if (navigatable2 != null) {
      return navigatable2;
    }
    if (isClassNameResource(this.myDescription)) {
      navigatable = findClass(this.myDescription, project, searchScope);
    }
    else {
      String path = getResourcePath();
      if (this.myDescription.startsWith(CLASS_PATH_RESOURCE_DESCRIPTION)) {
        navigatable = getClassPathResource(path, project, module, runConfigurationElement, searchScope);
      }
      else if (this.myDescription.startsWith(FILE_RESOURCE_DESCRIPTION)) {
        navigatable = getFileResource(path, project, searchScope);
      }
      else if (this.myDescription.startsWith(URL_RESOURCE_DESCRIPTION)) {
        navigatable = new UrlNavigatable(path);
      }
      else {
        navigatable = null;
      }
    }
    this.myNavigatableRef = new WeakReference<>(navigatable);
    return navigatable;
  }

  @Nullable
  private Navigatable getCachedValue() {
    Navigatable psiElement = SoftReference.dereference(this.myNavigatableRef);
    if (psiElement != null) {
      if (psiElement instanceof PsiElement element) {
        if (element.isValid()) {
          if (psiElement instanceof PsiClass) {
            if (matchesClass((PsiClass) psiElement, false)) {
              return psiElement;
            }
            return null;
          }
          else if (psiElement instanceof XmlFile) {
            if (matchesXmlConfig((XmlFile) psiElement)) {
              return psiElement;
            }
            return null;
          }
          else {
            return psiElement;
          }
        }
        return null;
      }
      return psiElement;
    }
    return null;
  }

  @Override
  @Nullable
  public PsiElement findResourceElement(Project project, @Nullable Module module, @Nullable PsiElement runConfigurationElement, GlobalSearchScope searchScope) {
    Navigatable findResourceNavigatable = findResourceNavigatable(project, module, runConfigurationElement, searchScope);
    if (findResourceNavigatable instanceof PsiElement psiElement) {
      return psiElement;
    }
    return null;
  }

  @Override
  @Nullable
  public Icon getIcon() {
    if (!hasDescription()) {
      return Icons.SpringJavaConfig;
    }
    if (isClassNameResource(this.myDescription)) {
      return AllIcons.Nodes.Class;
    }
    String path = getResourcePath();
    if (this.myDescription.startsWith(CLASS_PATH_RESOURCE_DESCRIPTION) || this.myDescription.startsWith(FILE_RESOURCE_DESCRIPTION)) {
      if (isClassResource(path)) {
        return AllIcons.Nodes.Class;
      }
      if (isXmlResource(path)) {
        return Icons.SpringConfig;
      }
      return null;
    }
    else if (this.myDescription.startsWith(URL_RESOURCE_DESCRIPTION)) {
      return JavaUltimateIcons.Javaee.Web_xml;
    }
    else {
      return null;
    }
  }

  @Override

  public String getResourcePath() {
    String resource = this.myDescription;
    int start = resource.indexOf('[');
    int end = resource.indexOf(']');
    if (start > -1 && end > -1 && start < end) {
      resource = resource.substring(start + 1, end);
    }
    String str = resource;
    return str;
  }

  @Override
  public boolean matchesClass(PsiClass psiClass, boolean strict) {
    if (!hasDescription()) {
      return false;
    }
    String className = null;
    if (isClassNameResource(this.myDescription)) {
      className = this.myDescription;
    }
    else {
      String path = getResourcePath();
      if (!isClassResource(path) || this.myDescription.startsWith(URL_RESOURCE_DESCRIPTION)) {
        return false;
      }
      if (this.myDescription.startsWith(CLASS_PATH_RESOURCE_DESCRIPTION)) {
        className = StringUtil.trimEnd(path, CLASS_DOT_EXTENSION, true).replaceAll("[\\\\/$]", ".");
      }
      else if (this.myDescription.startsWith(FILE_RESOURCE_DESCRIPTION)) {
        String innerClassesSuffix = null;
        int index = path.indexOf(36);
        if (index >= 0) {
          innerClassesSuffix = StringUtil.trimEnd(path.substring(index + 1), CLASS_DOT_EXTENSION, true);
          path = path.substring(0, index) + path.substring(path.length() - CLASS_DOT_EXTENSION.length());
        }
        VirtualFile resourceFile = VfsUtil.findFileByIoFile(new File(path), false);
        if (resourceFile == null || !resourceFile.isValid()) {
          return false;
        }
        PsiFile findFile = psiClass.getManager().findFile(resourceFile);
        if (findFile instanceof PsiClassOwner classOwner) {
          className = classOwner.getPackageName() + "." + FileUtilRt.getNameWithoutExtension(findFile.getName());
        }
        if (className != null && innerClassesSuffix != null) {
          className = className + "." + StringUtil.replace(innerClassesSuffix, "$", ".");
        }
      }
    }
    if (className == null) {
      return false;
    }
    if (className.equals(psiClass.getQualifiedName())) {
      return true;
    }
    if (strict) {
      return false;
    }
    PsiClass resourceClass = JavaPsiFacade.getInstance(psiClass.getProject()).findClass(className, psiClass.getResolveScope());
    return InheritanceUtil.isInheritorOrSelf(resourceClass, psiClass, true);
  }

  @Override
  public boolean matchesXmlConfig(XmlFile xmlFile) {
    VirtualFile elementFile;
    if (hasDescription() && !isClassNameResource(this.myDescription) && !this.myDescription.startsWith(URL_RESOURCE_DESCRIPTION)) {
      String path = getResourcePath();
      if (isClassResource(path) || (elementFile = xmlFile.getOriginalFile().getVirtualFile()) == null) {
        return false;
      }
      if (this.myDescription.startsWith(FILE_RESOURCE_DESCRIPTION)) {
        VirtualFile resourceFile = VfsUtil.findFileByIoFile(new File(path), false);
        if (resourceFile != null && resourceFile.isValid()) {
          return resourceFile.equals(elementFile);
        }
        return false;
      }
      else if (this.myDescription.startsWith(CLASS_PATH_RESOURCE_DESCRIPTION)) {
        String elementPath = elementFile.getPath();
        return PathUtil.toSystemIndependentName(elementPath).endsWith(PathUtil.toSystemIndependentName(path));
      }
      else {
        return false;
      }
    }
    return false;
  }

  void addBean(LiveBean bean) {
    this.myBeans.add(bean);
  }

  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof LiveResourceImpl liveResource) {
      return this.myDescription.equals(liveResource.myDescription) && this.myContext.equals(liveResource.myContext);
    }
    return false;
  }

  public int hashCode() {
    int result = (31 * 17) + this.myDescription.hashCode();
    return (31 * result) + this.myContext.hashCode();
  }

  public String toString() {
    return this.myDescription;
  }

  static boolean isClassNameResource(String resourceDescription) {
    return resourceDescription.indexOf(91) < 0;
  }

  static boolean isXmlResource(String path) {
    return StringUtil.endsWithIgnoreCase(path, XML_DOT_EXTENSION);
  }

  static boolean isClassResource(String path) {
    return StringUtil.endsWithIgnoreCase(path, CLASS_DOT_EXTENSION);
  }

  static PsiClass findClass(String className, Project project, GlobalSearchScope searchScope) {
    String innerClassesSuffix = null;
    int index = className.indexOf(36);
    if (index >= 0) {
      innerClassesSuffix = className.substring(index + 1);
      className = className.substring(0, index);
    }
    PsiClass psiClass = JavaExecutionUtil.findMainClass(project, className, searchScope);
    if (psiClass == null || innerClassesSuffix == null) {
      return psiClass;
    }
    return findInnerClass(psiClass, innerClassesSuffix);
  }

  private static PsiClass findInnerClass(PsiClass parent, String innerClassesSuffix) {
    String[] innerClasses = innerClassesSuffix.split("\\$");
    for (String innerClassName : innerClasses) {
      if (innerClassName.isEmpty()) {
        return parent;
      }
      if (StringUtil.isDecimalDigit(innerClassName.charAt(0))) {
        try {
          int classIndex = Integer.parseInt(innerClassName) - 1;
          for (AnonymousElementProvider provider : AnonymousElementProvider.EP_NAME.getExtensionList()) {
            PsiElement[] elements = provider.getAnonymousElements(parent);
            if (classIndex < elements.length) {
              PsiElement anonymousElement = elements[classIndex];
              if (!(anonymousElement instanceof PsiClass)) {
                return parent;
              }
              parent = (PsiClass) anonymousElement;
            }
          }
        }
        catch (NumberFormatException e) {
          return parent;
        }
      }
      else {
        PsiClass innerClass = parent.findInnerClassByName(innerClassName, false);
        if (innerClass == null) {
          return parent;
        }
        parent = innerClass;
      }
    }
    return parent;
  }

  private static NavigatablePsiElement getClassPathResource(String path, Project project, @Nullable Module module, @Nullable PsiElement runConfigurationElement,
          GlobalSearchScope searchScope) {
    PsiReference reference;
    PsiElement element;
    if (isClassResource(path)) {
      return findClass(StringUtil.trimEnd(path, CLASS_DOT_EXTENSION, true).replaceAll("[\\\\/]", "."), project, searchScope);
    }
    if (module == null || runConfigurationElement == null) {
      return null;
    }
    PsiReference[] references = new FilePathReferenceProvider().getReferencesByElement(runConfigurationElement, path, 0, true, module);
    if (references.length > 0 && (reference = references[references.length - 1]) != null && (element = reference.resolve()) != null) {
      return element.getContainingFile();
    }
    return null;
  }

  private static NavigatablePsiElement getFileResource(String path, Project project, GlobalSearchScope searchScope) {
    int index;
    String innerClassesSuffix = null;
    if (isClassResource(path) && (index = path.indexOf(36)) >= 0) {
      innerClassesSuffix = StringUtil.trimEnd(path.substring(index + 1), CLASS_DOT_EXTENSION, true);
      path = path.substring(0, index) + path.substring(path.length() - CLASS_DOT_EXTENSION.length());
    }
    VirtualFile resourceFile = VfsUtil.findFileByIoFile(new File(path), false);
    if (resourceFile != null && resourceFile.isValid()) {
      PsiFile findFile = PsiManager.getInstance(project).findFile(resourceFile);
      if (findFile instanceof PsiClassOwner classOwner) {
        String className = classOwner.getPackageName() + "." + FileUtilRt.getNameWithoutExtension(findFile.getName());
        PsiClass psiClass = JavaExecutionUtil.findMainClass(project, className, searchScope);
        if (psiClass != null && innerClassesSuffix != null) {
          psiClass = findInnerClass(psiClass, innerClassesSuffix);
        }
        if (psiClass != null) {
          return psiClass;
        }
      }
      return findFile;
    }
    return null;
  }

  private static class UrlNavigatable implements Navigatable {

    private final String myUrl;

    UrlNavigatable(String url) {
      this.myUrl = url;
    }

    public void navigate(boolean requestFocus) {
      BrowserUtil.browse(this.myUrl);
    }

    public boolean canNavigate() {
      return true;
    }

    public boolean canNavigateToSource() {
      return false;
    }

    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj instanceof UrlNavigatable) {
        return this.myUrl.equals(((UrlNavigatable) obj).myUrl);
      }
      return false;
    }

    public int hashCode() {
      return this.myUrl.hashCode();
    }
  }
}
