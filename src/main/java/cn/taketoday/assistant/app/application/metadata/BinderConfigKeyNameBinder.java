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

package cn.taketoday.assistant.app.application.metadata;

import com.intellij.microservices.jvm.config.ConfigKeyParts;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyManager;
import com.intellij.openapi.util.text.CharFilter;
import com.intellij.openapi.util.text.StringUtil;

import cn.taketoday.lang.Nullable;

class BinderConfigKeyNameBinder implements MetaConfigKeyManager.ConfigKeyNameBinder {
  private static final CharFilter UNIFORM_CHAR_FILTER = ch -> Character.isLetterOrDigit(ch) || ch == '.';

  public boolean bindsTo(MetaConfigKey key, String configKeyText) {
    if (!matchesFirstChar(key.getName(), configKeyText)) {
      return false;
    }
    String uniformKeyName = toUniform(key.getName());
    String cleanupConfigKeyText = toUniform(configKeyText);
    boolean exactMatch = uniformKeyName.equals(cleanupConfigKeyText);
    if (key.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      return exactMatch;
    }
    if (key.isAccessType(MetaConfigKey.AccessType.MAP_GROUP)) {
      return exactMatch || StringUtil.startsWith(cleanupConfigKeyText, uniformKeyName + ".");
    }
    else if (key.isAccessType(MetaConfigKey.AccessType.INDEXED)) {
      if (exactMatch) {
        return true;
      }
      if (!StringUtil.containsChar(configKeyText, '[')) {
        return false;
      }
      String beforeIndexAccess = StringUtil.substringBefore(configKeyText, "[");
      return uniformKeyName.equals(toUniform(StringUtil.trimEnd(beforeIndexAccess, '.')));
    }
    else {
      throw new IllegalArgumentException("unknown access type for " + key);
    }
  }

  public boolean matchesPrefix(MetaConfigKey key, String prefixText) {
    if (!matchesFirstChar(key.getName(), prefixText)) {
      return false;
    }
    String uniformKeyName = toUniform(key.getName());
    return StringUtil.startsWith(uniformKeyName, toUniform(prefixText));
  }

  public boolean matchesPart(String keyPart, String text) {
    if (!matchesFirstChar(keyPart, text)) {
      return false;
    }
    return matches(keyPart, text);
  }

  @Nullable
  public String bindsToKeyProperty(MetaConfigKey key, @Nullable String keyProperty, String configKeyText) {
    ConfigKeyParts parts;
    if (key.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      return null;
    }
    String keyName = key.getName();
    if (!matchesFirstChar(keyName, configKeyText) || (parts = ConfigKeyParts.splitToParts(key, configKeyText, true)) == null) {
      return null;
    }
    return parts.getKeyIndexIfMatches(keyName, keyProperty, BinderConfigKeyNameBinder::matches);
  }

  private static boolean matchesFirstChar(String keyName, String configKeyText) {
    return !configKeyText.isEmpty() && StringUtil.charsEqualIgnoreCase(keyName.charAt(0), configKeyText.charAt(0));
  }

  private static boolean matches(String keyName, String configKey) {
    return toUniform(keyName).equals(toUniform(configKey));
  }

  private static String toUniform(String input) {
    String lowerCase = StringUtil.toLowerCase(input);
    if (StringUtil.isLatinAlphanumeric(lowerCase)) {
      return lowerCase;
    }
    return StringUtil.strip(lowerCase, UNIFORM_CHAR_FILTER);
  }

}
