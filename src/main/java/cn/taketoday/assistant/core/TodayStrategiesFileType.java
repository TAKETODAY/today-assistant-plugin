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

import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.service.IconService;

/**
 * today-strategies.properties
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/19 15:41
 */
public class TodayStrategiesFileType extends LanguageFileType implements FileTypeIdentifiableByVirtualFile {
  public static final TodayStrategiesFileType FILE_TYPE = new TodayStrategiesFileType();

  static final String STRATEGIES_FILE_NAME = "today-strategies.properties";

  private TodayStrategiesFileType() {
    super(PropertiesLanguage.INSTANCE, true);
  }

  @Override
  public boolean isMyFileType(VirtualFile file) {
    if (!Comparing.equal(STRATEGIES_FILE_NAME, file.getNameSequence())) {
      return false;
    }
    else {
      VirtualFile parent = file.getParent();
      return parent != null && Comparing.equal("META-INF", parent.getNameSequence());
    }
  }

  @Override
  public String getName() {
    return STRATEGIES_FILE_NAME;
  }

  @Override
  public String getDescription() {
    return STRATEGIES_FILE_NAME;
  }

  @Override
  public String getDisplayName() {
    return InfraBundle.message("today.strategies.file.type");
  }

  @Override
  public String getDefaultExtension() {
    return "";
  }

  @Override
  public Icon getIcon() {
    return IconService.of().getFileIcon();
  }

  @Override
  public String getCharset(VirtualFile file, byte[] content) {
    return PropertiesFileType.INSTANCE.getCharset(file, content);
  }
}
