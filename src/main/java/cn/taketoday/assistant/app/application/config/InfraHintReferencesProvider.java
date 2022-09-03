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
package cn.taketoday.assistant.app.application.config;

import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;

import java.util.List;

import cn.taketoday.lang.Nullable;

/**
 * Gets references provided by SB 1.3 metadata and/or manual metadata.
 */
public abstract class InfraHintReferencesProvider {
  public static InfraHintReferencesProvider getInstance() {
    return ApplicationManager.getApplication().getService(InfraHintReferencesProvider.class);
  }

  /**
   * Available in ProcessingContext.
   */
  public static final Key<MetaConfigKey> HINT_REFERENCES_CONFIG_KEY = Key.create("HINT_REFERENCES_CONFIG_KEY");

  /**
   * Config key text in properties format.
   * Callers of this interface must provide it via ProcessingContext.
   */
  public static final Key<String> HINT_REFERENCES_CONFIG_KEY_TEXT = Key.create("HINT_REFERENCES_CONFIG_KEY_TEXT");

  /**
   * Available in ProcessingContext.
   */
  public static final Key<String> HINT_REFERENCES_MAP_KEY_PREFIX = Key.create("HINT_REFERENCES_MAP_KEY_PREFIX");

  /**
   * Gets additional key references (via configured hints).
   *
   * @param configKey Current config key.
   * @param keyPsiElement Current key element.
   * @param textRange Map key text range.
   * @param context Context, {@link #HINT_REFERENCES_CONFIG_KEY} is made available.
   * @return References.
   */
  public abstract PsiReference[] getKeyReferences(MetaConfigKey configKey,
          PsiElement keyPsiElement, TextRange textRange, ProcessingContext context);

  /**
   * Gets value references (hints, by type, key POJO path, explicit/fallback config).
   *
   * @param module Current module.
   * @param configKey Current config key.
   * @param keyPsiElement (Optional) The corresponding key element to determine value provider by POJO property (if applicable).
   * @param valuePsiElement Current value element.
   * @param valueTextRanges Text range(s) for value text(s).
   * @param context Context, {@link #HINT_REFERENCES_CONFIG_KEY} is made available.
   * @return References.
   */
  public abstract PsiReference[] getValueReferences(Module module,
          MetaConfigKey configKey,
          @Nullable PsiElement keyPsiElement,
          PsiElement valuePsiElement,
          List<TextRange> valueTextRanges,
          ProcessingContext context);
}
