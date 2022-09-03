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

import com.intellij.lang.Language;
import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.application.metadata.additional.BaseDefineLocalMetaConfigKeyIntention;

public class InfraApplicationPropertiesDefineLocalMetaConfigKeyIntention extends BaseDefineLocalMetaConfigKeyIntention {

  public String getFamilyName() {
    return InfraAppBundle.message("DefineLocalMetaConfigKeyFix.define.properties.configuration.key");
  }

  @Override
  public boolean isAvailable(PsiElement element) {
    if (!(element instanceof PropertyKeyImpl)) {
      return false;
    }
    MetaConfigKey configKey = MetaConfigKeyReference.getResolvedMetaConfigKey(element);
    return configKey == null;
  }

  @Override
  protected boolean isAvailable(Language language) {
    return PropertiesLanguage.INSTANCE.is(language);
  }

  public void invoke(Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    String keyName;
    int offset = editor.getCaretModel().getOffset();
    PsiElement element = file.findElementAt(offset);
    PsiElement parent = element == null ? null : element.getParent();
    if ((parent instanceof PropertyImpl) && (keyName = ((PropertyImpl) parent).getName()) != null) {
      invoke(project, file, editor, parent, keyName);
    }
  }
}
