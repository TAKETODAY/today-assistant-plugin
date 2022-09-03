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

package cn.taketoday.assistant.impl;

import com.intellij.ide.highlighter.HtmlFileType;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.impl.PsiTreeChangeEventImpl;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.spi.psi.SPIFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;

import org.jetbrains.uast.UAnnotated;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UImportStatement;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UVariable;
import org.jetbrains.uast.UastLanguagePlugin;
import org.jetbrains.uast.util.ClassSet;
import org.jetbrains.uast.util.ClassSetKt;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.lang.Nullable;

public class InfraOuterModelsModificationTracker extends SimpleModificationTracker {

  public static final LanguageFileType JSP = (LanguageFileType) FileTypeManager.getInstance().getStdFileType("JSP");
  public static final LanguageFileType JSPX = (LanguageFileType) FileTypeManager.getInstance().getStdFileType("JSPX");

  public InfraOuterModelsModificationTracker(Project project, Disposable parent, boolean useUastBased) {
    PsiManager.getInstance(project).addPsiTreeChangeListener(useUastBased ? new MyUastPsiTreeChangeAdapter(project) : new MyJavaPsiTreeChangeAdapter(), parent);
    MessageBusConnection connection = project.getMessageBus().connect(parent);
    connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkVirtualFileListenerAdapter(new MyVirtualFileListener(project)));
  }

  private boolean processConfigFileChange(PsiFile psiFile) {
    if (psiFile instanceof PropertiesFile) {
      incModificationCount();
      return true;
    }
    else if (psiFile instanceof SPIFile) {
      incModificationCount();
      return true;
    }
    else {
      String languageId = psiFile == null ? null : psiFile.getLanguage().getID();
      if ("yaml".equals(languageId)) {
        incModificationCount();
        return true;
      }
      return false;
    }
  }

  private final class MyVirtualFileListener implements VirtualFileListener {
    private final ProjectFileIndex myFileIndex;

    private MyVirtualFileListener(Project project) {
      this.myFileIndex = ProjectFileIndex.getInstance(project);
    }

    public void fileCreated(VirtualFileEvent event) {
      incModificationCountIfMine(event);
    }

    public void beforeFileDeletion(VirtualFileEvent event) {
      incModificationCountIfMine(event);
    }

    public void fileMoved(VirtualFileMoveEvent event) {
      incModificationCountIfMine(event);
    }

    public void propertyChanged(VirtualFilePropertyEvent event) {
      if ("name".equals(event.getPropertyName())) {
        incModificationCountIfMine(event);
      }
    }

    private void incModificationCountIfMine(VirtualFileEvent event) {
      VirtualFile file = event.getFile();
      if (!this.myFileIndex.isInContent(file)) {
        return;
      }
      if (!file.isDirectory() && isIgnoredFileType(file.getFileType())) {
        return;
      }
      InfraOuterModelsModificationTracker.this.incModificationCount();
    }

    private boolean isIgnoredFileType(FileType type) {
      return type.equals(HtmlFileType.INSTANCE)
              || ((type instanceof LanguageFileType)
              && "JavaScript".equals(((LanguageFileType) type).getLanguage().getID()))
              || type.equals(JSP) || type.equals(JSPX);
    }
  }

  private class MyJavaPsiTreeChangeAdapter extends PsiTreeChangeAdapter {

    public void beforeChildAddition(PsiTreeChangeEvent event) {
      processChange(event, event.getParent(), event.getChild());
    }

    public void childrenChanged(PsiTreeChangeEvent event) {
      if ((event instanceof PsiTreeChangeEventImpl) && ((PsiTreeChangeEventImpl) event).isGenericChange()) {
        return;
      }
      processChange(event, event.getParent(), event.getChild());
    }

    public void beforeChildRemoval(PsiTreeChangeEvent event) {
      processChange(event, event.getParent(), event.getChild());
    }

    public void childAdded(PsiTreeChangeEvent event) {
      processChange(event, event.getParent(), event.getChild());
    }

    public void childRemoved(PsiTreeChangeEvent event) {
      processChange(event, event.getParent(), event.getChild());
    }

    public void childReplaced(PsiTreeChangeEvent event) {
      processChange(event, event.getParent(), event.getOldChild());
    }

    private void processChange(PsiTreeChangeEvent event, PsiElement parent, PsiElement child) {
      PsiFile psiFile = event.getFile();
      if (InfraOuterModelsModificationTracker.this.processConfigFileChange(psiFile) || !(psiFile instanceof PsiClassOwner)) {
        return;
      }
      if ((parent instanceof PsiModifierList) && (child instanceof PsiAnnotation)) {
        checkRelevantAnnotation((PsiAnnotation) child);
      }
      else if ((parent instanceof PsiModifierList) && (parent.getParent() instanceof PsiClass)) {
        InfraOuterModelsModificationTracker.this.incModificationCount();
      }
      else if (parent instanceof PsiClass) {
        if ((child instanceof PsiClass) || (event.getNewChild() instanceof PsiClass) || (event.getOldChild() instanceof PsiClass)) {
          InfraOuterModelsModificationTracker.this.incModificationCount();
        }
      }
      else if ((parent instanceof PsiImportList) || (child instanceof PsiImportList) || PsiTreeUtil.getParentOfType(parent, PsiImportList.class) != null) {
        InfraOuterModelsModificationTracker.this.incModificationCount();
      }
      else {
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(parent, PsiAnnotation.class);
        if (annotation != null) {
          checkRelevantAnnotation(annotation);
        }
      }
    }

    private void checkRelevantAnnotation(PsiAnnotation annotation) {
      PsiModifierListOwner modifierListOwner = PsiTreeUtil.getParentOfType(annotation, PsiModifierListOwner.class);
      if (modifierListOwner == null || (modifierListOwner instanceof PsiClass) || (modifierListOwner instanceof PsiMethod)) {
        InfraOuterModelsModificationTracker.this.incModificationCount();
      }
    }
  }

  private class MyUastPsiTreeChangeAdapter extends PsiTreeChangeAdapter {
    private final Project myProject;
    private final Map<String, CachedValue<MyPsiPossibleTypes>> myPsiPossibleTypes = new HashMap();

    private MyUastPsiTreeChangeAdapter(Project project) {
      this.myProject = project;
    }

    public void beforeChildAddition(PsiTreeChangeEvent event) {
      processChange(event, event.getParent(), event.getChild());
    }

    public void childrenChanged(PsiTreeChangeEvent event) {
      if ((event instanceof PsiTreeChangeEventImpl) && ((PsiTreeChangeEventImpl) event).isGenericChange()) {
        return;
      }
      processChange(event, event.getParent(), event.getChild());
    }

    public void beforeChildRemoval(PsiTreeChangeEvent event) {
      processChange(event, event.getParent(), event.getChild());
    }

    public void childAdded(PsiTreeChangeEvent event) {
      processChange(event, event.getParent(), event.getChild());
    }

    public void childRemoved(PsiTreeChangeEvent event) {
      processChange(event, event.getParent(), event.getChild());
    }

    public void childReplaced(PsiTreeChangeEvent event) {
      processChange(event, event.getParent(), event.getOldChild());
    }

    private void processChange(PsiTreeChangeEvent event, PsiElement parent, PsiElement child) {
      MyPsiPossibleTypes possiblePsiTypes;
      PsiElement firstChild = null;
      PsiFile psiFile = event.getFile();
      if (InfraOuterModelsModificationTracker.this.processConfigFileChange(psiFile) || !(psiFile instanceof PsiClassOwner)) {
        return;
      }
      if (((parent instanceof PsiFile) && child == null) || (possiblePsiTypes = getPossiblePsiTypesFor(psiFile.getLanguage().getID())) == null) {
        return;
      }
      PsiElement newChild = event.getNewChild();
      PsiElement grandParent = parent == null ? null : parent.getParent();
      PsiElement firstSibling = (parent == null || !parent.isValid()) ? null : parent.getFirstChild();
      PsiElement unsafeGrandChild = null;
      if (!(child instanceof LazyParseablePsiElement)) {
        if (child == null) {
          firstChild = null;
        }
        else {
          try {
            firstChild = child.getFirstChild();
          }
          catch (PsiInvalidElementAccessException e) {
          }
        }
        unsafeGrandChild = firstChild;
      }
      if (isRelevantAnnotation(child, possiblePsiTypes)
              || isRelevantAnnotation(unsafeGrandChild, possiblePsiTypes)
              || isRelevantAnnotation(newChild, possiblePsiTypes)
              || ((ClassSetKt.isInstanceOf(grandParent, possiblePsiTypes.forClasses)
              && !ClassSetKt.isInstanceOf(parent, possiblePsiTypes.forAnnotationOwners))
              || (((ClassSetKt.isInstanceOf(parent, possiblePsiTypes.forClasses)
              || ClassSetKt.isInstanceOf(grandParent, possiblePsiTypes.forClasses))
              && (ClassSetKt.isInstanceOf(child, possiblePsiTypes.forClasses)
              || ClassSetKt.isInstanceOf(event.getNewChild(), possiblePsiTypes.forClasses)
              || ClassSetKt.isInstanceOf(event.getOldChild(), possiblePsiTypes.forClasses)))
              || ClassSetKt.isInstanceOf(firstSibling, possiblePsiTypes.forImports)
              || ClassSetKt.isInstanceOf(unsafeGrandChild, possiblePsiTypes.forImports)
              || ClassSetKt.isInstanceOf(child, possiblePsiTypes.forImports)
              || null != PsiTreeUtil.findFirstParent(parent, it -> {

        return ClassSetKt.isInstanceOf(possiblePsiTypes, possiblePsiTypes.forImports) || isRelevantAnnotation(it, possiblePsiTypes);
      }) || (child instanceof LazyParseablePsiElement)))) {
        InfraOuterModelsModificationTracker.this.incModificationCount();
      }
    }

    private boolean isRelevantAnnotation(@Nullable PsiElement psiElement, MyPsiPossibleTypes possiblePsiTypes) {
      if (!ClassSetKt.isInstanceOf(psiElement, possiblePsiTypes.forAnnotations)) {
        return false;
      }
      PsiElement modifierListOwner = PsiTreeUtil.findFirstParent(psiElement, it -> {
        return ClassSetKt.isInstanceOf(it, possiblePsiTypes.forAnnotationOwners) && !ClassSetKt.isInstanceOf(it, possiblePsiTypes.forAnnotations);
      });
      return modifierListOwner == null || ClassSetKt.isInstanceOf(modifierListOwner, possiblePsiTypes.forClasses) || (ClassSetKt.isInstanceOf(modifierListOwner,
              possiblePsiTypes.forMethods) && !ClassSetKt.isInstanceOf(modifierListOwner, possiblePsiTypes.forVariables));
    }

    @Nullable
    private MyPsiPossibleTypes getPossiblePsiTypesFor(String languageId) {
      return this.myPsiPossibleTypes.computeIfAbsent(languageId, _key -> {
        return CachedValuesManager.getManager(this.myProject).createCachedValue(() -> {
          UastLanguagePlugin uastLanguagePlugin = ContainerUtil.find(UastLanguagePlugin.Companion.getInstances(), it -> {
            return languageId.equals(it.getLanguage().getID());
          });
          return CachedValueProvider.Result.create(uastLanguagePlugin == null ? null : new MyPsiPossibleTypes(uastLanguagePlugin), ModificationTracker.NEVER_CHANGED);
        });
      }).getValue();
    }
  }

  public static class MyPsiPossibleTypes {

    public final ClassSet<PsiElement> forClasses;
    public final ClassSet<PsiElement> forMethods;
    public final ClassSet<PsiElement> forVariables;
    public final ClassSet<PsiElement> forImports;
    public final ClassSet<PsiElement> forAnnotations;
    public final ClassSet<PsiElement> forAnnotationOwners;

    private MyPsiPossibleTypes(UastLanguagePlugin uastPlugin) {
      this.forClasses = uastPlugin.getPossiblePsiSourceTypes(UClass.class);
      this.forMethods = uastPlugin.getPossiblePsiSourceTypes(UMethod.class);
      this.forVariables = uastPlugin.getPossiblePsiSourceTypes(UVariable.class);
      this.forImports = uastPlugin.getPossiblePsiSourceTypes(UImportStatement.class);
      this.forAnnotations = uastPlugin.getPossiblePsiSourceTypes(UAnnotation.class);
      this.forAnnotationOwners = uastPlugin.getPossiblePsiSourceTypes(UAnnotated.class);
    }
  }
}
