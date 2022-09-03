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

package cn.taketoday.assistant.model.utils.light;

import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.light.ImplicitVariableImpl;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;

import java.util.Objects;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.lang.Nullable;

public class InfraImplicitVariable extends ImplicitVariableImpl implements ItemPresentation {
  protected final PsiElement myDeclaration;

  public InfraImplicitVariable(String name, PsiType type, PsiElement declaration) {
    super(declaration.getManager(), InfraLightRenameableIdentifier.create(declaration, name), type, ElementManipulators.getManipulator(declaration) != null, declaration);
    myDeclaration = declaration;
  }

  @Override

  public PsiElement getNavigationElement() {
    return myDeclaration;
  }

  @Override
  public PsiElement setName(String name) throws IncorrectOperationException {
    PsiElement element = PsiImplUtil.setName(myNameIdentifier, name);
    if (element instanceof PsiIdentifier)
      myNameIdentifier = (PsiIdentifier) element;
    return this;
  }

  @Override
  public ItemPresentation getPresentation() {
    return this;
  }

  @Override
  @Nullable
  public PsiFile getContainingFile() {
    if (!isValid())
      throw new PsiInvalidElementAccessException(this);
    return myDeclaration != null ? myDeclaration.getContainingFile() : null;
  }

  @Override
  public String getText() {
    PsiIdentifier identifier = getNameIdentifier();
    return getType().getPresentableText() + " " + (identifier != null ? identifier.getText() : null);
  }

  @Override
  public String getPresentableText() {
    return getName();
  }

  @Override
  @Nullable
  public Icon getIcon(boolean open) {
    return Icons.Today;
  }

  @Override
  @Nullable
  public Icon getIcon(int flags) {
    return Icons.Today;
  }

  @Override
  public boolean isEquivalentTo(PsiElement another) {
    if (another == this)
      return true;
    if (another instanceof final InfraImplicitVariable implicitVariable) {
      String name = implicitVariable.getName();
      return name.equals(getName()) &&
              another.getManager().areElementsEquivalent(
                      implicitVariable.myDeclaration,
                      myDeclaration
              );
    }
    else {
      return getManager().areElementsEquivalent(myDeclaration, another);
    }
  }

  @Override

  public SearchScope getUseScope() {
    PsiFile file = (myDeclaration != null ? myDeclaration : getDeclarationScope()).getContainingFile();
    return file.getUseScope();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof InfraImplicitVariable variable))
      return false;

    return Objects.equals(getName(), variable.getName()) &&
            Objects.equals(myDeclaration, variable.myDeclaration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), myDeclaration);
  }

  public PsiElement getDeclaration() {
    return myDeclaration;
  }
}
