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
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.hints.HintReferenceProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PackageReferenceSet;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PackageScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.CommonProcessors;
import com.intellij.util.ProcessingContext;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.app.InfraModelConfigFileContributor;
import cn.taketoday.assistant.app.application.config.InfraHintReferencesProvider;
import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.ConfigurationValueSearchParams;
import cn.taketoday.lang.Nullable;

public class LoggerNameReferenceProvider implements HintReferenceProvider {
  private final boolean myResolveGroups;

  public LoggerNameReferenceProvider(boolean resolveGroups) {
    this.myResolveGroups = resolveGroups;
  }

  public PsiReference[] getReferences(PsiElement element, List<TextRange> textRanges, ProcessingContext context) {
    String elementText = element.getText();
    GlobalSearchScope resolveScope = element.getResolveScope();
    String importValue = context.get(InfraHintReferencesProvider.HINT_REFERENCES_MAP_KEY_PREFIX);
    NotNullLazyValue<GlobalSearchScope> referenceScope = NotNullLazyValue.lazy(() -> {
      if (importValue == null) {
        return resolveScope;
      }
      PsiPackage psiPackage = JavaPsiFacade.getInstance(element.getProject()).findPackage(importValue);
      if (psiPackage == null) {
        return GlobalSearchScope.EMPTY_SCOPE;
      }
      return resolveScope.intersectWith(new PackageScope(psiPackage, true, true));
    });
    JavaClassReferenceProvider classReferenceProvider = new JavaClassReferenceProvider() {

      public GlobalSearchScope getScope(Project project) {
        return referenceScope.get();
      }
    };
    classReferenceProvider.setSoft(true);
    if (importValue != null) {
      classReferenceProvider.setOption(JavaClassReferenceProvider.IMPORTS, Collections.singletonList(importValue));
    }
    PsiReference[] allReferences = PsiReference.EMPTY_ARRAY;
    for (TextRange textRange : textRanges) {
      String rangeText = textRange.substring(elementText);
      if (!StringUtil.contains(rangeText, "${")) {
        int offset = textRange.getStartOffset();
        JavaClassReferenceSet javaClassReferenceSet = new JavaClassReferenceSet(rangeText, element, offset, false, classReferenceProvider);
        PackageReferenceSet packageReferenceSet = new PackageReferenceSet(rangeText, element, offset, resolveScope) {
          public boolean isSoft() {
            return true;
          }

          public PsiPackageReference createReference(TextRange range, int index) {
            return new PsiPackageReference(this, range, index) {

              public Object[] getVariants() {
                return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
              }
            };
          }
        };
        PsiReference[] rangeReferences = ArrayUtil.mergeArrays(javaClassReferenceSet.getReferences(), packageReferenceSet.getPsiReferences());
        allReferences = ArrayUtil.mergeArrays(allReferences, rangeReferences);
        if (this.myResolveGroups) {
          allReferences = ArrayUtil.append(allReferences, new LoggingGroupReference(element, textRange));
        }
      }
    }
    return allReferences;
  }

  private static final class LoggingGroupReference extends PsiReferenceBase<PsiElement> {
    private static final String LOGGING_GROUP_KEY = "logging.group";

    private LoggingGroupReference(PsiElement element, TextRange range) {
      super(element, range, true);
    }

    @Nullable
    public PsiElement resolve() {
      String groupName = getCanonicalText();
      CommonProcessors.FindProcessor<ConfigurationValueResult> resolveProcessor = new CommonProcessors.FindProcessor<ConfigurationValueResult>() {
        public boolean accept(ConfigurationValueResult result) {
          String keyText = result.getKeyElement().getText();
          return StringUtil.endsWith(keyText, groupName);
        }
      };
      processLoggingGroups(resolveProcessor);
      ConfigurationValueResult value = resolveProcessor.getFoundValue();
      if (value != null) {
        return value.getKeyElement();
      }
      return null;
    }

    public Object[] getVariants() {
      SmartList smartList = new SmartList();
      Processor<ConfigurationValueResult> variantProcessor = result -> {
        PsiElement keyElement = result.getKeyElement();
        String logGroupName = StringUtil.substringAfter(keyElement.getText(), "logging.group.");
        if (StringUtil.isNotEmpty(logGroupName)) {
          smartList.add(
                  LookupElementBuilder.createWithSmartPointer(logGroupName, keyElement)
                          .withIcon(Icons.Today).withTailText(" (" + result.getValueText() + ")", true)
                          .withTypeText(keyElement.getContainingFile().getName())
          );
          return true;
        }
        return true;
      };
      processLoggingGroups(variantProcessor);
      Object[] array = smartList.toArray();
      return array;
    }

    private void processLoggingGroups(Processor<ConfigurationValueResult> processor) {
      MetaConfigKey loggingGroupKey;
      PsiFile psiFile;
      Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
      if (module == null || (loggingGroupKey = InfraApplicationMetaConfigKeyManager.getInstance().findCanonicalApplicationMetaConfigKey(module, LOGGING_GROUP_KEY)) == null) {
        return;
      }
      PsiManager psiManager = PsiManager.getInstance(module.getProject());
      ConfigurationValueSearchParams params = new ConfigurationValueSearchParams(module, loggingGroupKey);
      for (VirtualFile file : InfraConfigurationFileService.of().findConfigFiles(module, true)) {
        InfraModelConfigFileContributor contributor = InfraModelConfigFileContributor.getContributor(file);
        if (contributor != null && (psiFile = psiManager.findFile(file)) != null) {
          List<ConfigurationValueResult> values = contributor.findConfigurationValues(psiFile, params);
          if (!ContainerUtil.process(values, processor)) {
            return;
          }
        }
      }
    }
  }
}
