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

package cn.taketoday.assistant.app.application.yaml;

import com.intellij.lang.Language;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLValue;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.application.metadata.additional.BaseDefineLocalMetaConfigKeyIntention;

public class InfraApplicationYamlDefineLocalMetaConfigKeyIntention extends BaseDefineLocalMetaConfigKeyIntention {

  public String getFamilyName() {
    return InfraAppBundle.message("DefineLocalMetaConfigKeyFix.define.yaml.configuration.key");
  }

  @Override
  public boolean isAvailable(PsiElement element) {
    PsiElement parent = element.getParent();
    if (!(parent instanceof YAMLKeyValue yamlKeyValue)) {
      return false;
    }
    if (yamlKeyValue.getKey() != element) {
      return false;
    }
    YAMLValue value = yamlKeyValue.getValue();
    if ((value instanceof YAMLScalar) || (value instanceof YAMLSequence)) {
      MetaConfigKey configKey = MetaConfigKeyReference.getResolvedMetaConfigKey(yamlKeyValue);
      return configKey == null;
    }
    return false;
  }

  @Override
  protected boolean isAvailable(Language language) {
    return YAMLLanguage.INSTANCE.is(language);
  }

  public void invoke(Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    int offset = editor.getCaretModel().getOffset();
    PsiElement element = file.findElementAt(offset);
    PsiElement parent = element == null ? null : element.getParent();
    if (!(parent instanceof YAMLKeyValue)) {
      return;
    }
    String keyName = ConfigYamlUtils.getQualifiedConfigKeyName((YAMLKeyValue) parent);
    invoke(project, file, editor, parent, keyName);
  }
}
