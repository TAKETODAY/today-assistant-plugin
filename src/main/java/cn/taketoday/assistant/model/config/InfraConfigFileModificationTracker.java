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

package cn.taketoday.assistant.model.config;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter;
import com.intellij.util.SmartList;
import com.intellij.util.messages.MessageBusConnection;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.assistant.app.InfraModelConfigFileContributor;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import kotlin.jvm.internal.Intrinsics;

@Service({ Service.Level.PROJECT })
public final class InfraConfigFileModificationTracker extends SimpleModificationTracker implements Disposable {

  public InfraConfigFileModificationTracker(Project project) {
    MessageBusConnection connection = project.getMessageBus().connect(this);

    connection.subscribe(
            VirtualFileManager.VFS_CHANGES, new BulkVirtualFileListenerAdapter(new MyVirtualFileListener(this, project)));
  }

  @Override
  public void dispose() {

  }

  private static final class MyVirtualFileListener implements VirtualFileListener {
    private final List<FileType> fileTypes;
    private final ProjectFileIndex fileIndex;
    final InfraConfigFileModificationTracker tracker;

    public MyVirtualFileListener(InfraConfigFileModificationTracker tracker, Project project) {
      this.tracker = tracker;
      this.fileIndex = ProjectFileIndex.getInstance(project);
      SmartList<FileType> fileTypes = new SmartList<>();
      for (InfraModelConfigFileContributor it : InfraModelConfigFileContributor.array()) {
        fileTypes.add(it.getFileType());
      }
      this.fileTypes = fileTypes;
    }

    public void fileCreated(VirtualFileEvent event) {
      incModificationCountIfMine(event);
    }

    public void beforeFileDeletion(VirtualFileEvent event) {
      incModificationCountIfMine(event);
    }

    public void fileMoved(VirtualFileMoveEvent event) {
      incModificationCountIfMine(event);
    }

    public void propertyChanged(VirtualFilePropertyEvent event) {
      if (Intrinsics.areEqual(event.getPropertyName(), InfraMetadataConstant.NAME)) {
        incModificationCountIfMine(event);
      }
    }

    private void incModificationCountIfMine(VirtualFileEvent event) {
      VirtualFile file = event.getFile();
      if (this.fileIndex.isInContent(file)) {
        if (file.isDirectory() || this.fileTypes.contains(file.getFileType())) {
          this.tracker.incModificationCount();
        }
      }
    }
  }
}
