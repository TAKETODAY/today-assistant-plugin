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

package cn.taketoday.assistant.model.structure;

import com.intellij.ide.structureView.StructureView;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.ide.structureView.newStructureView.StructureViewComponent;
import com.intellij.ide.structureView.xml.XmlStructureViewBuilderProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.xml.XmlFile;

import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.lang.Nullable;

public class InfraXmlStructureViewBuilderProvider implements XmlStructureViewBuilderProvider {

  @Nullable
  public StructureViewBuilder createStructureViewBuilder(XmlFile file) {
    if (InfraDomUtils.isInfraXml(file)) {
      return new TreeBasedStructureViewBuilder() {

        public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
          return new InfraStructureViewModel(file, editor);
        }

        public StructureView createStructureView(FileEditor fileEditor, Project project) {
          StructureViewModel model = createStructureViewModel(fileEditor instanceof TextEditor ? ((TextEditor) fileEditor).getEditor() : null);
          StructureViewComponent structureViewComponent = new StructureViewComponent(fileEditor, model, project, false);
          Disposer.register(structureViewComponent, model::dispose);
          return structureViewComponent;
        }
      };
    }
    return null;
  }
}
