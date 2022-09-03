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

import com.intellij.ide.presentation.Presentation;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.PomRenameableTarget;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;

import java.util.Objects;

import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.PresentationConstant.INFRA_PROFILE;

@Presentation(typeName = INFRA_PROFILE)
public class InfraProfileTarget implements PomRenameableTarget<Object> {
  private String name;
  private final int nameOffset;
  private final SmartPsiElementPointer<PsiElement> elementPointer;

  public InfraProfileTarget(PsiElement element, String name, int nameOffset) {
    PsiFile containingFile = element.getContainingFile();
    Project project = containingFile == null ? element.getProject() : containingFile.getProject();
    this.elementPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(element, containingFile);
    this.nameOffset = nameOffset;
    this.name = name;
  }

  @Override
  public boolean isWritable() {
    return true;
  }

  @Override
  public Object setName(String name) {
    this.name = name;
    return null;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isValid() {
    return elementPointer.getElement() != null;
  }

  @Override
  public void navigate(boolean requestFocus) {
    Segment elementRange = elementPointer.getRange();
    if (elementRange == null) {
      return;
    }

    int offset = elementRange.getStartOffset();
    if (nameOffset < elementRange.getEndOffset() - offset) {
      offset += nameOffset;
    }
    VirtualFile virtualFile = elementPointer.getVirtualFile();
    if (virtualFile != null && virtualFile.isValid()) {
      PsiNavigationSupport.getInstance().createNavigatable(elementPointer.getProject(), virtualFile, offset).navigate(requestFocus);
    }
  }

  @Override
  public boolean canNavigate() {
    return canNavigateToSource();
  }

  @Override
  public boolean canNavigateToSource() {
    if (nameOffset < 0) {
      return false;
    }

    PsiElement element = elementPointer.getElement();
    return element != null && PsiNavigationSupport.getInstance().canNavigate(element);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;

    if (o == null || getClass() != o.getClass())
      return false;

    InfraProfileTarget target = (InfraProfileTarget) o;
    return Objects.equals(name, target.name);
  }

  @Override
  public int hashCode() {
    return name == null ? 0 : name.hashCode();
  }

  @Nullable
  public PsiElement getElement() {
    return elementPointer.getElement();
  }
}
