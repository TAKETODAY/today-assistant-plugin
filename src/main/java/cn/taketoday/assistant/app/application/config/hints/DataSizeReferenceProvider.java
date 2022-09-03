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

package cn.taketoday.assistant.app.application.config.hints;

import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.hints.HintReferenceBase;
import com.intellij.microservices.jvm.config.hints.HintReferenceProviderBase;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.assistant.app.InfraClassesConstants;
import cn.taketoday.assistant.app.application.config.InfraHintReferencesProvider;
import cn.taketoday.lang.Nullable;

class DataSizeReferenceProvider extends HintReferenceProviderBase {

  private static final String ALL_DATA_UNIT_SUFFIXES = StringUtil.join(DataUnit.values(), unit -> {
    return unit.getSuffix();
  }, "|");

  private static final Pattern PATTERN = Pattern.compile("^([+\\-]?\\d+)(" + ALL_DATA_UNIT_SUFFIXES + ")$");

  DataSizeReferenceProvider() {
  }

  protected PsiReference createReference(PsiElement element, TextRange textRange, ProcessingContext context) {
    return new HintReferenceBase(element, textRange) {

      @Nullable
      protected PsiElement doResolve() {
        if (getResolveMessage() == null) {
          return getElement();
        }
        return null;
      }

      public String getUnresolvedMessagePattern() {
        String str = "Invalid DataSize: " + getResolveMessage();
        return str;
      }

      @Nullable
      private String getResolveMessage() {
        String value = getValue();
        String numberValue = value;
        DataUnit dataUnit = null;
        Matcher matcher = PATTERN.matcher(value);
        if (matcher.matches()) {
          String unit = matcher.group(2);
          dataUnit = DataUnit.fromString(unit);
          numberValue = value.substring(0, value.length() - unit.length());
        }
        try {
          Long decodedValue = Long.decode(numberValue);
          if (dataUnit == DataUnit.BYTES) {
            return null;
          }
          if (dataUnit == null) {
            MetaConfigKey configKey = context.get(InfraHintReferencesProvider.HINT_REFERENCES_CONFIG_KEY);
            PsiAnnotation dataSizeUnitAnno = cn.taketoday.assistant.app.application.config.hints.HintReferenceUtils.findAnnotationOnConfigPropertyField(configKey,
                    InfraClassesConstants.DATA_SIZE_UNIT);
            if (dataSizeUnitAnno != null) {
              dataUnit = JamCommonUtil.getEnumValue(dataSizeUnitAnno.findAttributeValue("value"), DataUnit.class);
            }
            if (dataUnit == null) {
              dataUnit = DataUnit.BYTES;
            }
          }
          try {
            Math.multiplyExact(decodedValue.longValue(), dataUnit.getBytes());
            return null;
          }
          catch (ArithmeticException e) {
            return "overflow [" + dataUnit.getSuffix() + "]";
          }
        }
        catch (NumberFormatException e2) {
          return "cannot convert '" + numberValue + "' to Long";
        }
      }
    };
  }

  private enum DataUnit {
    BYTES("B", 1),
    KILOBYTES("KB", 1024),
    MEGABYTES("MB", 1048576),
    GIGABYTES("GB", 1073741824),
    TERABYTES("TB", 1099511627776L);

    private final String suffix;
    private final long bytes;

    DataUnit(String suffix, long bytes) {
      this.suffix = suffix;
      this.bytes = bytes;
    }

    public String getSuffix() {
      return this.suffix;
    }

    public long getBytes() {
      return this.bytes;
    }

    @Nullable
    public static DataUnit fromString(String value) {
      DataUnit[] values;
      for (DataUnit unit : values()) {
        if (unit.suffix.equals(value)) {
          return unit;
        }
      }
      return null;
    }
  }
}
