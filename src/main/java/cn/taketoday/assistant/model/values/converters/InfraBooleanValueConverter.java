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

package cn.taketoday.assistant.model.values.converters;

import com.intellij.util.ArrayUtil;
import com.intellij.util.xml.converters.values.BooleanValueConverter;

import java.util.Arrays;

public class InfraBooleanValueConverter extends BooleanValueConverter {

  private static final String[] VALUES_TRUE = { "true", "on", "yes", "1" };

  private static final String[] VALUES_FALSE = { "false", "off", "no", "0" };
  private static final String[] SORTED_VALUES = ArrayUtil.mergeArrays(VALUES_TRUE, VALUES_FALSE);

  static {
    Arrays.sort(SORTED_VALUES);
    Arrays.sort(VALUES_TRUE);
  }

  public InfraBooleanValueConverter(boolean allowEmpty) {
    super(allowEmpty);
  }

  public String[] getTrueValues() {
    return VALUES_TRUE;
  }

  public String[] getFalseValues() {
    return VALUES_FALSE;
  }
}
