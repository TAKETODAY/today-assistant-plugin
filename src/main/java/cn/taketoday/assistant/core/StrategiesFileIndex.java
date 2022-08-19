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

package cn.taketoday.assistant.core;

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
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

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.util.CommonUtils;
import cn.taketoday.core.MultiValueMap;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/19 19:05
 */
public class StrategiesFileIndex extends FileBasedIndexExtension<String, List<String>> {

  private static final ID<String, List<String>> NAME = ID.create("today.StrategiesFileIndex");

  @Override
  public ID<String, List<String>> getName() {
    return NAME;
  }

  @Override
  public KeyDescriptor<String> getKeyDescriptor() {
    return EnumeratorStringDescriptor.INSTANCE;
  }

  @Override
  public DataIndexer<String, List<String>, FileContent> getIndexer() {
    return inputData -> {
//      byte[] content = inputData.getContent();
//        Properties properties = new Properties();
//
//        ExceptionUtils.sneakyThrow(() -> properties.load(new ByteArrayInputStream(content)));

//        MultiValueMap<String, String> strategies = new DefaultMultiValueMap<>();
//        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
//          Object key = entry.getKey();
//          Object value = entry.getValue();
//          if (key != null && value != null) {
//            String strategyKey = key.toString();
//            // split as string list
//            List<String> strategyValues = StringUtils.splitAsList(value.toString());
//            for (String strategyValue : strategyValues) {
//              strategyValue = strategyValue.trim(); // trim whitespace
//              if (StringUtils.isNotEmpty(strategyValue)) {
//                strategies.add(strategyKey, strategyValue);
//              }
//            }
//          }
//        }

      return MultiValueMap.defaults();
    };
  }

  @Override
  public DataExternalizer<List<String>> getValueExternalizer() {
    return StringCollectionExternalizer.STRING_LIST_EXTERNALIZER;
  }

  @Override
  public int getVersion() {
    return 3;
  }

  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return new DefaultFileTypeSpecificInputFilter(TodayStrategiesFileType.FILE_TYPE);
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  public static List<PsiClass> getClasses(Project project, GlobalSearchScope scope, String key) {
    GlobalSearchScope resolveScope = adjustScope(project, scope);
    if (resolveScope == null) {
      return CollectionsKt.emptyList();
    }
    else {
      HashMap<VirtualFile, List<String>> vfToValues = new HashMap<>();
      FileBasedIndex.getInstance().processValues(StrategiesFileIndex.NAME, key, null, (file, value) -> {
        vfToValues.put(file, value);
        return true;
      }, resolveScope);

      PsiManager psiManager = PsiManager.getInstance(project);

      JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
      Set<Map.Entry<VirtualFile, List<String>>> entries = vfToValues.entrySet();

      var ret = new ArrayList<PsiClass>();
      for (Map.Entry<VirtualFile, List<String>> entry : entries) {
        List<PsiClass> temp;
        label34:
        {
          PsiFile file = psiManager.findFile(entry.getKey());
          if (file != null) {
            resolveScope = file.getResolveScope();

            for (String fqn : entry.getValue()) {
              PsiClass psiClass = javaPsiFacade.findClass(fqn, resolveScope);
              if (psiClass != null) {
                ret.add(psiClass);
              }
            }

            temp = ret;
            break label34;
          }

          temp = Collections.emptyList();
        }

        ret.addAll(temp);
      }

      ret.forEach(System.err::println);
      return ret;
    }
  }

  public static boolean processValues(
          Project project, GlobalSearchScope scope,
          @Nullable String valueHint, PairProcessor<IProperty, PsiClass> processor) {
    GlobalSearchScope adjustedScope = adjustScope(project, scope);
    if (adjustedScope != null) {
      var collectKeysProcessor = new CollectUniquesProcessor<String>();
      FileBasedIndex.getInstance()
              .processAllKeys(StrategiesFileIndex.NAME, collectKeysProcessor, adjustedScope, null);

      PsiManager psiManager = PsiManager.getInstance(project);

      JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
      List<String> valueHintValueList = Collections.singletonList(valueHint);

      for (String key : collectKeysProcessor.getResults()) {
        var map = new HashMap<VirtualFile, List<String>>();
        FileBasedIndex.getInstance().processValues(StrategiesFileIndex.NAME, key, null,
                (file, value) -> {
                  if (valueHint == null) {
                    Intrinsics.checkNotNullExpressionValue(value, "value");
                    map.put(file, value);
                  }
                  else if (value.contains(valueHint)) {
                    map.put(file, valueHintValueList);
                  }
                  return true;
                }, adjustedScope);

        for (var entry : map.entrySet()) {
          PsiFile file = psiManager.findFile(entry.getKey());
          if (file == null) {
            throw new NullPointerException("null cannot be cast to non-null type com.intellij.lang.properties.psi.impl.PropertiesFileImpl");
          }

          if (file instanceof PropertiesFileImpl psiFile) {
            adjustedScope = psiFile.getResolveScope();
            GlobalSearchScope resolveScope = adjustedScope;
            IProperty property = psiFile.findPropertyByKey(key);

            for (String fqn : entry.getValue()) {
              PsiClass findClass = javaPsiFacade.findClass(fqn, resolveScope);
              if (findClass != null) {
                if (!processor.process(property, findClass)) {
                  return false;
                }
              }
            }
          }
        }
      }

    }
    return true;
  }

  private static GlobalSearchScope adjustScope(Project project, GlobalSearchScope scope) {
    return CommonUtils.getConfigFilesScope(project, scope);
  }

}
