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

package cn.taketoday.assistant.facet;

import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import cn.taketoday.lang.Nullable;

public abstract class InfraFileSet implements Disposable {
  private final InfraFileSetData data;
  private final InfraFacet facet;
  private final List<VirtualFilePointer> files;
  private boolean autodetected;

  public abstract Icon getIcon();

  public InfraFileSet(String id, String name, InfraFacet parent) {
    this(InfraFileSetData.create(id, name), parent);
  }

  public InfraFileSet(InfraFileSetData data, InfraFacet facet) {
    this.files = new SmartList<>();
    this.data = data;
    this.facet = facet;
    for (String url : data.getFiles()) {
      files.add(createVirtualFilePointer(url));
    }
  }

  public InfraFileSet(InfraFileSet original) {
    this.files = new SmartList<>();
    this.facet = original.facet;
    this.data = InfraFileSetData.create(original.data);
    this.files.addAll(original.files);
    this.autodetected = original.isAutodetected();
    this.data.setActiveProfiles(original.getActiveProfiles());
  }

  public String getId() {
    return this.data.getId();
  }

  public String getName() {
    return this.data.getName();
  }

  public void setName(String name) {
    this.data.setName(name);
  }

  public String getQualifiedName() {
    return InfraFileSetService.of().getQualifiedName(this);
  }

  public boolean isNew() {
    return false;
  }

  public boolean isAutodetected() {
    return this.autodetected;
  }

  public void setAutodetected(boolean autodetected) {
    this.autodetected = autodetected;
  }

  public boolean isRemoved() {
    return this.data.isRemoved();
  }

  public void setRemoved(boolean removed) {
    this.data.setRemoved(removed);
  }

  public InfraFacet getFacet() {
    return this.facet;
  }

  public InfraFileSetData getData() {
    return this.data;
  }

  public Set<String> getActiveProfiles() {
    return this.data.getActiveProfiles();
  }

  public void setActiveProfiles(Set<String> activeProfiles) {
    this.data.setActiveProfiles(activeProfiles);
  }

  public Set<InfraFileSet> getDependencyFileSets() {
    Set<InfraFileSet> sets = new LinkedHashSet<>();
    for (String dependencyId : this.data.getDependencies()) {
      InfraFileSet fileSet = InfraFileSetService.of().findDependencyFileSet(this, dependencyId);
      ContainerUtil.addIfNotNull(sets, fileSet);
    }
    return sets;
  }

  public void setDependencies(List<InfraFileSet> springFileSets) {
    this.data.getDependencies().clear();
    for (InfraFileSet dependency : springFileSets) {
      addDependency(dependency);
    }
  }

  public void addDependency(InfraFileSet springFileSet) {
    this.data.addDependency(getDependencyIdFor(springFileSet));
  }

  public void removeDependency(InfraFileSet springFileSet) {
    this.data.getDependencies().remove(getDependencyIdFor(springFileSet));
  }

  private String getDependencyIdFor(InfraFileSet other) {
    return InfraFileSetService.of().getDependencyIdFor(this, other);
  }

  public List<VirtualFilePointer> getFiles() {
    return this.files;
  }

  public Set<VirtualFilePointer> getXmlFiles() {
    Set<VirtualFilePointer> configFiles = getConfigFiles(StdFileTypes.XML);
    return configFiles;
  }

  public Set<VirtualFilePointer> getCodeConfigurationFiles() {
    VirtualFile virtualFile;
    Set<VirtualFilePointer> filePointers = new LinkedHashSet<>();
    for (VirtualFilePointer virtualFilePointer : this.files) {
      if (virtualFilePointer.isValid() && (virtualFile = virtualFilePointer.getFile()) != null && virtualFile.isValid() && !isFileType(StdFileTypes.XML, virtualFile) && !isFileType(
              PropertiesFileType.INSTANCE, virtualFile)) {
        PsiFile psiFile = PsiManager.getInstance(facet.getModule().getProject()).findFile(virtualFile);
        if (psiFile instanceof PsiClassOwner) {
          filePointers.add(virtualFilePointer);
        }
      }
    }
    return filePointers;
  }

  private Set<VirtualFilePointer> getConfigFiles(FileType fileType) {
    VirtualFile virtualFile;
    Set<VirtualFilePointer> filePointers = new LinkedHashSet<>();
    for (VirtualFilePointer virtualFilePointer : this.files) {
      if (virtualFilePointer.isValid() && (virtualFile = virtualFilePointer.getFile()) != null && isFileType(fileType, virtualFile)) {
        filePointers.add(virtualFilePointer);
      }
    }
    return filePointers;
  }

  private static boolean isFileType(FileType fileType, VirtualFile file) {
    return FileTypeRegistry.getInstance().isFileOfType(file, fileType);
  }

  public Set<VirtualFilePointer> getPropertiesFiles() {
    return getConfigFiles(PropertiesFileType.INSTANCE);
  }

  public boolean hasFile(@Nullable VirtualFile file) {
    VirtualFile virtualFile;
    if (file == null) {
      return false;
    }
    for (VirtualFilePointer virtualFilePointer : this.files) {
      if (virtualFilePointer.isValid() && (virtualFile = virtualFilePointer.getFile()) != null && file.equals(virtualFile)) {
        return true;
      }
    }
    return false;
  }

  protected VirtualFilePointer createVirtualFilePointer(String url) {
    return VirtualFilePointerManager.getInstance().create(url, this, null);
  }

  public void addFile(VirtualFile file) {
    addFile(file.getUrl());
  }

  public void addFile(String url) {
    if (!StringUtil.isEmptyOrSpaces(url)) {
      this.files.add(createVirtualFilePointer(url));
      this.data.addFile(url);
    }
  }

  public void removeFile(VirtualFilePointer file) {
    this.files.remove(file);
    this.data.removeFile(file.getUrl());
  }

  @Override
  public void dispose() {
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InfraFileSet set = (InfraFileSet) o;
    if (data != null) {
      if (!data.equals(set.data)) {
        return false;
      }
    }
    else if (set.data != null) {
      return false;
    }
    return facet != null ? facet.equals(set.facet) : set.facet == null;
  }

  @Override
  public int hashCode() {
    int result = facet != null ? facet.hashCode() : 0;
    return (31 * result) + (data != null ? data.hashCode() : 0);
  }

}
