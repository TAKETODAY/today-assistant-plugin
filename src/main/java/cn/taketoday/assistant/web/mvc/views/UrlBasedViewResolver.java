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

package cn.taketoday.assistant.web.mvc.views;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.javaee.web.WebDirectoryElement;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Stack;

import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.assistant.model.utils.resources.InfraResourcesBuilder;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;
import cn.taketoday.lang.Nullable;

public class UrlBasedViewResolver extends WithPrefixSuffix {

  private final Module myModule;

  private final String myPath;

  private final String myPrefix;

  private final String mySuffix;

  private final String myID;

  private final String myPathAndPrefix;

  public UrlBasedViewResolver(Module module, String ID, @Nullable String path, @Nullable String prefix, @Nullable String suffix) {
    this.myModule = module;
    this.myID = ID;
    this.myPath = StringUtil.notNullize(path);
    this.myPrefix = StringUtil.notNullize(prefix);
    this.mySuffix = StringUtil.notNullize(suffix);
    this.myPathAndPrefix = this.myPath + this.myPrefix;
  }

  private boolean isClasspathPrefix() {
    return StringUtil.startsWith(this.myPathAndPrefix, "classpath:");
  }

  @Override
  @TestOnly

  public String getID() {
    String str = this.myID;
    return str;
  }

  @Override

  public Set<PsiElement> resolveView(String viewName) {
    if (isClasspathPrefix()) {
      PsiElement viewForClasspath = resolveViewForClasspath(viewName);
      return viewForClasspath == null ? Collections.emptySet() : Collections.singleton(viewForClasspath);
    }
    return getWebDirectoryElements(calculatePath(viewName))
            .stream()
            .map(WebDirectoryElement::getOriginalFile)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
  }

  public Module getModule() {
    return this.myModule;
  }

  private Set<WebDirectoryElement> getWebDirectoryElements(String path) {
    Set<WebDirectoryElement> elements = new HashSet<>();
    for (WebFacet webFacet : WebFacet.getInstances(myModule)) {
      ContainerUtil.addIfNotNull(elements, WebUtil.getWebUtil().findWebDirectoryElement(path, webFacet));
    }
    return elements;
  }

  private String calculatePath(String viewName) {
    return joinPaths(this.myPath, joinPaths(this.myPrefix, viewName + this.mySuffix, false), true);
  }

  private static String joinPaths(String path, String fileName, boolean forceSlash) {
    if (path.isEmpty()) {
      return fileName;
    }
    if (path.endsWith("/") && fileName.startsWith("/")) {
      return path + fileName.substring(1);
    }
    if (forceSlash && !path.endsWith("/") && !fileName.startsWith("/")) {
      return path + "/" + fileName;
    }
    return path + fileName;
  }

  @Nullable
  private PsiElement resolveViewForClasspath(String viewName) {
    InfraResourcesBuilder builder = InfraResourcesBuilder.create(myModule, calculatePath(viewName), 1);
    PsiReference[] references = ResourcesUtil.of().getReferences(builder);
    Collection<PsiFileSystemItem> items = ResourcesUtil.of().getResourceItems(references, Conditions.alwaysTrue());
    return ContainerUtil.getFirstItem(items);
  }

  @Override
  public List<LookupElement> getAllResolverViews() {
    return getAllResolverViews(Collections.emptyList());
  }

  public List<LookupElement> getAllResolverViews(List<String> pathSegments) {
    if (isClasspathPrefix()) {
      return getAllViewsForClasspath(pathSegments);
    }
    Set<WebDirectoryElement> webDirectoryElements = getWebDirectoryElements(this.myPathAndPrefix);
    List<LookupElement> lookupElements = new ArrayList<>();
    CommonProcessors.CollectProcessor<LookupElement> collectProcessor = new CommonProcessors.CollectProcessor<>(lookupElements);
    for (WebDirectoryElement webDirectoryElement : webDirectoryElements) {
      VirtualFile file = webDirectoryElement.getOriginalVirtualFile();
      if (file != null) {
        for (VirtualFile child : file.getChildren()) {
          if (this.myPathAndPrefix.isEmpty()) {
            new MyDirectoryLookupWalker(pathSegments, collectProcessor).visitFile(child);
          }
          else {
            VfsUtilCore.visitChildrenRecursively(child, new MyDirectoryLookupWalker(pathSegments, collectProcessor));
          }
        }
      }
    }
    return lookupElements;
  }

