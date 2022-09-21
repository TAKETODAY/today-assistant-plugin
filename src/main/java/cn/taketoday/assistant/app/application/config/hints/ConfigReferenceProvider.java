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

package cn.taketoday.assistant.app.application.config.hints;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.microservices.jvm.config.hints.HintReferenceProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.jps.model.java.JavaResourceRootType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.app.InfraModelConfigFileContributor;
import cn.taketoday.lang.Nullable;

public class ConfigReferenceProvider implements HintReferenceProvider {
  public static final String OPTIONAL_PREFIX = "optional:";
  public static final String FILE_PREFIX = "file:";
  public static final String CLASSPATH_PREFIX = "classpath:";
  public static final String CONFIG_TREE_PREFIX = "configtree:";

  public PsiReference[] getReferences(PsiElement element, List<TextRange> textRanges, ProcessingContext context) {
    String text = element.getText();
    PsiReference[] result = PsiReference.EMPTY_ARRAY;
    for (TextRange range : textRanges) {
      String nextValue = range.substring(text);
      boolean soft = false;
      if (nextValue.startsWith(OPTIONAL_PREFIX)) {
        nextValue = nextValue.substring(OPTIONAL_PREFIX.length());
        range = new TextRange(range.getStartOffset() + OPTIONAL_PREFIX.length(), range.getEndOffset());
        soft = true;
      }
      result = ArrayUtil.mergeArrays(result, getReferences(element, nextValue, range.getStartOffset(), soft));
    }
    return result;
  }

  public PsiReference[] getReferences(PsiElement psiElement, String text, int offset, boolean soft) {
    if (StringUtil.isEmptyOrSpaces(text)) {
      return new PsiReference[] { new PrefixReference(psiElement, text, offset, soft) };
    }
    if (text.startsWith(CLASSPATH_PREFIX)) {
      return getClasspathReferences(psiElement, text.substring(CLASSPATH_PREFIX.length()), CLASSPATH_PREFIX.length() + offset, soft);
    }
    if (text.startsWith(CONFIG_TREE_PREFIX)) {
      return getConfigTreeReferences(psiElement, text.substring(CONFIG_TREE_PREFIX.length()), CONFIG_TREE_PREFIX.length() + offset);
    }
    if (text.startsWith(FILE_PREFIX)) {
      return getFileReferences(psiElement, text.substring(FILE_PREFIX.length()), FILE_PREFIX.length() + offset);
    }
    return ArrayUtil.append(getClasspathReferences(psiElement, text, offset, soft), new PrefixReference(psiElement, text, offset, soft), PsiReference.class);
  }

  private static FileReference[] getClasspathReferences(PsiElement psiElement, String text, int offset, boolean soft) {
    FileReferenceSet referenceSet = new FileReferenceSet(text, psiElement, offset, null, true, false, getFileTypes()) {

      protected boolean isSoft() {
        return soft;
      }

      public Collection<PsiFileSystemItem> computeDefaultContexts() {
        Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
        if (module != null) {
          VirtualFile file = psiElement.getContainingFile().getOriginalFile().getVirtualFile();
          boolean includeTestScope = file != null && ProjectFileIndex.getInstance(psiElement.getProject()).isInTestSourceContent(file);
          return new SmartList(getClasspathRoots(module, includeTestScope));
        }
        return super.computeDefaultContexts();
      }
    };
    if (text.startsWith("/")) {
      referenceSet.addCustomization(FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION, FileReferenceSet.ABSOLUTE_TOP_LEVEL);
    }
    return referenceSet.getAllReferences();
  }

  public static List<PsiDirectory> getClasspathRoots(Module module, boolean includeTestScope) {
    SmartList smartList = new SmartList();
    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    PsiManager psiManager = PsiManager.getInstance(module.getProject());
    for (VirtualFile sourceRoot : moduleRootManager.getSourceRoots(JavaResourceRootType.RESOURCE)) {
      PsiDirectory directory = psiManager.findDirectory(sourceRoot);
      if (directory != null) {
        smartList.add(directory);
      }
    }
    if (includeTestScope) {
      for (VirtualFile sourceRoot2 : moduleRootManager.getSourceRoots(JavaResourceRootType.TEST_RESOURCE)) {
        PsiDirectory directory2 = psiManager.findDirectory(sourceRoot2);
        if (directory2 != null) {
          smartList.add(directory2);
        }
      }
    }
    return smartList;
  }

  private static FileReference[] getConfigTreeReferences(PsiElement psiElement, String text, int offset) {
    FileReferenceSet referenceSet = new FileReferenceSet(text, psiElement, offset, null, true) {

      protected boolean isSoft() {
        return true;
      }

      protected Condition<PsiFileSystemItem> getReferenceCompletionFilter() {
        return PsiFileSystemItem::isDirectory;
      }

      public Collection<PsiFileSystemItem> computeDefaultContexts() {
        PsiDirectory contentRoot = getContentRoot(psiElement);
        if (contentRoot == null) {
          return super.computeDefaultContexts();
        }
        return new SmartList(contentRoot);
      }
    };
    if (text.startsWith("/")) {
      referenceSet.addCustomization(FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION, FileReferenceSet.ABSOLUTE_TOP_LEVEL);
    }
    return referenceSet.getAllReferences();
  }

  private static FileReference[] getFileReferences(PsiElement psiElement, String text, int offset) {
    FileReferenceSet referenceSet = new FileReferenceSet(text, psiElement, offset, null, true, false, getFileTypes()) {

      protected boolean isSoft() {
        return true;
      }

      public Collection<PsiFileSystemItem> computeDefaultContexts() {
        PsiDirectory contentRoot = getContentRoot(psiElement);
        if (contentRoot == null) {
          return super.computeDefaultContexts();
        }
        return new SmartList(contentRoot);
      }
    };
    if (text.startsWith("/")) {
      referenceSet.addCustomization(FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION, FileReferenceSet.ABSOLUTE_TOP_LEVEL);
    }
    return referenceSet.getAllReferences();
  }

  private static FileType[] getFileTypes() {
    return ContainerUtil.map2Array(InfraModelConfigFileContributor.EP_NAME.getExtensionList(), FileType.EMPTY_ARRAY, InfraModelConfigFileContributor::getFileType);
  }

  @Nullable
  private static PsiDirectory getContentRoot(PsiElement psiElement) {
    VirtualFile contentRoot;
    VirtualFile file = psiElement.getContainingFile().getOriginalFile().getVirtualFile();
    if (file == null || (contentRoot = ProjectRootManager.getInstance(psiElement.getProject()).getFileIndex().getContentRootForFile(file)) == null) {
      return null;
    }
    return psiElement.getManager().findDirectory(contentRoot);
  }

  private static final class PrefixReference extends PsiReferenceBase<PsiElement> {
    private final boolean myOptional;

    private PrefixReference(PsiElement psiElement, String text, int offset, boolean optional) {
      super(psiElement, optional);
      this.myOptional = optional;
      int colonIdx = text.indexOf(58);
      setRangeInElement(TextRange.create(offset, offset + (colonIdx == -1 ? text.length() : colonIdx)));
    }

    public PsiElement resolve() {
      return getElement();
    }

    public Object[] getVariants() {
      List<String> prefixes = new ArrayList<>(Arrays.asList(CLASSPATH_PREFIX, FILE_PREFIX, CONFIG_TREE_PREFIX));
      if (!this.myOptional) {
        prefixes.add(OPTIONAL_PREFIX);
      }
      return ContainerUtil.map2Array(prefixes, prefix -> {
        return LookupElementBuilder.create(prefix).bold();
      });
    }
  }
}
