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

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import cn.taketoday.assistant.facet.beans.CustomSetting;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/25 12:17
 */
public abstract class InfraFileSetEditorCustomization {

  public static final ExtensionPointName<InfraFileSetEditorCustomization> EP_NAME =
          ExtensionPointName.create("cn.taketoday.assistant.fileSetEditorCustomization");

  public static final Key<InfraFileSet> EXTRA_ACTION_FILESET = Key.create("currentFileSet");

  /**
   * Determines whether this customization applies to the given fileset.
   * <p/>
   * NB: {@code instanceof} cannot be used here, use fileset ID and/or files to determine applicability.
   *
   * @param fileSet Fileset to be processed.
   * @return {@code true} if customization should be used.
   */
  public abstract boolean isApplicable(InfraFileSet fileSet);

  /**
   * Returns custom groups of configuration files.
   * <p/>
   * All files contained in these groups will be removed from any of the default groups.
   *
   * @param fileSet Fileset to be processed.
   * @return List of custom config file groups.
   */
  public List<InfraFileSetEditorCustomization.CustomConfigFileGroup> getCustomConfigFileGroups(InfraFileSet fileSet) {
    return Collections.emptyList();
  }

  /**
   * Returns new instances of all custom settings.
   * <p/>
   * Will be enabled in facet editor if at least one fileset matches {@link #isApplicable(InfraFileSet)}.
   *
   * @return Custom facet settings.
   */
  public List<CustomSetting> getCustomSettings() {
    return Collections.emptyList();
  }

  /**
   * Returns custom actions for further customization of current <em>autodetected</em> fileset and/or setting framework specific options.
   * <p/>
   * The currently selected fileset can be obtained via {@link DataManager#loadFromDataContext(com.intellij.openapi.actionSystem.DataContext, Key)}
   * using key {@link #EXTRA_ACTION_FILESET}.
   *
   * @return Custom actions.
   */
  public AnAction[] getExtraActions() {
    return AnAction.EMPTY_ARRAY;
  }

  public static InfraFileSetEditorCustomization[] array() {
    return EP_NAME.getExtensions();
  }

  public static class CustomConfigFileGroup {

    private final String myName;
    private final Icon myIcon;
    private final Set<VirtualFilePointer> myFiles;

    public CustomConfigFileGroup(String name, Icon icon, Set<VirtualFilePointer> files) {
      myName = name;
      myIcon = icon;
      myFiles = files;
    }

    public String getName() {
      return myName;
    }

    public Icon getIcon() {
      return myIcon;
    }

    public Set<VirtualFilePointer> getFiles() {
      return myFiles;
    }
  }
}