  private List<LookupElement> getAllViewsForClasspath(List<String> pathSegments) {
    InfraResourcesBuilder builder = InfraResourcesBuilder.create(myModule, this.myPathAndPrefix, 1).endingSlashNotAllowed(false);
    PsiReference[] references = ResourcesUtil.of().getReferences(builder);
    Collection<PsiFileSystemItem> directories = ResourcesUtil.of().getResourceItems(references, PsiFileSystemItem::isDirectory);
    SmartList smartList = new SmartList();
    CommonProcessors.CollectProcessor<LookupElement> collectProcessor = new CommonProcessors.CollectProcessor<>(smartList);
    for (PsiFileSystemItem directory : directories) {
      for (VirtualFile file : directory.getVirtualFile().getChildren()) {
        VfsUtilCore.visitChildrenRecursively(file, new MyDirectoryLookupWalker(pathSegments, collectProcessor));
      }
    }
    return smartList;
  }

  @Override
  public String bindToElement(PsiElement element) {
    WebDirectoryElement webDirectoryElement;
    if ((element instanceof PsiFile) && (webDirectoryElement = WebUtil.findWebDirectoryByFile((PsiFile) element)) != null) {
      String path = webDirectoryElement.getPath();
      if (path.startsWith(this.myPathAndPrefix) && path.endsWith(this.mySuffix)) {
        return path.substring(this.myPathAndPrefix.length(), path.length() - this.mySuffix.length());
      }
      return path;
    }
    return null;
  }

  @Override
  public String handleElementRename(String path) {
    return StringUtil.trimEnd(path, this.mySuffix);
  }

  public Collection<PsiFileSystemItem> getRoots() {
    Set<PsiFileSystemItem> elements = new HashSet<>();
    for (WebFacet webFacet : WebFacet.getInstances(this.myModule)) {
      ContainerUtil.addIfNotNull(elements, WebUtil.getWebUtil().findWebDirectoryElement(this.myPathAndPrefix, webFacet));
    }
    return elements;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder().append("UrlBasedViewResolver{");
    if (!this.myPath.isEmpty()) {
      builder.append("myPath='").append(this.myPath).append("', ");
    }
    return builder.append("myPrefix='").append(this.myPrefix).append('\'').append(", mySuffix='").append(this.mySuffix).append('\'').append('}').toString();
  }

  @Override
  public String getPath() {
    return this.myPath;
  }

  @Override

  public String getPrefix() {
    return this.myPrefix;
  }

  @Override
  public String getSuffix() {
    return this.mySuffix;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UrlBasedViewResolver resolver)) {
      return false;
    }
    return myModule.equals(resolver.myModule) && myPath.equals(resolver.myPath) && myPrefix.equals(resolver.myPrefix) && mySuffix.equals(resolver.mySuffix);
  }

  public int hashCode() {
    int result = myModule.hashCode();
    return (31 * ((31 * ((31 * result) + myPath.hashCode())) + myPrefix.hashCode())) + mySuffix.hashCode();
  }

  private class MyDirectoryLookupWalker extends VirtualFileVisitor<Object> {
    private final Stack<String> nameStack;
    private final List<String> myPathSegments;
    private final Processor<? super LookupElement> myVariants;

    private MyDirectoryLookupWalker(List<String> pathSegments, Processor<? super LookupElement> variants) {
      super(VirtualFileVisitor.limit(5));
      this.myPathSegments = pathSegments;
      this.myVariants = variants;
      this.nameStack = new Stack<>();
    }

    public boolean visitFile(VirtualFile file) {
      PsiFile psiFile;
      String fileName = file.getName();
      if (file.isDirectory()) {
        if (ContainerUtil.startsWith(this.nameStack, this.myPathSegments) || ContainerUtil.startsWith(this.myPathSegments, this.nameStack)) {
          this.nameStack.push(fileName);
          return true;
        }
        return false;
      }
      if (fileName.endsWith(UrlBasedViewResolver.this.mySuffix) && (psiFile = PsiManager.getInstance(UrlBasedViewResolver.this.getModule().getProject()).findFile(file)) != null) {
        String viewName = StringUtil.join(ContainerUtil.append(this.nameStack, fileName.substring(0, fileName.length() - UrlBasedViewResolver.this.mySuffix.length())), "/");
        return this.myVariants.process(LookupElementBuilder.create(viewName).withPsiElement(psiFile).withIcon(psiFile.getIcon(0)).withTailText(" (" + fileName + ")", true));
      }
      return super.visitFile(file);
    }

    public void afterChildrenVisited(VirtualFile file) {
      if (file.isDirectory()) {
        this.nameStack.pop();
      }
      super.afterChildrenVisited(file);
    }
  }
}
