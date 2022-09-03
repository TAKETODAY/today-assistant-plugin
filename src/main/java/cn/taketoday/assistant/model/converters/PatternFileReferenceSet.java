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

package cn.taketoday.assistant.model.converters;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.psi.search.PsiFileSystemItemProcessor;

import java.util.Collection;
import java.util.regex.Pattern;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.lang.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class PatternFileReferenceSet extends FileReferenceSet {

  private final String myTemplateName;

  public PatternFileReferenceSet(String str, PsiElement element, int offset) {
    this(str, element, offset, true);
  }

  public PatternFileReferenceSet(String str, PsiElement element, int offset, boolean endingSlashNotAllowed) {
    this(str, element, offset, endingSlashNotAllowed, null);
  }

  public PatternFileReferenceSet(String str, PsiElement element, int offset, boolean endingSlashNotAllowed,
          @Nullable String templateName) {
    super(str, element, offset, null, true, endingSlashNotAllowed, null, false);
    myTemplateName = templateName;

    // build references after setting myTemplateName
    reparse();
  }

  @Override
  public FileReference createFileReference(TextRange range, int index, String text) {
    if (!isAntPattern(text)) {
      return new FileReference(this, range, index, text) {
        @Nullable
        @Override
        public String getNewFileTemplateName() {
          return myTemplateName;
        }
      };
    }

    return new PatternFileReference(this, range, index, text, getReferenceCompletionFilter());
  }

  // @see cn.taketoday.util.AntPathMatcher#isPattern
  public static boolean isAntPattern(String str) {
    return (str.indexOf('*') != -1 || str.indexOf('?') != -1);
  }

  @Override
  protected boolean isSoft() {
    return true;
  }

  /**
   * @author Dmitry Avdeev
   */
  public static class PatternFileReference extends FileReference {

    private final Condition<? super PsiFileSystemItem> myFilter;

    public PatternFileReference(FileReferenceSet referenceSet,
            TextRange range,
            int index,
            String text,
            Condition<? super PsiFileSystemItem> filter) {
      super(referenceSet, range, index, text);
      myFilter = filter;
    }

    @Override
    protected void innerResolveInContext(String text,
            PsiFileSystemItem context,
            Collection<ResolveResult> result, boolean caseSensitive) {

      if ("**".equals(text)) {
        addDirectoryResolves(context, result);
      }
      else {
        String patternText = FileUtil.convertAntToRegexp(text);
        Pattern pattern = Pattern.compile(patternText);

        PsiElement[] psiElements = context.getChildren();
        for (PsiElement psiElement : psiElements) {
          if (psiElement instanceof final PsiFileSystemItem psiFileSystemItem) {

            if (pattern.matcher(psiFileSystemItem.getName()).matches()) {
              boolean acceptFile = psiFileSystemItem.isDirectory() ||
                      myFilter.value(getOriginalFile(psiFileSystemItem));
              if (acceptFile) {
                result.add(new PsiElementResolveResult(psiElement));
              }
            }
          }
        }
      }
    }

    private static void addDirectoryResolves(PsiElement context, Collection<? super ResolveResult> result) {
      if (!(context instanceof final PsiFileSystemItem fileSystemItem)) {
        return;
      }

      if (fileSystemItem.isDirectory()) {
        result.add(new PsiElementResolveResult(context));

        fileSystemItem.processChildren(new PsiFileSystemItemProcessor() {

          @Override
          public boolean acceptItem(String name, boolean isDirectory) {
            return isDirectory;
          }

          @Override
          public boolean execute(PsiFileSystemItem element) {
            VirtualFile virtualFile = element.getVirtualFile();
            if (virtualFile != null) {
              addDirectoryResolves(element.getManager().findDirectory(virtualFile), result);
            }
            return true;
          }
        });
      }
    }

    @Override
    public String getUnresolvedMessagePattern() {
      return InfraBundle.message("pattern.fileset.no.matching.files");
    }
  }
}
