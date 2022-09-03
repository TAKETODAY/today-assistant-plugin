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

package cn.taketoday.assistant.app.application.properties;

import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.microservices.jvm.config.ConfigKeyPathReference;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.beanProperties.BeanProperty;
import com.intellij.psi.impl.beanProperties.BeanPropertyElement;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.refactoring.rename.BeanPropertyRenameHandler;

import cn.taketoday.lang.Nullable;

public class InfraApplicationPropertiesConfigKeyPathBeanPropertyRenameHandler extends BeanPropertyRenameHandler {
  @Nullable
  protected BeanProperty getProperty(DataContext context) {
    Editor editor = CommonDataKeys.EDITOR.getData(context);
    PsiFile file = CommonDataKeys.PSI_FILE.getData(context);
    if (editor == null || !(file instanceof PropertiesFile)) {
      return null;
    }
    PsiReference findReferenceAt = file.findReferenceAt(editor.getCaretModel().getOffset());
    if (!(findReferenceAt instanceof ConfigKeyPathReference)) {
      return null;
    }
    ConfigKeyPathReference.PathType type = ((ConfigKeyPathReference) findReferenceAt).getPathType();
    if (type != ConfigKeyPathReference.PathType.BEAN_PROPERTY) {
      return null;
    }
    PsiElement resolve = findReferenceAt.resolve();
    if (resolve instanceof BeanPropertyElement beanPropertyElement) {
      PsiMethod method = beanPropertyElement.getMethod();
      if (PropertyUtilBase.isSimplePropertyAccessor(method, true)) {
        return BeanProperty.createBeanProperty(method, true);
      }
      return null;
    }
    return null;
  }
}
