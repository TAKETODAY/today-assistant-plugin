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

package cn.taketoday.assistant.core;

import com.intellij.lang.properties.codeInspection.unsorted.AlphaUnsortedPropertiesFileInspectionSuppressor;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.fileTypes.FileTypeRegistry;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/19 18:37
 */
public class TodayStrategiesFileInspectionSuppressor
        implements AlphaUnsortedPropertiesFileInspectionSuppressor {

  @Override
  public boolean suppressInspectionFor(PropertiesFile propertiesFile) {
    return FileTypeRegistry.getInstance().isFileOfType(propertiesFile.getVirtualFile(), TodayStrategiesFileType.FILE_TYPE);
  }
}
