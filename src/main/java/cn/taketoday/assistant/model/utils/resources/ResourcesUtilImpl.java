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

package cn.taketoday.assistant.model.utils.resources;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FilePathReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.WrappingConverter;
import com.intellij.util.xml.impl.ConvertContextFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import cn.taketoday.assistant.model.converters.PatternFileReferenceSet;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.lang.Nullable;

public class ResourcesUtilImpl extends ResourcesUtil {

  private static final String CLASSPATH_PREFIX = "classpath:";

  private static final String CLASSPATH_PREFIX_ASTERISK = "classpath*:";

  private static final String FILE_PREFIX = "file:";

  private static final String HTTP_PREFIX = "http:";
  private static final Function<PsiFile, Collection<PsiFileSystemItem>> TEST_CONTEXT_EVALUATOR = file -> {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return Collections.singletonList(file.getContainingDirectory());
    }
    VirtualFile dir = file.getProject().getBaseDir();
    if (dir == null) {
      return Collections.emptyList();
    }
    PsiDirectory directory = file.getManager().findDirectory(dir);
    return ContainerUtil.createMaybeSingletonList(directory);
  };

  private static FilePathReferenceProvider createFilePathReferenceProvider(InfraResourcesBuilder builder) {
    return new FilePathReferenceProvider(builder.isEndingSlashNotAllowed()) {
      protected FileReference createFileReference(FileReferenceSet referenceSet, TextRange range, int index, String text) {
        if (!PatternFileReferenceSet.isAntPattern(text)) {
          return new FileReference(referenceSet, range, index, text) {
            @Nullable
            public String getNewFileTemplateName() {
              return builder.getTemplateName();
            }
          };
        }
        return new PatternFileReferenceSet.PatternFileReference(referenceSet, range, index, text, builder.getFilter());
      }

      protected boolean isPsiElementAccepted(PsiElement element) {
        return super.isPsiElementAccepted(element) && (element instanceof PsiFileSystemItem) && builder.getFilter().value((PsiFileSystemItem) element);
      }
    };
  }

  @Override
  public <V extends PsiFileSystemItem> Collection<V> getResourceItems(InfraProperty property, Condition<PsiFileSystemItem> filter) {
    Collection<V> result = new LinkedHashSet<>();
    InfraPropertyUtils.processInfraValues(property, (genericValue, s) -> {
      Object value = genericValue.getValue();
      if (value instanceof String) {
        Converter converter = WrappingConverter.getDeepestConverter(property.getValueAttr().getConverter(), property.getValueAttr());
        value = converter.fromString(genericValue.getStringValue(), ConvertContextFactory.createConvertContext(genericValue));
      }
      if (value instanceof Collection<?> collection) {
        for (Object o : collection) {
          if ((o instanceof PsiFileSystemItem) && filter.value((PsiFileSystemItem) o)) {
            result.add((V) o);
          }
        }
        return true;
      }
      return true;
    });
    return result;
  }

  @Override
  public <V extends PsiFileSystemItem> Collection<V> getResourceItems(PsiReference[] references, Condition<PsiFileSystemItem> filter) {
    Collection<V> result = new LinkedHashSet();
    FileReferenceSet curSet = null;
    for (PsiReference psiReference : references) {
      PsiReference reference = psiReference;
      FileReferenceSet refSet = reference instanceof FileReference ? ((FileReference) reference).getFileReferenceSet() : null;
      if (refSet != null) {
        if (refSet == curSet) {
          continue;
        }

        curSet = refSet;
        reference = refSet.getLastReference();
        if (reference == null) {
          continue;
        }
      }

      if (reference instanceof PsiPolyVariantReference) {
        ResolveResult[] resolveResults = ((PsiPolyVariantReference) reference).multiResolve(false);

        for (ResolveResult resolveResult : resolveResults) {
          PsiElement psiElement = resolveResult.getElement();
          if (psiElement instanceof PsiFileSystemItem && filter.value((PsiFileSystemItem) psiElement)) {
            result.add((V) psiElement);
          }
        }
      }
      else {
        PsiElement psiElement = reference.resolve();
        if (psiElement instanceof PsiFileSystemItem && filter.value((PsiFileSystemItem) psiElement)) {
          result.add((V) psiElement);
        }
      }
    }

    return result;
  }

  @Override
  public PsiReference[] getReferences(InfraResourcesBuilder builder) {
    PsiReference[] prefixedReferences = getPrefixedReferences(builder);
    if (prefixedReferences != null) {
      return prefixedReferences;
    }
    FileReferenceSet set = new PatternFileReferenceSet(builder.getText(), builder.getElement(), builder.getOffset(), builder.isEndingSlashNotAllowed(), builder.getTemplateName()) {

      @Override
      protected boolean isSoft() {
        return builder.isSoft();
      }

      protected Condition<PsiFileSystemItem> getReferenceCompletionFilter() {
        return builder.getFilter();
      }

    };
    if (builder.getCustomDefaultPathEvaluator() != null) {
      set.addCustomization(FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION, builder.getCustomDefaultPathEvaluator());
    }
    else if (builder.isFromCurrent()) {
      set.addCustomization(FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION, file -> {
        return ContainerUtil.createMaybeSingletonList(file.getContainingDirectory());
      });
    }
    else if (builder.isFromRoot()) {
      set.addCustomization(FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION, FileReferenceSet.ABSOLUTE_TOP_LEVEL);
    }
    return ArrayUtil.append(set.getAllReferences(), new PrefixReference(builder), PsiReference.class);
  }

  private static PsiReference[] getPrefixedReferences(InfraResourcesBuilder builder) {
    String text = builder.getText();
    if (StringUtil.isEmptyOrSpaces(text) || text.startsWith(HTTP_PREFIX)) {
      return new PsiReference[] { new PrefixReference(builder) };
    }
    if (text.startsWith(CLASSPATH_PREFIX)) {
      return getFilePathReferences(builder, CLASSPATH_PREFIX);
    }
    if (text.startsWith(CLASSPATH_PREFIX_ASTERISK)) {
      return getFilePathReferences(builder, CLASSPATH_PREFIX_ASTERISK);
    }
    if (text.startsWith(FILE_PREFIX)) {
      String str = text.substring(FILE_PREFIX.length());
      if (str.startsWith("/") || FileUtil.isAbsolute(str)) {
        return PsiReference.EMPTY_ARRAY;
      }
      FileReferenceSet set = new PatternFileReferenceSet(str, builder.getElement(), FILE_PREFIX.length() + builder.getOffset(), builder.isEndingSlashNotAllowed(), builder.getTemplateName()) {
        @Override
        protected boolean isSoft() {
          return builder.isSoft();
        }

        protected Condition<PsiFileSystemItem> getReferenceCompletionFilter() {
          return builder.getFilter();
        }
      };
      if (builder.getCustomDefaultPathEvaluator() != null) {
        set.addCustomization(FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION, builder.getCustomDefaultPathEvaluator());
      }
      else {
        PsiFile psiFile = builder.getElement().getContainingFile();
        PsiLanguageInjectionHost injectionHost = InjectedLanguageManager.getInstance(psiFile.getProject()).getInjectionHost(psiFile);
        if (injectionHost != null) {
          psiFile = injectionHost.getContainingFile();
        }
        VirtualFile file = psiFile.getVirtualFile();
        boolean test = file != null && ProjectFileIndex.getInstance(psiFile.getProject()).isInTestSourceContent(file);
        set.addCustomization(FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION, test ? TEST_CONTEXT_EVALUATOR : FileReferenceSet.ABSOLUTE_TOP_LEVEL);
      }
      return ArrayUtil.append(set.getAllReferences(), new PrefixReference(builder), PsiReference.class);
    }
    return null;
  }

  private static PsiReference[] getFilePathReferences(InfraResourcesBuilder builder, @Nullable String prefix) {
    String text;
    int offset;
    if (prefix == null) {
      text = builder.getText();
      offset = builder.getOffset();
    }
    else {
      text = StringUtil.substringAfter(builder.getText(), prefix);
      offset = builder.getOffset() + prefix.length();
    }
    return createFilePathReferenceProvider(builder).getReferencesByElement(builder.getElement(), text, offset, builder.isSoft(), builder.getModules());
  }

  @Override
  public PsiReference[] getClassPathReferences(InfraResourcesBuilder builder) {
    PsiReference[] prefixedReferences = getPrefixedReferences(builder);
    return prefixedReferences != null ? prefixedReferences : getFilePathReferences(builder, null);
  }

  @Override
  @Nullable
  public String getResourceFileReferenceString(PsiFile resourceFile) {
    VirtualFile virtualFile = resourceFile == null ? null : resourceFile.getVirtualFile();
    if (virtualFile == null) {
      return null;
    }
    ProjectFileIndex index = ProjectRootManager.getInstance(resourceFile.getProject()).getFileIndex();
    VirtualFile sourceRoot = index.getSourceRootForFile(virtualFile);
    if (sourceRoot != null) {
      return "classpath:" + VfsUtilCore.getRelativePath(virtualFile, sourceRoot, '/');
    }
    VirtualFile contentRoot = index.getContentRootForFile(virtualFile);
    if (contentRoot != null) {
      return "file:" + VfsUtilCore.getRelativePath(virtualFile, contentRoot, '/');
    }
    return "file:" + virtualFile.getPath();
  }

  public static final class PrefixReference extends PsiReferenceBase<PsiElement> {

    private PrefixReference(InfraResourcesBuilder builder) {
      super(builder.getElement(), builder.isSoft());
      String text = builder.getText();
      int colonIdx = text.indexOf(58);
      colonIdx = colonIdx == -1 ? text.length() : colonIdx;
      int offset = builder.getOffset();
      setRangeInElement(TextRange.create(offset, offset + colonIdx));
    }

    public PsiElement resolve() {
      return getElement();
    }

    public Object[] getVariants() {
      return new Object[] {
              LookupElementBuilder.create(CLASSPATH_PREFIX).bold(),
              LookupElementBuilder.create(CLASSPATH_PREFIX_ASTERISK).bold(),
              LookupElementBuilder.create(FILE_PREFIX).bold(),
              LookupElementBuilder.create(HTTP_PREFIX).bold()
      };
    }
  }
}
