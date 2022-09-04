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
package cn.taketoday.assistant.app.application.metadata.additional;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.icons.AllIcons;
import com.intellij.json.JsonFileType;
import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonElementGenerator;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Processor;
import com.intellij.util.ThrowableRunnable;

import java.io.IOException;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.InfraConfigFileConstants;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public abstract class BaseDefineLocalMetaConfigKeyIntention implements IntentionAction, HighPriorityAction, Iconable {
  @Override
  public Icon getIcon(int flags) {
    return Icons.Today;
  }

  @Override
  public String getText() {
    return InfraAppBundle.message("DefineLocalMetaConfigKeyFix");
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public boolean isAvailable(Project project, Editor editor, PsiFile file) {
    if (!isAvailable(file.getLanguage())) {
      return false;
    }

    if (!InfraUtils.hasFacets(project)
            || !InfraLibraryUtil.hasFrameworkLibrary(project)) {
      return false;
    }

    if (!InfraConfigurationFileService.of().isApplicationConfigurationFile(file)) {
      return false;
    }

    int offset = editor.getCaretModel().getOffset();
    PsiElement element = file.findElementAt(offset);
    return element != null && isAvailable(element);
  }

  protected abstract boolean isAvailable(PsiElement psiElement);

  protected abstract boolean isAvailable(Language language);

  protected void invoke(Project project,
          PsiFile file,
          @Nullable Editor editor,
          PsiElement propertyElement,
          String keyName) {
    Module module = ModuleUtilCore.findModuleForPsiElement(propertyElement);
    if (module == null) {
      return;
    }
    String moduleName = module.getName();

    InfraAdditionalConfigUtils additionalConfigUtils = new InfraAdditionalConfigUtils(module);
    if (!additionalConfigUtils.hasResourceRoots()) {
      ApplicationManager.getApplication()
              .invokeLater(() ->
                              Messages.showWarningDialog(project,
                                      InfraAppBundle.message("infra.properties.no.resources.roots", moduleName),
                                      InfraAppBundle.message("infra.name")),
                      project.getDisposed());
      return;
    }

    Processor<JsonFile> addKeyToExistingProcessor = jsonFile -> {
      addKey(project, jsonFile, keyName);
      return false;
    };
    if (!additionalConfigUtils.processAdditionalMetadataFiles(addKeyToExistingProcessor)) {
      return;
    }

    List<VirtualFile> roots = additionalConfigUtils.getResourceRoots();
    if (editor == null) {
      createMetadataJson(project, roots.get(0), keyName);
    }
    else {
      SimpleListCellRenderer<VirtualFile> renderer = SimpleListCellRenderer.create((label, value, index) -> {
        label.setText(ProjectUtil.calcRelativeToProjectPath(value, project));
        label.setIcon(AllIcons.Modules.ResourcesRoot);
      });

      JBPopupFactory.getInstance()
              .createPopupChooserBuilder(roots)
              .setTitle(InfraAppBundle.message("infra.properties.no.json.metadata.popup", keyName))
              .setAdText(InfraAppBundle.message("infra.properties.no.json.metadata.hint",
                      InfraConfigFileConstants.ADDITIONAL_CONFIGURATION_METADATA_JSON))
              .setRenderer(renderer)
              .setItemChosenCallback(selectedRoot -> {
                if (selectedRoot == null)
                  return;

                createMetadataJson(project, selectedRoot, keyName);

                DaemonCodeAnalyzer.getInstance(project).restart(file);
              })
              .setRequestFocus(true)
              .createPopup()
              .showInBestPositionFor(editor);
    }
  }

  private static void createMetadataJson(Project project, VirtualFile selectedRoot, String keyName) {
    WriteCommandAction.writeCommandAction(project)
            .withName(InfraAppBundle.message("infra.properties.json.metadata.create", keyName))
            .run(() -> {
              try {
                VirtualFile metaInf = VfsUtil.createDirectoryIfMissing(selectedRoot, "META-INF");
                VirtualFile addVf = metaInf.findChild(InfraConfigFileConstants.ADDITIONAL_CONFIGURATION_METADATA_JSON);
                if (addVf == null) {
                  addVf =
                          metaInf.createChildData(project, InfraConfigFileConstants.ADDITIONAL_CONFIGURATION_METADATA_JSON);
                  VfsUtil.saveText(addVf, "{ \"" + InfraMetadataConstant.PROPERTIES + "\": [ ] }");
                }

                PsiFile jsonFile = PsiManager.getInstance(project).findFile(addVf);
                assert jsonFile != null;
                if (jsonFile instanceof JsonFile) { // .json extension can be reassigned to e.g. TextMate
                  addKey(project, (JsonFile) jsonFile, keyName);
                }
                else {
                  String fileName = addVf.getName();
                  ApplicationManager.getApplication()
                          .invokeLater(() ->
                                          Messages.showWarningDialog(project,
                                                  InfraAppBundle.message("infra.properties.file.not.associated.with.json",
                                                          fileName,
                                                          JsonFileType.INSTANCE.getName()),
                                                  InfraAppBundle.message("infra.name")),
                                  project.getDisposed());
                }
              }
              catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  private static void addKey(Project project, JsonFile additionalJson, String keyName) {
    if (!ReadonlyStatusHandler.ensureFilesWritable(project, additionalJson.getVirtualFile())) {
      return;
    }

    JsonElementGenerator generator = new JsonElementGenerator(project);
    JsonArray propertiesArray = findOrCreatePropertiesArray(generator, additionalJson);
    if (propertiesArray == null) {
      Messages.showWarningDialog(project,
              InfraAppBundle.message("infra.properties.invalid.json",
                      additionalJson.getVirtualFile().getPath()),
              InfraAppBundle.message("infra.name"));
      return;
    }

    WriteAction.run((ThrowableRunnable<IncorrectOperationException>) () -> {
      JsonObject value =
              generator.createValue("{\n" +
                      "  \"" + InfraMetadataConstant.NAME + "\": \"" + keyName + "\",\n" +
                      "  \"" + InfraMetadataConstant.TYPE + "\": \"java.lang.String\",\n" +
                      "  \"" + InfraMetadataConstant.DESCRIPTION + "\": \"Description for " + keyName + ".\"" +
                      "}");

      boolean hasValues = !propertiesArray.getValueList().isEmpty();
      if (hasValues) {
        propertiesArray.addBefore(generator.createComma(), propertiesArray.getLastChild());
      }

      JsonObject added = (JsonObject) propertiesArray.addBefore(value, propertiesArray.getLastChild());

      CodeStyleManager.getInstance(project)
              .reformatText(additionalJson, 0, additionalJson.getTextLength());

      added.navigate(true);
    });
  }

  @Nullable
  private static JsonArray findOrCreatePropertiesArray(JsonElementGenerator generator, JsonFile additionalJson) {
    JsonObject rootObject = ObjectUtils.tryCast(additionalJson.getTopLevelValue(), JsonObject.class);
    if (rootObject == null)
      return null;

    JsonProperty propertiesRoot = rootObject.findProperty(InfraMetadataConstant.PROPERTIES);
    if (propertiesRoot == null) {
      return WriteAction.compute((ThrowableComputable<JsonArray, IncorrectOperationException>) () -> {
        JsonProperty propertiesProperty =
                generator.createProperty(InfraMetadataConstant.PROPERTIES, "[]");
        if (!rootObject.getPropertyList().isEmpty()) {
          rootObject.addBefore(generator.createComma(), rootObject.getLastChild());
        }

        JsonProperty propertiesAdded = (JsonProperty) rootObject.addBefore(propertiesProperty, rootObject.getLastChild());
        return (JsonArray) propertiesAdded.getValue();
      });
    }

    return ObjectUtils.tryCast(propertiesRoot.getValue(), JsonArray.class);
  }
}
