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

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.CharTailType;
import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.highlighting.HighlightedReference;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.TailTypeDecorator;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import kotlin.jvm.internal.Intrinsics;

final class DynamicPropertyRegistryReference extends MetaConfigKeyReference<PsiElement> implements HighlightedReference {
  private final TailType MAP_DOT_TAIL;

  private final PsiElement psiElement;

  private final String propertyName;

  public PsiElement getPsiElement() {
    return this.psiElement;
  }

  public String getPropertyName() {
    return this.propertyName;
  }

  public DynamicPropertyRegistryReference(PsiElement psiElement, String propertyName) {
    super(InfraApplicationMetaConfigKeyManager.getInstance(), psiElement, propertyName);
    this.psiElement = psiElement;
    this.propertyName = propertyName;
    this.MAP_DOT_TAIL = new CharTailType('.') {
      public int processTail(Editor editor, int tailOffset) {
        int offset = super.processTail(editor, tailOffset);
        Project project = editor.getProject();
        Intrinsics.checkNotNull(project);
        AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
        return offset;
      }
    };
  }

  public String getReferenceDisplayText() {
    return this.propertyName;
  }

  public Object[] getVariants() {
    List<MetaConfigKey> configKeys = (List<MetaConfigKey>) getConfigKeyManager().getAllMetaConfigKeys(getElement());
    List result = new ArrayList(configKeys.size());
    for (MetaConfigKey configKey : configKeys) {
      MetaConfigKey.MetaConfigKeyPresentation presentation = configKey.getPresentation();
      LookupElement lookupElement = presentation.getLookupElement();
      MetaConfigKey.AccessType[] accessTypeArr = MetaConfigKey.AccessType.MAP_GROUP;
      TailType tailType = configKey.isAccessType(Arrays.copyOf(accessTypeArr, accessTypeArr.length)) ? this.MAP_DOT_TAIL : null;
      LookupElement tuneLookupElement = configKey.getPresentation().tuneLookupElement(TailTypeDecorator.withTail(lookupElement, tailType));
      result.add(tuneLookupElement);
    }
    return result.toArray(new Object[0]);
  }
}
