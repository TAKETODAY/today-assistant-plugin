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

package cn.taketoday.assistant.model.config;

import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/28 12:37
 */
public class ConfigurationValueResult {

  private final PsiElement keyElement;
  @Nullable
  private final String keyIndexText;
  @Nullable
  private final PsiElement valueElement;
  @Nullable
  private final String valueText;
  private final int documentId;

  private final ConfigurationValueSearchParams params;

  public ConfigurationValueResult(PsiElement keyElement, @Nullable String keyIndexText,
          @Nullable PsiElement valueElement, @Nullable String valueText, int documentId,
          ConfigurationValueSearchParams params) {
    this.keyElement = keyElement;
    this.keyIndexText = keyIndexText;
    this.valueElement = valueElement;
    this.valueText = valueText;
    this.documentId = documentId;
    this.params = params;
  }

  public MetaConfigKeyReference getMetaConfigKeyReference() {
    MetaConfigKeyReference metaConfigKeyReference = ContainerUtil.findInstance(this.keyElement.getReferences(), MetaConfigKeyReference.class);
    if (metaConfigKeyReference == null) {
      String refText = StringUtil.join(this.keyElement.getReferences(), psiReference -> psiReference.getClass().getName(), "|");
      Class var10002 = this.keyElement.getClass();
      throw new IllegalStateException("" + var10002 + " - '" + this.keyElement.getText() + "' - " + refText + "}");
    }
    else {
      return metaConfigKeyReference;
    }
  }

  public final PsiElement getKeyElement() {
    return this.keyElement;
  }

  @Nullable
  public final String getKeyIndexText() {
    return this.keyIndexText;
  }

  @Nullable
  public final PsiElement getValueElement() {
    return this.valueElement;
  }

  @Nullable
  public final String getValueText() {
    return this.valueText;
  }

  public final int getDocumentId() {
    return this.documentId;
  }

  public final ConfigurationValueSearchParams getParams() {
    return this.params;
  }

}
