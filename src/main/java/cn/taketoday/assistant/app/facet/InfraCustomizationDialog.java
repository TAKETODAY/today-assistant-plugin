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

package cn.taketoday.assistant.app.facet;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.CheckboxTreeListener;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.xml.config.ConfigFileSearcher;
import com.intellij.xml.config.ConfigFilesTreeBuilder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.app.InfraModelConfigFileContributor;
import cn.taketoday.assistant.app.InfraModelConfigFileNameContributor;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.beans.CustomSetting;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraAppBundle.message;

class InfraCustomizationDialog extends DialogWrapper {
  private JPanel myMainPanel;
  private JPanel myTreePanel;
  private JPanel myTextFieldPanel;
  private JBLabel myKeyLabel;
  private JPanel myConfigurationPanel;
  private TextFieldWithAutoCompletion<InfraConfigNameProposal> mySpringConfigNameTextField;
  private final Project myProject;
  private final InfraFileSet myFileSet;
  private final InfraModelConfigFileNameContributor myFileNameContributor;
  private CheckboxTreeBase myFilesTree;
  private ConfigFilesTreeBuilder myFilesTreeBuilder;
  private final Set<PsiFile> myPsiFiles;
  private boolean foundConfigFilesByName;
  private final List<VirtualFile> myCustomConfigFiles;
  private final Module myModule;

