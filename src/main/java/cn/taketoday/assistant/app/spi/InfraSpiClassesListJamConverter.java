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

import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

import cn.taketoday.assistant.core.StrategiesClassesListJamConverter;
import cn.taketoday.assistant.core.StrategiesManager;
import cn.taketoday.lang.Nullable;
import kotlin.collections.CollectionsKt;

public final class InfraSpiClassesListJamConverter extends StrategiesClassesListJamConverter {
  private final String importKey;

  public InfraSpiClassesListJamConverter(String configKey, String importKey) {
    super(configKey);
    this.importKey = importKey;
  }

  public PsiReference[] createReferences(JamStringAttributeElement<PsiClass> attributeElement, PsiLanguageInjectionHost injectionHost) {
    return new PsiReference[] {
            new InfraSpiClassReference(myConfigKey, importKey,
                    injectionHost, null, attributeElement.getStringValue())
    };
  }

  public static final class InfraSpiClassReference extends ClassReference {
    private final String importKey;

    public InfraSpiClassReference(String configKey, String importKey,
            PsiElement literal, @Nullable TextRange rangeInElement, @Nullable String text) {
      super(configKey, literal, rangeInElement, text);
      this.importKey = importKey;
    }

    protected List<PsiClass> getRelevantClasses(@Nullable PsiElement literal, String configKey) {
      if (literal == null) {
        return CollectionsKt.emptyList();
      }
      else {
        Module module = ModuleUtilCore.findModuleForPsiElement(literal);
        if (module == null) {
          return CollectionsKt.emptyList();
        }
        else {
          boolean isInTest;
          label22:
          {
            PsiFile originalFile = literal.getContainingFile().getOriginalFile();
            VirtualFile containingFile = originalFile.getVirtualFile();
            if (containingFile != null) {
              ModuleRootManager instance = ModuleRootManager.getInstance(module);
              if (instance.getFileIndex().isInTestSourceContent(containingFile)) {
                isInTest = true;
                break label22;
              }
            }

            isInTest = false;
          }

          List<PsiClass> result = StrategiesManager.from(module).getClassesListValue(isInTest, configKey);
          List<PsiClass> imports = InfraImportsManager.from(module).getClasses(isInTest, this.importKey);
          return ContainerUtil.concat(result, imports);
        }
      }
    }
  }
}
