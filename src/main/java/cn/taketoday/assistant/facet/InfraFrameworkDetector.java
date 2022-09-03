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

package cn.taketoday.assistant.facet;

import com.intellij.facet.FacetType;
import com.intellij.framework.FrameworkType;
import com.intellij.framework.detection.DetectedFrameworkDescription;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FrameworkDetectionContext;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.util.indexing.FileContent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class InfraFrameworkDetector extends FacetBasedFrameworkDetector<InfraFacet, InfraFacetConfiguration> {

  public static FrameworkType getSpringFrameworkType() {
    return new InfraFrameworkDetector().getFrameworkType();
  }

  public InfraFrameworkDetector() {
    super("today", 1);
  }

  public List<? extends DetectedFrameworkDescription> detect(Collection<? extends VirtualFile> newFiles, FrameworkDetectionContext context) {
    return Collections.emptyList();
  }

  public FacetType<InfraFacet, InfraFacetConfiguration> getFacetType() {
    return FacetType.findInstance(InfraFacetType.class);
  }

  public FileType getFileType() {
    return XmlFileType.INSTANCE;
  }

  public ElementPattern<FileContent> createSuitableFilePattern() {
    return StandardPatterns.alwaysFalse();
  }
}
