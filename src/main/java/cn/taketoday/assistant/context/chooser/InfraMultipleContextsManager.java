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

package cn.taketoday.assistant.context.chooser;

import com.intellij.facet.FacetFinder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.openapi.vfs.newvfs.AttributeInputStream;
import com.intellij.openapi.vfs.newvfs.AttributeOutputStream;
import com.intellij.openapi.vfs.newvfs.FileAttribute;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.SmartList;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UFile;
import org.jetbrains.uast.UastContextKt;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.impl.ModelsCreationContext;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraMultipleContextsManager {
  private static final FileAttribute CURRENT_CONTEXT = new FileAttribute("today_context", 1, false);

  public static InfraMultipleContextsManager of() {
    return ApplicationManager.getApplication().getService(InfraMultipleContextsManager.class);
  }

  public InfraContextDescriptor getContextDescriptor(PsiFile file) {
    InfraContextDescriptor cached = getUserDefinedContextDescriptor(file);
    if (cached != null) {
      return cached;
    }
    else {
      return CachedValuesManager.getCachedValue(file,
              () -> CachedValueProvider.Result.create(this.calculateDescriptor(file),
                      InfraModificationTrackersManager.from(file.getProject()).getOuterModelsDependencies()));
    }
  }

  private static Object[] geDependencies(Project project) {
    return new Object[] {
            ProjectRootManager.getInstance(project),
            InfraModificationTrackersManager.from(project).getOuterModelsModificationTracker(),
            FacetFinder.getInstance(project).getAllFacetsOfTypeModificationTracker(InfraFacet.FACET_TYPE_ID)
    };
  }

  @Nullable
  public InfraContextDescriptor getUserDefinedContextDescriptor(PsiFile file) {
    PsiFile originalFile = file.getOriginalFile();
    return originalFile.getUserData(InfraContextDescriptor.KEY);
  }

  public List<InfraContextDescriptor> getAllContextDescriptors(PsiFile psiFile) {
    return CachedValuesManager.getCachedValue(psiFile, () -> {
      return CachedValueProvider.Result.create(getDescriptors(psiFile), geDependencies(psiFile.getProject()));
    });
  }

  private static List<InfraContextDescriptor> getDescriptors(PsiFile psiFile) {
    Module module = ModuleUtilCore.findModuleForPsiElement(psiFile);
    if (module == null) {
      return Collections.emptyList();
    }
    ModelsCreationContext context = ModelsCreationContext.create(module).loadModelsFromDependentModules().loadModelsFromModuleDependencies();
    Set<InfraModel> modelsByModule = InfraManager.getAllModels(context);
    if (modelsByModule.isEmpty()) {
      return Collections.emptyList();
    }
    SmartList<InfraContextDescriptor> smartList = new SmartList<>();
    PsiFile file = psiFile.getOriginalFile();
    for (InfraModel model : modelsByModule) {
      InfraFileSet fileSet = model.getFileSet();
      if (containsFile(file, model, fileSet)) {
        smartList.add(createDescriptor(model, fileSet));
      }
    }
    if (!smartList.isEmpty()) {
      smartList.sort(Comparator.comparing(InfraContextDescriptor::getName));
    }
    return smartList;
  }

  private static boolean containsFile(PsiFile file, InfraModel model, InfraFileSet fileSet) {
    if (fileSet == null) {
      return false;
    }
    return containsFile(file.getVirtualFile(), model, new LinkedHashSet<>()) || isImplicitConfigFile(file, model);
  }

  private static boolean isImplicitConfigFile(PsiFile file, InfraModel model) {
    return isConfigFileCandidate(file) && InfraModelVisitorUtils.hasConfigFile(model, file);
  }

  private static boolean isConfigFileCandidate(PsiFile file) {
    if (!(file instanceof XmlFile) || !InfraDomUtils.isInfraXml((XmlFile) file)) {
      UFile uFile = UastContextKt.toUElement(file, UFile.class);
      if (uFile != null) {
        for (UClass aClass : uFile.getClasses()) {
          if (InfraUtils.isConfigurationOrMeta(aClass.getJavaPsi())) {
            return true;
          }
        }
        return false;
      }
      return false;
    }
    return true;
  }

  private static boolean containsFile(VirtualFile virtualFile, InfraModel model, Set<InfraModel> visited) {
    InfraFileSet fileSet = model.getFileSet();
    if (fileSet == null || !fileSet.hasFile(virtualFile)) {
      visited.add(model);
      for (InfraModel depModel : model.getDependencies()) {
        if (!visited.contains(depModel) && containsFile(virtualFile, depModel, visited)) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  private static InfraContextDescriptor createDescriptor(InfraModel model, InfraFileSet fileSet) {
    return createDescriptor(fileSet, model.getModule());
  }

  public static InfraContextDescriptor createDescriptor(InfraFileSet fileSet, Module module) {
    final String qualifiedName = fileSet.getQualifiedName();
    return new InfraContextDescriptor(module, fileSet.getId(), fileSet.getName()) {

      @Override
      public String getQualifiedName() {
        return qualifiedName;
      }
    };
  }

  public void persistDescriptor(PsiFile file, InfraContextDescriptor descriptor) {
    VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
    if (virtualFile == null || virtualFile.isDirectory()) {
      return;
    }
    file.putUserData(InfraContextDescriptor.KEY, descriptor);
    String id = descriptor.getQualifiedName();
    try {
      AttributeOutputStream writeFileAttribute = CURRENT_CONTEXT.writeFileAttribute(virtualFile);
      writeFileAttribute.writeUTF(StringUtil.notNullize(id));
      if (writeFileAttribute != null) {
        writeFileAttribute.close();
      }
    }
    catch (IOException e) {
    }
  }

  @Nullable
  private static String getPersistedDescriptor(PsiFile file) {
    VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
    if (virtualFile != null && !virtualFile.isDirectory() && (virtualFile instanceof VirtualFileWithId)) {
      try {
        AttributeInputStream readFileAttribute = CURRENT_CONTEXT.readFileAttribute(virtualFile);
        if (readFileAttribute != null) {
          String readUTF = readFileAttribute.readUTF();
          if (readFileAttribute != null) {
            readFileAttribute.close();
          }
          return readUTF;
        }
        if (readFileAttribute != null) {
          readFileAttribute.close();
        }
        return null;
      }
      catch (IOException e) {
        return null;
      }
    }
    return null;
  }

  private InfraContextDescriptor calculateDescriptor(PsiFile file) {
    List<InfraContextDescriptor> descriptors = null;
    String qualifiedName = getPersistedDescriptor(file);
    if (qualifiedName != null) {
      if (qualifiedName.equals(InfraContextDescriptor.LOCAL_CONTEXT.getQualifiedName())) {
        return InfraContextDescriptor.LOCAL_CONTEXT;
      }
      else if (qualifiedName.equals(InfraContextDescriptor.ALL_CONTEXTS.getQualifiedName())) {
        return InfraContextDescriptor.ALL_CONTEXTS;
      }
      else {
        descriptors = getAllContextDescriptors(file);
        for (InfraContextDescriptor descriptor : descriptors) {
          if (qualifiedName.equals(descriptor.getQualifiedName())) {
            return descriptor;
          }
        }
      }
    }
    Module currentModule = ModuleUtilCore.findModuleForPsiElement(file);
    if (currentModule != null) {
      if (descriptors == null) {
        descriptors = getAllContextDescriptors(file);
      }
      for (InfraContextDescriptor descriptor2 : descriptors) {
        if (currentModule.equals(descriptor2.getModule())) {
          return descriptor2;
        }
      }
    }
    return InfraContextDescriptor.LOCAL_CONTEXT;
  }
}
