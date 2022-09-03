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

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.impl.ui.FacetEditorsFactoryImpl;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.facet.ui.libraries.FrameworkLibraryValidator;
import com.intellij.framework.library.DownloadableLibraryService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;

import org.jdom.Element;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.context.model.LocalXmlModel;
import cn.taketoday.assistant.facet.beans.CustomSetting;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/22 18:06
 */
public class InfraFacetConfiguration
        extends SimpleModificationTracker implements FacetConfiguration, ModificationTracker, Disposable {

  private static final String FILESET = "fileset";

  private static final String SET_ID = "id";

  private static final String ACTIVE_PROFILES = "profiles";

  private static final String SET_NAME = "name";

  private static final String SET_REMOVED = "removed";

  private static final String FILE = "file";

  private static final String DEPENDENCY = "dependency";

  private static final String AUTODETECTED = "autodetected";
  private final Set<InfraFileSetData> myFileSets = new LinkedHashSet<>();

  private final NotNullLazyValue<Set<CustomSetting>> myCustomSettings = NotNullLazyValue.atomicLazy(() -> {
    Set<CustomSetting> myCustomSettings = new LinkedHashSet<>();
    myCustomSettings.add(new CustomSetting.BOOLEAN(LocalXmlModel.PROCESS_EXPLICITLY_ANNOTATED, InfraBundle.message("process.explicitly.annotated.beans"), true));
    for (InfraFileSetEditorCustomization customization : InfraFileSetEditorCustomization.array()) {
      myCustomSettings.addAll(customization.getCustomSettings());
    }
    return myCustomSettings;
  });

  private final SortedMap<String, String> myAutodetectedFileSetProfiles = new TreeMap();
  private final SimpleModificationTracker mySettingsModificationTracker = new SimpleModificationTracker();

  public Set<InfraFileSetData> getFileSetDescriptors() {
    return Collections.unmodifiableSet(this.myFileSets);
  }

  public boolean addFileSetData(InfraFileSetData fileSet) {
    incSettingsModificationCount();
    return this.myFileSets.add(fileSet);
  }

  @Override
  public FacetEditorTab[] createEditorTabs(FacetEditorContext editorContext, FacetValidatorsManager validatorsManager) {
    FrameworkLibraryValidator validator = FacetEditorsFactoryImpl.getInstanceImpl()
            .createLibraryValidator(
                    DownloadableLibraryService.getInstance()
                            .createDescriptionForType(InfraLibraryType.class), editorContext, validatorsManager, "today"
            );
    validatorsManager.registerValidator(validator);
    return new FacetEditorTab[] {
            new InfraConfigurationTab(editorContext, validatorsManager)
    };
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    for (Element setElement : element.getChildren(FILESET)) {
      String auto = setElement.getAttributeValue(AUTODETECTED);
      if (Boolean.parseBoolean(auto)) {
        String setId = setElement.getAttributeValue(SET_ID);
        String activeProfiles = setElement.getAttributeValue("profiles");
        if (setId != null && activeProfiles != null) {
          synchronized(this.myAutodetectedFileSetProfiles) {
            this.myAutodetectedFileSetProfiles.put(setId, activeProfiles);
          }
        }
      }
      else {
        String setName = setElement.getAttributeValue(SET_NAME);
        String setId2 = setElement.getAttributeValue(SET_ID);
        String removed = setElement.getAttributeValue(SET_REMOVED);
        if (setName != null && setId2 != null) {
          InfraFileSetData fileSet = InfraFileSetData.create(setId2, setName);
          List<Element> deps = setElement.getChildren(DEPENDENCY);
          for (Element dep : deps) {
            fileSet.addDependency(dep.getText());
          }
          List<Element> files = setElement.getChildren(FILE);
          for (Element fileElement : files) {
            String text = fileElement.getText();
            fileSet.addFile(text);
          }
          fileSet.setRemoved(Boolean.parseBoolean(removed));
          addFileSetData(fileSet);
          String activeProfiles2 = setElement.getAttributeValue("profiles");
          if (StringUtil.isNotEmpty(activeProfiles2)) {
            fileSet.setActiveProfiles(new LinkedHashSet<>(StringUtil.split(activeProfiles2, ",")));
          }
        }
      }
    }
    for (CustomSetting settingBean : getCustomSettings()) {
      String name = settingBean.getName();
      String s = element.getAttributeValue(name);
      if (s != null) {
        settingBean.setStringValue(s);
        settingBean.apply();
      }
    }
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    for (InfraFileSetData fileSet : this.myFileSets) {
      Element setElement = new Element(FILESET);
      setElement.setAttribute(SET_ID, fileSet.getId());
      setElement.setAttribute(SET_NAME, fileSet.getName());
      setElement.setAttribute(SET_REMOVED, Boolean.toString(fileSet.isRemoved()));
      element.addContent(setElement);
      for (String dep : fileSet.getDependencies()) {
        Element depElement = new Element(DEPENDENCY);
        depElement.setText(dep);
        setElement.addContent(depElement);
      }
      for (String url : fileSet.getFiles()) {
        Element fileElement = new Element(FILE);
        fileElement.setText(url);
        setElement.addContent(fileElement);
      }
      Set<String> activeProfiles = fileSet.getActiveProfiles();
      if (activeProfiles.size() > 0) {
        setElement.setAttribute("profiles", StringUtil.join(activeProfiles, ","));
      }
    }
    for (CustomSetting customSetting : getCustomSettings()) {
      if (customSetting.getStringValue() != null) {
        element.setAttribute(customSetting.getName(), customSetting.getStringValue());
      }
    }
    synchronized(this.myAutodetectedFileSetProfiles) {
      for (Map.Entry<String, String> entry : this.myAutodetectedFileSetProfiles.entrySet()) {
        if (StringUtil.isNotEmpty(entry.getValue())) {
          Element setElement2 = new Element(FILESET);
          setElement2.setAttribute(SET_ID, entry.getKey());
          setElement2.setAttribute("profiles", entry.getValue());
          setElement2.setAttribute(AUTODETECTED, "true");
          element.addContent(setElement2);
        }
      }
    }
  }

  public void setModified() {
    incSettingsModificationCount();
  }

  @Override
  public void dispose() {
  }

  public synchronized void registerAutodetectedFileSet(InfraFileSet fileset) {
    String id = fileset.getId();
    if (!this.myAutodetectedFileSetProfiles.containsKey(id)) {
      this.myAutodetectedFileSetProfiles.put(id, "");
    }
    fileset.setActiveProfiles(getActiveProfiles(id));
    incModificationCount();
  }

  public synchronized void setActiveProfilesForAutodetected(String filesetId, Set<String> profiles) {
    this.myAutodetectedFileSetProfiles.put(filesetId, StringUtil.join(profiles, ","));
  }

  private synchronized Set<String> getActiveProfiles(String filesetId) {
    String profiles = myAutodetectedFileSetProfiles.get(filesetId);
    return StringUtil.isEmptyOrSpaces(profiles) ? new LinkedHashSet<>() : new LinkedHashSet<>(StringUtil.split(profiles, ","));
  }

  public void removeFileSetDescriptors() {
    this.myFileSets.clear();
  }

  public Set<CustomSetting> getCustomSettings() {
    return this.myCustomSettings.getValue();
  }

  private void incSettingsModificationCount() {
    incModificationCount();
    this.mySettingsModificationTracker.incModificationCount();
  }

  public ModificationTracker getSettingsModificationTracker() {
    return this.mySettingsModificationTracker;
  }
}
