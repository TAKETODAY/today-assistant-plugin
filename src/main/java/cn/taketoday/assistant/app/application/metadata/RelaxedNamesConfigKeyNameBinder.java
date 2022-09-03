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
import com.intellij.microservices.jvm.config.RelaxedNames;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ConcurrentFactoryMap;

import java.util.Map;

import cn.taketoday.lang.Nullable;

class RelaxedNamesConfigKeyNameBinder implements MetaConfigKeyManager.ConfigKeyNameBinder {
  private static final Map<String, RelaxedNames> ourRelaxedNamesCache = ConcurrentFactoryMap.createMap(keyName -> {
    int lastDotIdx = keyName.lastIndexOf(46);
    if (lastDotIdx == -1) {
      return new RelaxedNames(keyName);
    }
    String propertyName = RelaxedNames.dashedPropertyNameToCamelCase(keyName);
    String replace = keyName.substring(0, lastDotIdx + 1) + propertyName;
    return new RelaxedNames(replace);
  });

  public boolean bindsTo(MetaConfigKey key, String configKeyText) {
    String keyName = key.getName();
    if (!matchesFirstChar(keyName, configKeyText)) {
      return false;
    }
    if (key.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      return matches(keyName, configKeyText);
    }
    if (key.isAccessType(MetaConfigKey.AccessType.MAP_GROUP)) {
      return matchesMapType(key, configKeyText);
    }
    if (key.isAccessType(MetaConfigKey.AccessType.INDEXED)) {
      return matchesIndexedType(keyName, configKeyText);
    }
    throw new IllegalArgumentException("unknown access type for " + key);
  }

  public boolean matchesPrefix(MetaConfigKey key, String prefixText) {
    String keyName = key.getName();
    if (!matchesFirstChar(keyName, prefixText)) {
      return false;
    }
    if (StringUtil.startsWith(keyName, prefixText)) {
      return true;
    }
    for (String value : getRelaxedNames(keyName).getValues()) {
      if (value.startsWith(prefixText)) {
        return true;
      }
    }
    return false;
  }

  public boolean matchesPart(String keyPart, String text) {
    if (!matchesFirstChar(keyPart, text) || !matchesLastChar(keyPart, text)) {
      return false;
    }
    return keyPart.equalsIgnoreCase(text) || new RelaxedNames(keyPart).getValues().contains(text);
  }

  @Nullable
  public String bindsToKeyProperty(MetaConfigKey key, @Nullable String keyProperty, String configKeyText) {
    ConfigKeyParts parts;
    if (key.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      return null;
    }
    String keyName = key.getName();
    if (!matchesFirstChar(keyName, configKeyText) || (parts = ConfigKeyParts.splitToParts(key, configKeyText, false)) == null) {
      return null;
    }
    return parts.getKeyIndexIfMatches(keyName, keyProperty, RelaxedNamesConfigKeyNameBinder::matches);
  }

  private static boolean matchesFirstChar(String keyName, String configKeyText) {
    return !configKeyText.isEmpty() && StringUtil.charsEqualIgnoreCase(keyName.charAt(0), configKeyText.charAt(0));
  }

  private static boolean matchesMapType(MetaConfigKey key, String configKeyText) {
    String keyName = key.getName();
    if (keyName.equalsIgnoreCase(configKeyText)) {
      return true;
    }
    ConfigKeyParts parts = ConfigKeyParts.splitToParts(key, configKeyText, false);
    return parts != null && parts.matchesKeyName(keyName, RelaxedNamesConfigKeyNameBinder::matches);
  }

  private static boolean matchesIndexedType(String keyName, String configKeyText) {
    int indexKeyIndex = configKeyText.indexOf(91);
    String propertyName = indexKeyIndex != -1 ? StringUtil.trimEnd(configKeyText.substring(0, indexKeyIndex), '.') : configKeyText;
    return matches(keyName, propertyName);
  }

  private static boolean matches(String keyName, String configKey) {
    return keyName.equalsIgnoreCase(configKey) || matchesRelaxed(keyName, configKey);
  }

  private static boolean matchesRelaxed(String keyName, String configKey) {
    if (!matchesLastChar(keyName, configKey)) {
      return false;
    }
    return getRelaxedNames(keyName).getValues().contains(configKey);
  }

  private static RelaxedNames getRelaxedNames(String keyName) {
    return ourRelaxedNamesCache.get(keyName);
  }

  private static boolean matchesLastChar(String keyName, String configKey) {
    return !configKey.isEmpty() && StringUtil.charsEqualIgnoreCase(keyName.charAt(keyName.length() - 1), configKey.charAt(configKey.length() - 1));
  }
}
