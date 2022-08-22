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

import com.intellij.codeInsight.completion.JavaLookupElementBuilder;
import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.jam.JamConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.spring.spi.SpringSpiManager;
import com.intellij.util.containers.ContainerUtil;

import cn.taketoday.lang.Nullable;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.InfraBundle;

public class StrategiesClassesListJamConverter extends JamConverter<PsiClass> {
  protected final String myConfigKey;

  @Nullable
  public Object m530fromString(@Nullable String str, JamStringAttributeElement jamStringAttributeElement) {
    return fromString(str, (JamStringAttributeElement<PsiClass>) jamStringAttributeElement);
  }

  public StrategiesClassesListJamConverter(String configKey) {
    this.myConfigKey = configKey;
  }

  @Nullable
  public PsiClass fromString(@Nullable String s, JamStringAttributeElement<PsiClass> context) {
    PsiLanguageInjectionHost host;
    if (!StringUtil.isEmptyOrSpaces(s) && (host = context.getLanguageInjectionHost()) != null) {
      PsiReference[] references = createReferences(context, host);
      if (references.length == 1) {
        return (PsiClass) references[0].resolve();
      }
      return null;
    }
    return null;
  }

  @Override
  public PsiReference[] createReferences(JamStringAttributeElement<PsiClass> context, PsiLanguageInjectionHost injectionHost) {
    return new PsiReference[] {
            new SpringSpiClassReference(this.myConfigKey, injectionHost, context.getStringValue())
    };
  }

  public static class SpringSpiClassReference extends PsiReferenceBase<PsiElement> implements EmptyResolveMessageProvider {
    private final String myConfigKey;
    private final String myText;

    public SpringSpiClassReference(String configKey, PsiElement literal, String text) {
      super(literal);
      this.myConfigKey = configKey;
      this.myText = text;
    }

    public SpringSpiClassReference(String configKey, PsiElement element, TextRange rangeInElement, String text) {
      super(element, rangeInElement);
      this.myConfigKey = configKey;
      this.myText = text;
    }

    @Nullable
    public PsiElement resolve() {
      if (StringUtil.isEmptyOrSpaces(this.myText)) {
        return null;
      }
      return (PsiElement) ContainerUtil.find(getRelevantClasses(getElement(), this.myConfigKey), psiClass -> {
        return this.myText.equals(psiClass.getQualifiedName());
      });
    }

    @Override
    public Object[] getVariants() {
      List<PsiClass> classes = getRelevantClasses(getElement(), this.myConfigKey);
      return ContainerUtil.map2Array(classes, LookupElement.class, psiClass -> {
        return JavaLookupElementBuilder.forClass(psiClass, psiClass.getQualifiedName(), true).withPresentableText(StringUtil.notNullize(psiClass.getName()));
      });
    }

    protected List<PsiClass> getRelevantClasses(@Nullable PsiElement literal, String configKey) {
      if (literal == null) {
        return Collections.emptyList();
      }
      Module module = ModuleUtilCore.findModuleForPsiElement(literal);
      if (module == null) {
        return Collections.emptyList();
      }
      VirtualFile containingFile = literal.getContainingFile().getOriginalFile().getVirtualFile();
      boolean isInTest = containingFile != null && ModuleRootManager.getInstance(module).getFileIndex().isInTestSourceContent(containingFile);
      return SpringSpiManager.getInstance(module).getClassesListValue(isInTest, configKey);
    }

    
    public String getUnresolvedMessagePattern() {
      return InfraBundle.message("StrategiesClassesListJamConverter.unresolved.message.pattern", this.myText, this.myConfigKey);
    }
  }
}
