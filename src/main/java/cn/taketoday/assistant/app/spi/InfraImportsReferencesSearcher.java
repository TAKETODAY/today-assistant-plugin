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

package cn.taketoday.assistant.app.spi;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.ClassUtil;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;

import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

public final class InfraImportsReferencesSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {

  public InfraImportsReferencesSearcher() {
    super(true);
  }

  public void processQuery(ReferencesSearch.SearchParameters p, Processor<? super PsiReference> processor) {
    PsiElement elementToSearch = p.getElementToSearch();
    if (!elementToSearch.isValid()) {
      return;
    }
    SearchScope effectiveSearchScope = p.getEffectiveSearchScope();
    if (!(effectiveSearchScope instanceof GlobalSearchScope)) {
      effectiveSearchScope = null;
    }
    GlobalSearchScope scope = (GlobalSearchScope) effectiveSearchScope;
    if (scope == null) {
      return;
    }
    SmartList names = new SmartList();
    if (elementToSearch instanceof PsiClass psiClass) {
      String name = psiClass.getQualifiedName();
      if (name == null) {
        return;
      }
      names.add(name);
      String jvmClassName = ClassUtil.getJVMClassName(psiClass);
      if (jvmClassName == null) {
        return;
      }
      if (!Intrinsics.areEqual(jvmClassName, name)) {
        names.add(jvmClassName);
      }
    }
    else if (elementToSearch instanceof PsiPackage) {
      names.add(((PsiPackage) elementToSearch).getQualifiedName());
    }
    if (names.isEmpty()) {
      return;
    }
    Project project = elementToSearch.getProject();
    GlobalSearchScope adjustedScope = InfraImportsFileIndex.adjustScope(project, scope);
    if (adjustedScope == null) {
      return;
    }
    FileTypeIndex.processFiles(InfraImportsFileType.FILE_TYPE, new Processor<VirtualFile>() {
      public boolean process(VirtualFile it) {
        for (Object name : names) {
          String name2 = (String) name;
          String name3 = it.getName();
          if (StringsKt.startsWith(name3, name2, false)) {
            PsiFile psiFile = elementToSearch.getManager().findFile(it);
            if (psiFile == null) {
              return true;
            }
            for (PsiReference reference : psiFile.getReferences()) {
              if (Intrinsics.areEqual(reference.getCanonicalText(), name2) && !processor.process(reference)) {
                return false;
              }
            }
          }
        }
        return true;
      }
    }, adjustedScope);
  }
}
