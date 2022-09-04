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
package cn.taketoday.assistant.profiles;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.containers.ContainerUtil;

import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.Icons;

/**
 * @author Sergey Vasiliev
 * @see InfraProfilesFactory#getProfilesReferences(Module, PsiElement, String, int, String, boolean)
 */
public class InfraProfilePsiReference extends PsiReferenceBase<PsiElement> {
  private final Module myModule;
  private final String myProfileName;
  private final boolean myIsDefinition;

  public InfraProfilePsiReference(PsiElement element,
          TextRange range, Module module, boolean definition) {
    super(element, range);
    myIsDefinition = definition;
    myProfileName = range.substring(element.getText());
    myModule = module;
  }

  @Override
  public PsiElement resolve() {
    if (StringUtil.isEmptyOrSpaces(myProfileName))
      return myElement;

    InfraProfileTarget target;
    if (myIsDefinition) {
      target = new InfraProfileTarget(getElement(), myProfileName, getRangeInElement().getStartOffset());
    }
    else {
      target = InfraProfilesFactory.of().findProfileTarget(myModule, includeTestScope(), myProfileName);
    }
    return target == null ? null : PomService.convertToPsi(getElement().getProject(), target);
  }

  @Override
  public boolean isSoft() {
    return true;
  }

  @Override
  public Object[] getVariants() {
    Set<String> names = new LinkedHashSet<>();
    for (InfraProfileTarget target : InfraProfilesFactory.of().findProfileTargets(myModule, includeTestScope())) {
      names.add(target.getName());
    }
    return ContainerUtil.map2Array(names, Object.class, name -> LookupElementBuilder.create(name).withIcon(Icons.SpringProfile));
  }

  private boolean includeTestScope() {
    PsiFile file = myElement.getContainingFile();
    if (file == null)
      return false;

    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null)
      return false;

    return ModuleRootManager.getInstance(myModule).getFileIndex().isInTestSourceContent(virtualFile);
  }
}
