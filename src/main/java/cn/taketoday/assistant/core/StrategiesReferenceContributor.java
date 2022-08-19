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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;

import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.Icons;
import cn.taketoday.lang.Constant;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/19 18:37
 */
public class StrategiesReferenceContributor extends PsiReferenceContributor {

  @Override
  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PropertyKeyImpl.class)
                    .inVirtualFile(PlatformPatterns.virtualFile().ofType(TodayStrategiesFileType.FILE_TYPE)), new PsiReferenceProvider() {
              @Override
              public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
                String text = element.getText();
                String[] words = text.split("\\s");
                if (words.length != 1) {
                  return PsiReference.EMPTY_ARRAY;
                }
                var classReferences = StrategiesClassReferenceProvider.CLASS_REFERENCE_PROVIDER.getReferencesByString(words[0], element, 0);
                return ArrayUtil.append(classReferences, new KeyReference(element), PsiReference.class);
              }
            }, 100.0);
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyValueImpl.class)
                    .inVirtualFile(
                            PlatformPatterns.virtualFile()
                                    .ofType(TodayStrategiesFileType.FILE_TYPE)
                    ),
            StrategiesClassReferenceProvider.INSTANCE, 100.0);
  }

  private static final class KeyReference extends PsiReferenceBase.Immediate<PsiElement> {
    private static final Set<String> DEFAULT_KEYS = Set.of(
            "cn.taketoday.context.ApplicationListener",
            "cn.taketoday.context.ApplicationContextInitializer",
            "cn.taketoday.test.context.TestExecutionListener",
            "cn.taketoday.test.context.ContextCustomizerFactory",
            "cn.taketoday.context.annotation.config.EnableAutoConfiguration"
    );

    private KeyReference(PsiElement element) {
      super(element, true, element);
    }

    @Override
    public Object[] getVariants() {
      PsiFile containingFile = getElement().getContainingFile().getOriginalFile();
      if (containingFile instanceof PropertiesFile propertiesFile) {
        Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
        if (module == null) {
          return Constant.EMPTY_OBJECTS;
        }

        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(module.getProject());
        GlobalSearchScope scope = containingFile.getResolveScope();
        List<LookupElement> variants = new SmartList<>();

        for (String key : DEFAULT_KEYS) {
          if (propertiesFile.findPropertyByKey(key) == null) {
            addKeyVariant(javaPsiFacade, scope, variants, key);
          }
        }

        boolean isInTestSources = ModuleRootManager.getInstance(module)
                .getFileIndex()
                .isInTestSourceContent(containingFile.getVirtualFile());

        var strategiesFiles = StrategiesManager.from(module).getStrategiesFiles(isInTestSources);
        for (PropertiesFile file : strategiesFiles) {
          for (IProperty property : file.getProperties()) {
            String name = property.getName();
            if (!DEFAULT_KEYS.contains(name) && propertiesFile.findPropertyByKey(name) == null) {
              addKeyVariant(javaPsiFacade, scope, variants, name);
            }
          }
        }

        return variants.toArray();
      }
      else {
        return Constant.EMPTY_OBJECTS;
      }
    }

    private static void addKeyVariant(JavaPsiFacade javaPsiFacade,
            GlobalSearchScope scope, List<LookupElement> variants, String key) {
      PsiClass psiClass = javaPsiFacade.findClass(key, scope);
      if (psiClass != null) {
        variants.add(LookupElementBuilder.create(psiClass, key).withIcon(Icons.Today));
      }
    }
  }
}
