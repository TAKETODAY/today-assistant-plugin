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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PackageScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.spi.psi.SPIClassProviderReferenceElement;
import com.intellij.spi.psi.SPIFile;
import com.intellij.util.CommonProcessors.CollectUniquesProcessor;
import com.intellij.util.PairProcessor;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.externalizer.StringCollectionExternalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.lang.Nullable;
import kotlin.collections.CollectionsKt;
import kotlin.text.StringsKt;

public final class InfraImportsFileIndex extends FileBasedIndexExtension<String, List<? extends String>> {
  private static final ID<String, List<String>> NAME = ID.create("infrastructure.importsFileIndex");

  public static List<PsiClass> getClasses(Project project, GlobalSearchScope scope, String key) {
    GlobalSearchScope adjustedScope = adjustScope(project, scope);
    if (adjustedScope != null) {
      HashMap<VirtualFile, List<String>> vfToValues = new HashMap<>();
      FileBasedIndex.getInstance().processValues(NAME, key, null, (file, list) -> {
        vfToValues.put(file, list);
        return true;
      }, adjustedScope);
      PsiManager psiManager = PsiManager.getInstance(project);
      JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
      ArrayList<PsiClass> ret = new ArrayList<>();

      for (Map.Entry<VirtualFile, List<String>> valueKey : vfToValues.entrySet()) {
        VirtualFile key2 = valueKey.getKey();
        List<String> value = valueKey.getValue();
        PsiFile findFile = psiManager.findFile(key2);
        if (findFile != null) {
          GlobalSearchScope resolveScope = findFile.getResolveScope();
          for (String fqn : value) {
            PsiClass findClass = javaPsiFacade.findClass(fqn, resolveScope);
            if (findClass != null) {
              ret.add(findClass);
            }
          }
        }
      }
      return ret;
    }
    return CollectionsKt.emptyList();
  }

  public static boolean processValues(Project project, GlobalSearchScope scope,
          @Nullable String valueHint, PairProcessor<PsiElement, PsiClass> pairProcessor) {

    GlobalSearchScope adjustedScope = adjustScope(project, scope);
    if (adjustedScope != null) {
      var collector = new CollectUniquesProcessor<String>();
      FileBasedIndex.getInstance().processAllKeys(NAME, collector, adjustedScope, null);

      PsiManager psiManager = PsiManager.getInstance(project);
      JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
      List<String> valueHintValueList = Collections.singletonList(valueHint);
      for (String key : collector.getResults()) {
        var vfToValues = new HashMap<VirtualFile, List<String>>();

        FileBasedIndex.getInstance().processValues(NAME, key, null, (VirtualFile file, List<String> list) -> {
          if (valueHint == null) {
            vfToValues.put(file, list);
          }
          else if (list.contains(valueHint)) {
            vfToValues.put(file, valueHintValueList);
          }
          return true;
        }, adjustedScope);

        for (Map.Entry<VirtualFile, List<String>> entry : vfToValues.entrySet()) {
          PsiElement findFile = psiManager.findFile(entry.getKey());
          if (findFile != null) {
            GlobalSearchScope resolveScope = findFile.getResolveScope();
            var values = PsiTreeUtil.findChildrenOfType(findFile, SPIClassProviderReferenceElement.class);
            for (SPIClassProviderReferenceElement value : values) {
              String fqn = value.getText();
              PsiClass findClass;
              if (Objects.equals(fqn, valueHint)
                      && (findClass = javaPsiFacade.findClass(fqn, resolveScope)) != null) {
                if (!pairProcessor.process(value, findClass)) {
                  return false;
                }
              }
            }
          }
        }
      }
      return true;
    }
    return true;
  }

  @Nullable
  public static GlobalSearchScope adjustScope(Project project, GlobalSearchScope scope) {
    PsiPackage metaInfPackage = JavaPsiFacade.getInstance(project).findPackage("META-INF.config");
    if (metaInfPackage != null) {
      GlobalSearchScope packageScope = PackageScope.packageScope(metaInfPackage, false);
      return scope.intersectWith(packageScope);
    }
    return scope;
  }

  public ID getName() {
    return NAME;
  }

  public KeyDescriptor<String> getKeyDescriptor() {
    return EnumeratorStringDescriptor.INSTANCE;
  }

  @Override
  public DataIndexer getIndexer() {
    return InfraImportsFileIndexer.INSTANCE;
  }

  @Override
  public DataExternalizer getValueExternalizer() {
    return StringCollectionExternalizer.STRING_LIST_EXTERNALIZER;
  }

  public int getVersion() {
    return 1;
  }

  public FileBasedIndex.InputFilter getInputFilter() {
    return new DefaultFileTypeSpecificInputFilter(InfraImportsFileType.FILE_TYPE);
  }

  public boolean dependsOnFileContent() {
    return true;
  }

  static final class InfraImportsFileIndexer implements DataIndexer<String, List<String>, FileContent> {
    public static final InfraImportsFileIndexer INSTANCE = new InfraImportsFileIndexer();

    public Map<String, List<String>> map(FileContent inputData) {
      PsiFile psiFile = inputData.getPsiFile();

      if (psiFile instanceof SPIFile psiElement) {
        HashMap<String, List<String>> result = new HashMap<>();
        String key = FileUtil.getNameWithoutExtension(inputData.getFileName());
        var findChildrenOfType = PsiTreeUtil.findChildrenOfType(psiElement, SPIClassProviderReferenceElement.class);
        ArrayList<String> list = new ArrayList<>(Math.max(findChildrenOfType.size(), 10));
        for (SPIClassProviderReferenceElement it : findChildrenOfType) {
          String text = it.getText();
          list.add(StringsKt.replace(text, '$', '.', false));
        }
        result.put(key, list);
        return result;
      }
      return Collections.emptyMap();
    }

  }
}