  private void $$$setupUI$$$() {
    JPanel jPanel = new JPanel();
    this.myMainPanel = jPanel;
    jPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1, false, false));
    JPanel jPanel2 = new JPanel();
    this.myConfigurationPanel = jPanel2;
    jPanel2.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1, false, false));
    jPanel2.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithoutIndent");
    jPanel.add(jPanel2, new GridConstraints(0, 0, 1, 1, 0, 3, 7, 7, null, null, null));
    jPanel2.add(new Spacer(), new GridConstraints(2, 0, 1, 2, 0, 2, 1, 6, null, null, null));
    JPanel jPanel3 = new JPanel();
    this.myTreePanel = jPanel3;
    jPanel3.setLayout(new BorderLayout(0, 0));
    jPanel3.setToolTipText(ResourceBundle.getBundle("messages/InfraMvcBundle").getString("infra.customization.dialog.config.tree.tooltip"));
    jPanel2.add(jPanel3, new GridConstraints(1, 0, 1, 2, 0, 3, 7, 3, null, null, null));
    JBLabel jBLabel = new JBLabel();
    this.myKeyLabel = jBLabel;
    jBLabel.setText("");
    jPanel2.add(jBLabel, new GridConstraints(0, 0, 1, 1, 0, 0, 0, 0, null, null, null));
    jPanel2.add(new Spacer(), new GridConstraints(0, 2, 2, 1, 0, 1, 6, 1, null, null, null));
    JPanel jPanel4 = new JPanel();
    this.myTextFieldPanel = jPanel4;
    jPanel4.setLayout(new BorderLayout(0, 0));
    jPanel2.add(jPanel4, new GridConstraints(0, 1, 1, 1, 0, 3, 7, 3, new Dimension(250, -1), null, null));
  }

  public JComponent $$$getRootComponent$$$() {
    return this.myMainPanel;
  }

  InfraCustomizationDialog(Project project, InfraFileSet fileSet, InfraModelConfigFileNameContributor fileNameContributor) {
    super(project);
    $$$setupUI$$$();
    this.myPsiFiles = new HashSet<>();
    this.myCustomConfigFiles = new ArrayList<>();
    this.myProject = project;
    this.myFileSet = fileSet;
    this.myFileNameContributor = fileNameContributor;
    this.myModule = fileSet.getFacet().getModule();
    setTitle(message("infra.customization.dialog.title", this.myFileSet.getName()));
    initUI();
    init();
  }

  protected void doOKAction() {
    CustomSetting.STRING configFileNameSetting = getConfigFileNameSetting();
    String configFileName = getConfigFileName();
    if (StringUtil.isEmpty(configFileName)) {
      configFileNameSetting.clear();
    }
    else {
      configFileNameSetting.setStringValue(configFileName);
    }
    CustomSetting.STRING customConfigFilesSetting = getCustomConfigFilesSetting();
    if (!this.myCustomConfigFiles.isEmpty()) {
      customConfigFilesSetting.setStringValue(StringUtil.join(this.myCustomConfigFiles, VirtualFile::getUrl, ";"));
    }
    else {
      customConfigFilesSetting.clear();
    }
    super.doOKAction();
  }

  private CustomSetting.STRING getConfigFileNameSetting() {
    return getCustomSetting(this.myFileNameContributor.getCustomNameSettingDescriptor().key);
  }

  private CustomSetting.STRING getCustomConfigFilesSetting() {
    return getCustomSetting(this.myFileNameContributor.getCustomFilesSettingDescriptor().key);
  }

  private CustomSetting.STRING getCustomSetting(Key<CustomSetting.STRING> id) {
    return this.myFileSet.getFacet().findSetting(id);
  }

  protected boolean postponeValidation() {
    return false;
  }

  private void initUI() {
    InfraModelConfigFileNameContributor.CustomizationPresentation presentation = this.myFileNameContributor.getCustomizationPresentation();
    this.myConfigurationPanel.setBorder(IdeBorderFactory.createTitledBorder(presentation.customizationPanelTitle, false));
    this.myKeyLabel.setText(String.format("<html><code>%s</code>:</html>", presentation.configFileKey));
    this.mySpringConfigNameTextField = new TextFieldWithAutoCompletion<>(this.myProject, new InfraConfigNameAutoCompletionListProvider(getSpringConfigNameProposals()), true, null);
    this.myTextFieldPanel.add(this.mySpringConfigNameTextField);
    CustomSetting.STRING configFileNameSetting = getConfigFileNameSetting();
    String defaultValue = configFileNameSetting.getDefaultValue();
    if (!Objects.equals(configFileNameSetting.getStringValue(), defaultValue)) {
      this.mySpringConfigNameTextField.setText(configFileNameSetting.getStringValue());
    }
    this.mySpringConfigNameTextField.setPlaceholder(message("infra.customization.dialog.config.not.set", defaultValue));
    this.mySpringConfigNameTextField.addDocumentListener(new DocumentListener() {

      public void documentChanged(DocumentEvent event) {
        InfraCustomizationDialog.this.updateTree();
      }
    });
    CustomSetting.STRING customConfigFilesSetting = getCustomConfigFilesSetting();
    String urls = customConfigFilesSetting.getStringValue();
    if (urls != null) {
      for (String url : StringUtil.split(urls, ";")) {
        VirtualFile customConfigFile = VirtualFileManager.getInstance().findFileByUrl(url);
        ContainerUtil.addIfNotNull(this.myCustomConfigFiles, customConfigFile);
      }
    }
    this.myFilesTree = new CheckboxTreeBase(new CheckboxTreeBase.CheckboxTreeCellRendererBase() {
      public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        ConfigFilesTreeBuilder.renderNode(value, expanded, getTextRenderer());
        if (!(value instanceof DefaultMutableTreeNode)) {
          return;
        }
        Object object = ((DefaultMutableTreeNode) value).getUserObject();
        if (object instanceof PsiFile) {
          getTextRenderer().setIcon(InfraCustomizationDialog.this.myFileNameContributor.getFileIcon());
        }
        else if (object instanceof FileType) {
          getTextRenderer().setIcon(new LayeredIcon(AllIcons.Nodes.Folder, Icons.TodayOverlay));
        }
      }
    }, null, new CheckboxTreeBase.CheckPolicy(false, false, true, true)) {
      protected boolean shouldShowBusyIconIfNeeded() {
        return true;
      }
    };
    ConfigFilesTreeBuilder.installSearch(this.myFilesTree);
    this.myFilesTreeBuilder = new ConfigFilesTreeBuilder(this.myFilesTree) {
      protected DefaultMutableTreeNode createFileNode(Object file) {
        CheckedTreeNode node = new CheckedTreeNode(file);
        if (file instanceof PsiFile) {
          node.setChecked(true);
          node.setEnabled(InfraCustomizationDialog.this.myCustomConfigFiles.contains(((PsiFile) file).getVirtualFile()));
        }
        else {
          node.setChecked(false);
          node.setEnabled(false);
        }
        return node;
      }
    };
    this.myFilesTree.addCheckboxTreeListener(new CheckboxTreeListener() {

      public void nodeStateChanged(CheckedTreeNode node) {
        Object userObject = node.getUserObject();
        if (userObject instanceof PsiFile) {
          VirtualFile configFile = ((PsiFile) userObject).getVirtualFile();
          if (node.isChecked()) {
            InfraCustomizationDialog.this.myCustomConfigFiles.add(configFile);
          }
          else {
            InfraCustomizationDialog.this.myCustomConfigFiles.remove(configFile);
          }
        }
      }
    });
    this.myFilesTree.setRootVisible(false);
    this.myFilesTree.setShowsRootHandles(true);
    updateTree();
    JPanel panel = ToolbarDecorator.createDecorator(this.myFilesTree).setAddAction(button -> {
      addCustomFiles();
    }).setAddActionName(message("infra.customization.add.config.files.action")).createPanel();
    this.myTreePanel.add(panel);
  }

  private List<InfraConfigNameProposal> getSpringConfigNameProposals() {
    SmartList<String> smartList = new SmartList<>();
    for (InfraModelConfigFileNameContributor contributor : InfraModelConfigFileNameContributor.EP_NAME.getExtensions()) {
      if (contributor.accept(this.myModule)) {
        smartList.add(contributor.getCustomNameSettingDescriptor().defaultValue);
      }
    }
    List<InfraConfigNameProposal> results = new ArrayList<>();
    List<VirtualFile> directories = InfraModelConfigFileContributor.getConfigFileDirectories(this.myModule, false);
    if (directories.isEmpty()) {
      return results;
    }
    GlobalSearchScope searchScope = GlobalSearchScopesCore.directoriesScope(this.myModule.getProject(), false, directories.toArray(VirtualFile.EMPTY_ARRAY));
    for (InfraModelConfigFileContributor contributor2 : InfraModelConfigFileContributor.EP_NAME.getExtensions()) {
      FileType fileType = contributor2.getFileType();
      Collection<VirtualFile> allFiles = FileTypeIndex.getFiles(fileType, searchScope);
      for (VirtualFile file : allFiles) {
        String nameWithoutExtension = file.getNameWithoutExtension();
        if (!StringUtil.containsChar(nameWithoutExtension, '-') && !smartList.contains(nameWithoutExtension)) {
          Pair<List<VirtualFile>, List<VirtualFile>> configFiles = contributor2.findApplicationConfigFiles(this.myModule, false, nameWithoutExtension);
          results.add(new InfraConfigNameProposal(fileType, nameWithoutExtension, ContainerUtil.concat(configFiles.first, configFiles.second)));
        }
      }
    }
    results.sort((o1, o2) -> StringUtil.naturalCompare(o1.myName, o2.myName));
    return results;
  }

  private void addCustomFiles() {
    Set<FileType> configFileFiletypes = ContainerUtil.map2Set(InfraModelConfigFileContributor.EP_NAME.getExtensions(), InfraModelConfigFileContributor::getFileType);
    List<VirtualFile> configFiles = InfraConfigurationFileService.of().findConfigFiles(this.myModule, false, contributor2 -> {
      return !contributor2.equals(this.myFileNameContributor) && contributor2.accept(this.myModule);
    });
    Set<VirtualFile> configFilesFromDifferentContributors = new HashSet<>(configFiles);
    configFilesFromDifferentContributors.addAll(InfraConfigurationFileService.of().collectImports(this.myModule, configFiles));
    FileChooserDescriptor chooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, true) {
      public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
        if (!super.isFileVisible(file, showHiddenFiles)) {
          return false;
        }
        return file.isDirectory()
               ? !ProjectFileIndex.getInstance(myProject).isExcluded(file)
               : configFileFiletypes.contains(file.getFileType())
                       && !configFilesFromDifferentContributors.contains(file)
                       && !ContainerUtil.exists(myPsiFiles, psiFile -> {
                 return psiFile.getVirtualFile().equals(file);
               });
      }
    }.withShowHiddenFiles(false).withTitle(message("infra.config.chooser.dialog.title"))
            .withRoots(ModuleRootManager.getInstance(this.myModule).getContentRoots());
    FileChooserDialog fileChooser = FileChooserFactory.getInstance().createFileChooser(chooserDescriptor, this.myProject, this.myMainPanel);
    VirtualFile[] files = fileChooser.choose(this.myProject);
    if (files.length != 0) {
      ContainerUtil.addAll(this.myCustomConfigFiles, files);
      updateTree();
    }
  }

  private void updateTree() {
    CheckedTreeNode myRoot = new CheckedTreeNode(null);
    this.myFilesTree.setModel(new DefaultTreeModel(myRoot));
    this.myPsiFiles.clear();
    this.myFilesTree.setPaintBusy(true);
    try {
      ConfigFileSearcher[] searchers = getSearchers();
      for (ConfigFileSearcher searcher : searchers) {
        this.myPsiFiles.addAll(searcher.searchWithFiles());
      }
      this.myFilesTreeBuilder.buildTree((DefaultMutableTreeNode) this.myFilesTree.getModel().getRoot(), searchers);
      TreeUtil.expandAll(this.myFilesTree);
      this.myFilesTree.setPaintBusy(false);
    }
    catch (Throwable th) {
      this.myFilesTree.setPaintBusy(false);
      throw th;
    }
  }

  private ConfigFileSearcher[] getSearchers() {
    InfraModelConfigFileContributor[] infraModelConfigFileContributorArr;
    PsiManager psiManager = PsiManager.getInstance(this.myProject);
    String defaultConfigName = getConfigFileNameSetting().getDefaultValue();
    this.foundConfigFilesByName = false;
    SmartList<ConfigFileSearcher> smartList = new SmartList<>();
    for (InfraModelConfigFileContributor contributor : InfraModelConfigFileContributor.EP_NAME.getExtensions()) {
      ConfigFileSearcher searcher = new ConfigFileSearcher(this.myModule, this.myModule.getProject()) {

        public Set<PsiFile> search(@Nullable Module module, Project project) {
          List<VirtualFile> allConfigFiles = new SmartList<>();
          String configName = StringUtil.defaultIfEmpty(InfraCustomizationDialog.this.getConfigFileName(), defaultConfigName);
          Pair<List<VirtualFile>, List<VirtualFile>> configFiles = contributor.findApplicationConfigFiles(module, false, configName);
          if (!configFiles.first.isEmpty() || !configFiles.second.isEmpty()) {
            InfraCustomizationDialog.this.foundConfigFilesByName = true;
          }
          allConfigFiles.addAll(configFiles.first);
          allConfigFiles.addAll(configFiles.second);
          allConfigFiles.addAll(InfraConfigurationFileService.of().collectImports(module, allConfigFiles));
          return new LinkedHashSet<>(PsiUtilCore.toPsiFiles(psiManager, allConfigFiles));
        }
      };
      smartList.add(searcher);
    }
    smartList.add(new ConfigFileSearcher(this.myModule, this.myModule.getProject()) {

      public Set<PsiFile> search(@Nullable Module module, Project project) {
        List<VirtualFile> customConfigFiles =
                module == null
                ? myCustomConfigFiles
                : ContainerUtil.concat(myCustomConfigFiles,
                        InfraConfigurationFileService.of().collectImports(module, myCustomConfigFiles));
        return new LinkedHashSet<>(PsiUtilCore.toPsiFiles(psiManager, customConfigFiles));
      }
    });
    return smartList.toArray(new ConfigFileSearcher[0]);
  }

  private String getConfigFileName() {
    return this.mySpringConfigNameTextField.getText();
  }

  @Nullable
  protected JComponent createCenterPanel() {
    return this.myMainPanel;
  }

  @Nullable
  protected ValidationInfo doValidate() {
    if (!this.foundConfigFilesByName) {
      return new ValidationInfo(
              message("infra.customization.dialog.no.configs", StringUtil.defaultIfEmpty(getConfigFileName(), getConfigFileNameSetting().getDefaultValue())),
              this.mySpringConfigNameTextField).asWarning().withOKEnabled();
    }
    return null;
  }

  @Nullable
  protected String getDimensionServiceKey() {
    return "InfraCustomization";
  }

  private static final class InfraConfigNameProposal {
    private final FileType myFileType;
    private final String myName;
    private final List<VirtualFile> myConfigFilesByName;

    private InfraConfigNameProposal(FileType fileType, String name, List<VirtualFile> configFilesByName) {
      this.myFileType = fileType;
      this.myName = name;
      this.myConfigFilesByName = configFilesByName;
    }
  }

  private static final class InfraConfigNameAutoCompletionListProvider extends TextFieldWithAutoCompletionListProvider<InfraConfigNameProposal> {

    private InfraConfigNameAutoCompletionListProvider(List<InfraConfigNameProposal> variants) {
      super(variants);
    }

    public String getLookupString(InfraConfigNameProposal item) {
      return item.myName;
    }

    public String getTypeText(InfraConfigNameProposal item) {
      return "(." + item.myFileType.getDefaultExtension() + ")";
    }

    public String getAdvertisement() {
      return message("infra.config.files.add.ad");
    }

    public String getTailText(InfraConfigNameProposal item) {
      List<VirtualFile> configFiles = item.myConfigFilesByName;
      StringBuilder tailText = new StringBuilder(" ");
      if (configFiles.size() > 1) {
        tailText.append("[Detected Profiles: ");
        SmartList<String> smartList = new SmartList<>();
        for (VirtualFile file : configFiles) {
          String nameWithoutExtension = file.getNameWithoutExtension();
          if (StringUtil.containsChar(nameWithoutExtension, '-')) {
            smartList.add(StringUtil.substringAfter(nameWithoutExtension, "-"));
          }
        }
        tailText.append(StringUtil.join(smartList, ", "));
        tailText.append("] ");
      }
      return tailText.toString();
    }

    @Nullable
    public Icon getIcon(InfraConfigNameProposal item) {
      return item.myFileType.getIcon();
    }
  }
}
