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

package cn.taketoday.assistant.app.spi;

import com.intellij.lang.spi.SPILanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Objects;

import javax.swing.Icon;

import cn.taketoday.assistant.service.IconService;

import static cn.taketoday.assistant.InfraAppBundle.message;

public final class InfraImportsFileType extends LanguageFileType implements FileTypeIdentifiableByVirtualFile {
  public static final String IMPORTS_FILE_EXTENSION = "imports";
  private static final String FILE_TYPE_NAME = "Infrastructure imports";
  public static final InfraImportsFileType FILE_TYPE = new InfraImportsFileType();

  private InfraImportsFileType() {
    super(SPILanguage.INSTANCE, true);
  }

  public boolean isMyFileType(VirtualFile file) {
    VirtualFile parent;
    VirtualFile parent2;
    return Objects.equals(IMPORTS_FILE_EXTENSION, file.getExtension())
            && (parent = file.getParent()) != null
            && "config".contentEquals(parent.getNameSequence())
            && (parent2 = parent.getParent()) != null
            && "META-INF".contentEquals(parent2.getNameSequence());
  }

  public String getName() {
    return FILE_TYPE_NAME;
  }

  public String getDescription() {
    return message("infra.imports.file.type.description");
  }

  public String getDisplayName() {
    return message("infra.imports.file.type.name");
  }

  public String getDefaultExtension() {
    return IMPORTS_FILE_EXTENSION;
  }

  public Icon getIcon() {
    IconService springSpiIconService = IconService.of();
    return springSpiIconService.getFileIcon();
  }

  public String getCharset(VirtualFile file, byte[] content) {
    return "UTF-8";
  }

}
