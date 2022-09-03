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

import com.intellij.psi.ElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.impl.light.LightIdentifier;
import com.intellij.util.IncorrectOperationException;

public class InfraLightRenameableIdentifier extends LightIdentifier {
  private final PsiElement myDeclaration;

  public static InfraLightRenameableIdentifier create(PsiElement declaration, String name) {
    return new InfraLightRenameableIdentifier(declaration, name);
  }

  private InfraLightRenameableIdentifier(PsiElement declaration, String name) {
    super(declaration.getManager(), name);
    myDeclaration = declaration;
  }

  @Override
  public PsiElement replace(PsiElement newElement) throws IncorrectOperationException {
    String newName = newElement.getText();
    if (myDeclaration.isValid()) {
      ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(myDeclaration);
      if (manipulator != null) {
        manipulator.handleContentChange(myDeclaration, newName);
      }
      else if (myDeclaration instanceof PsiNamedElement) {
        ((PsiNamedElement) myDeclaration).setName(newName);
      }
    }

    return create(myDeclaration, newName);
  }

  @Override
  public boolean isValid() {
    return myDeclaration == null || myDeclaration.isValid();
  }
}
