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

package cn.taketoday.assistant.facet.editor;

import com.intellij.DynamicBundle;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.TreeExpander;
import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeModelAdapter;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.xml.config.ConfigFileSearcher;
import com.intellij.xml.config.ConfigFilesTreeBuilder;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFileSetImpl;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.lang.Nullable;

public class FileSetEditor extends DialogWrapper {
  private JPanel myMainPanel;
  private JTextField mySetName;
  private JBLabel myHelpLabel;
  private JComboBox myParentBox;
  private InfraFilesTree myFilesTree;
  private JPanel myTreePanel;
  private final InfraFileSet myOriginalSet;
  private final InfraFileSet myFileSet;
  private ConfigFilesTreeBuilder myBuilder;
  private final Module myModule;
  private Set<String> myExistingContextNames;
  private TreeExpander myTreeExpander;

  private void $$$setupUI$$$() {
    JPanel jPanel = new JPanel();
    this.myMainPanel = jPanel;
    jPanel.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1, false, false));
    jPanel.setMinimumSize(new Dimension(600, 200));
    JLabel jLabel = new JLabel();
    $$$loadLabelText$$$(jLabel, DynamicBundle.getBundle("messages/InfraBundle", FileSetEditor.class).getString("facet.context.edit.name.label"));
    jPanel.add(jLabel, new GridConstraints(0, 0, 1, 1, 4, 0, 0, 0, null, null, null));
    JTextField jTextField = new JTextField();
    this.mySetName = jTextField;
    jPanel.add(jTextField, new GridConstraints(0, 1, 1, 1, 0, 1, 3, 0, null, new Dimension(150, -1), null));
    JComboBox jComboBox = new JComboBox();
    this.myParentBox = jComboBox;
    jPanel.add(jComboBox, new GridConstraints(1, 1, 1, 1, 8, 1, 2, 0, null, new Dimension(490, 22), null));
    JLabel jLabel2 = new JLabel();
    $$$loadLabelText$$$(jLabel2, DynamicBundle.getBundle("messages/InfraBundle", FileSetEditor.class).getString("facet.context.edit.parent.label"));
    jPanel.add(jLabel2, new GridConstraints(1, 0, 1, 1, 4, 0, 0, 0, null, null, null));
    JBLabel jBLabel = new JBLabel();
    this.myHelpLabel = jBLabel;
    jBLabel.setComponentStyle(UIUtil.ComponentStyle.SMALL);
    jBLabel.setFontColor(UIUtil.FontColor.BRIGHTER);
    jBLabel.setHorizontalTextPosition(11);
    jPanel.add(jBLabel, new GridConstraints(2, 0, 1, 2, 8, 0, 3, 0, null, null, null));
    JPanel jPanel2 = new JPanel();
    this.myTreePanel = jPanel2;
    jPanel2.setLayout(new BorderLayout(0, 0));
    jPanel.add(jPanel2, new GridConstraints(3, 0, 1, 2, 0, 3, 3, 3, null, null, null));
    jLabel.setLabelFor(jTextField);
    jLabel2.setLabelFor(jComboBox);
  }

  public JComponent $$$getRootComponent$$$() {
    return this.myMainPanel;
  }

  private void $$$loadLabelText$$$(JLabel jLabel, String str) {
    StringBuilder stringBuffer = new StringBuilder();
    boolean z = false;
    char c = 0;
    int i = -1;
    int i2 = 0;
    while (i2 < str.length()) {
      if (str.charAt(i2) == '&') {
        i2++;
        if (i2 == str.length()) {
          break;
        }
        else if (!z && str.charAt(i2) != '&') {
          z = true;
          c = str.charAt(i2);
          i = stringBuffer.length();
        }
      }
      stringBuffer.append(str.charAt(i2));
      i2++;
    }
    jLabel.setText(stringBuffer.toString());
    if (z) {
      jLabel.setDisplayedMnemonic(c);
      jLabel.setDisplayedMnemonicIndex(i);
    }
  }

  public FileSetEditor(Module module, InfraFileSet fileSet, Set<InfraFileSet> currentModuleParentCandidates, ConfigFileSearcher... searchers) {
    super(module.getProject(), true);
    this.myModule = module;
    this.myOriginalSet = fileSet;
    InfraFileSet springFileSet = this.myOriginalSet;
    $$$setupUI$$$();
    this.myFileSet = new InfraFileSetImpl(springFileSet);
    init(fileSet, module, currentModuleParentCandidates, searchers);
  }

  public FileSetEditor(Component parent, Module module, InfraFileSet fileSet, Set<InfraFileSet> currentModuleParentCandidates, ConfigFileSearcher... searchers) {
    super(parent, true);
    this.myModule = module;
    this.myOriginalSet = fileSet;
    InfraFileSet springFileSet = this.myOriginalSet;
    $$$setupUI$$$();
    this.myFileSet = new InfraFileSetImpl(springFileSet);
    init(fileSet, module, currentModuleParentCandidates, searchers);
  }

  private void init(InfraFileSet fileSet, Module module, Set<InfraFileSet> currentModuleParentCandidates, ConfigFileSearcher... searchers) {
    setTitle(this.myOriginalSet.isNew() ? InfraBundle.message("facet.context.new.title") : InfraBundle.message("facet.context.edit.title", fileSet.getName()));
    this.mySetName.setText(fileSet.getName());
    this.mySetName.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        FileSetEditor.this.updateFileSet();
      }
    });
    this.myHelpLabel.setText(InfraBundle.message("fileset.editor.help.label"));
    initParentSelection(fileSet, currentModuleParentCandidates);
    initTree(fileSet, module.getProject(), searchers);
    this.myExistingContextNames = new HashSet();
    Set<InfraFileSet> allSets = InfraFileSetService.of().getAllSets(fileSet.getFacet());
    allSets.remove(fileSet);
    for (InfraFileSet set : allSets) {
      this.myExistingContextNames.add(set.getName());
    }
    init();
    setOKActionEnabled(this.myOriginalSet.isNew());
  }

  private void initParentSelection(InfraFileSet fileSet, Set<InfraFileSet> currentModuleParentCandidates) {
    this.myParentBox.setRenderer(new ParentContextListCellRenderer());
    this.myParentBox.addItem(null);
    List<InfraFileSet> parentFileSetCandidates = getParentFileSetCandidates(fileSet, currentModuleParentCandidates);
    if (parentFileSetCandidates.isEmpty()) {
      this.myParentBox.setEnabled(false);
      return;
    }
    Set<InfraFileSet> ourDependencies = fileSet.getDependencyFileSets();
    Object selectedParentFileSet = null;
    for (InfraFileSet parentCandidate : parentFileSetCandidates) {
      if (!parentCandidate.getDependencyFileSets().contains(fileSet)) {
        this.myParentBox.addItem(parentCandidate);
        if (ourDependencies.contains(parentCandidate)) {
          selectedParentFileSet = parentCandidate;
        }
      }
    }
    this.myParentBox.setSelectedItem(selectedParentFileSet);
    this.myParentBox.addItemListener(e -> {
      updateFileSet();
    });
    new ComboboxSpeedSearch(this.myParentBox) {
      protected String getElementText(Object element) {
        if (element == null) {
          return InfraBundle.message("facet.context.edit.parent.none.selected");
        }
        return ((InfraFileSet) element).getName();
      }
    };
  }

  private void initTree(InfraFileSet fileSet, Project project, ConfigFileSearcher[] searchers) {
    PsiFile psiFile;
    CheckedTreeNode myRoot = new CheckedTreeNode(null);
    this.myFilesTree = new InfraFilesTree();
    this.myFilesTree.setModel(new DefaultTreeModel(myRoot));
    for (ConfigFileSearcher searcher : searchers) {
      searcher.search();
    }
    this.myBuilder = new ConfigFilesTreeBuilder(this.myFilesTree) {
      protected DefaultMutableTreeNode createFileNode(Object file) {
        CheckedTreeNode node = new CheckedTreeNode(file);
        node.setChecked(
                ((file instanceof PsiFile) && FileSetEditor.this.myFileSet.hasFile(((PsiFile) file).getVirtualFile())) || ((file instanceof VirtualFile) && FileSetEditor.this.myFileSet.hasFile(
                        (VirtualFile) file)));
        return node;
      }
    };
    Set<PsiFile> psiFiles = this.myBuilder.buildTree(myRoot, searchers);
    PsiManager psiManager = PsiManager.getInstance(project);
    List<VirtualFilePointer> list = fileSet.getFiles();
    for (VirtualFilePointer pointer : list) {
      VirtualFile file = pointer.getFile();
      if (file != null && file.isValid() && ((psiFile = psiManager.findFile(file)) == null || !psiFiles.contains(psiFile))) {
        this.myBuilder.addFile(file);
      }
    }
    this.myFilesTree.getModel().addTreeModelListener(new TreeModelAdapter() {
      public void treeNodesChanged(TreeModelEvent e) {
        FileSetEditor.this.updateFileSet();
      }
    });
    this.myTreeExpander = new DefaultTreeExpander(this.myFilesTree);
    this.myTreeExpander.expandAll();
    initTreeToolbar();
  }

  private void initTreeToolbar() {
    CommonActionsManager actionManager = CommonActionsManager.getInstance();
    this.myTreePanel.add(ToolbarDecorator.createDecorator(this.myFilesTree).setAddAction(this::doAddAction)
            .addExtraAction(AnActionButton.fromAction(actionManager.createExpandAllAction(this.myTreeExpander, this.myFilesTree)))
            .addExtraAction(AnActionButton.fromAction(actionManager.createCollapseAllAction(this.myTreeExpander, this.myFilesTree))).disableUpDownActions().createPanel());
  }

  private void doAddAction(AnActionButton button) {
    JBList<AddFileType> list = new JBList<>(AddFileType.values());
    list.setCellRenderer(SimpleListCellRenderer.create((label, value, index) -> {
      label.setIcon(value.getIcon());
      label.setText(value.getDisplayName());
    }));
    JBPopupFactory.getInstance()
            .createListPopupBuilder(list)
            .setTitle(InfraBundle.message("facet.context.edit.add.file.choose.type")).setItemChoosenCallback(() -> {
              AddFileType addFileType = list.getSelectedValue();
              if (addFileType != null) {
                this.performAddFiles(addFileType);
              }
            }).createPopup().show(button.getPreferredPopupPoint());
  }

  private void performAddFiles(cn.taketoday.assistant.facet.editor.AddFileType addFileType) {
    Project project = this.myModule.getProject();
    final Condition<VirtualFile> fileVisibleCondition = addFileType.getFileVisibleCondition(project);
    FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, true, false, true, true) {
      public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
        if (!super.isFileVisible(file, showHiddenFiles)) {
          return false;
        }
        if (file.isDirectory() || FileTypeRegistry.getInstance().isFileOfType(file, ArchiveFileType.INSTANCE)) {
          return true;
        }
        return fileVisibleCondition.value(file);
      }
    };
    descriptor.putUserData(LangDataKeys.MODULE_CONTEXT, this.myModule);
    descriptor.setTitle(addFileType.getDisplayName());
    VirtualFile[] files = FileChooser.chooseFiles(descriptor, this.myMainPanel, project, null);
    if (files.length == 0) {
      return;
    }
    DefaultMutableTreeNode treeNode = null;
    for (VirtualFile file : files) {
      treeNode = this.myBuilder.addFile(file);
      if (treeNode instanceof CheckedTreeNode) {
        ((CheckedTreeNode) treeNode).setChecked(true);
      }
    }
    updateFileSet();
    this.myTreeExpander.expandAll();
    TreeUtil.selectInTree(treeNode, false, this.myFilesTree, true);
  }

  private static List<InfraFileSet> getParentFileSetCandidates(InfraFileSet fileSet, Set<InfraFileSet> currentModuleParentCandidates) {

    Module module = fileSet.getFacet().getModule();
    Set<InfraFileSet> allSets = new HashSet<>();
    Set<Module> dependentModules = new HashSet<>();
    ModuleUtilCore.getDependencies(module, dependentModules);
    dependentModules.remove(module);

    for (Module dependentModule : dependentModules) {
      InfraFacet facet = InfraFacet.from(dependentModule);
      if (facet != null) {
        allSets.addAll(InfraFileSetService.of().getAllSets(facet));
      }
    }

    allSets.addAll(currentModuleParentCandidates);
    allSets.remove(fileSet);
    List<InfraFileSet> activeSets = ContainerUtil.filter(allSets, (set) -> !set.isRemoved());
    activeSets.sort(Comparator.comparing((Function<InfraFileSet, String>) set -> set.getFacet().getModule().getName()).thenComparing(InfraFileSet::getName));
    return activeSets;
  }

  @Nullable
  protected JComponent createCenterPanel() {
    return this.myMainPanel;
  }

  protected String getDimensionServiceKey() {
    return "infra file set editor";
  }

  public boolean isOKActionEnabled() {
    if (this.myOriginalSet.isNew() || !this.myFileSet.getName().equals(this.myOriginalSet.getName()) || this.myFileSet.getFiles().size() != this.myOriginalSet.getFiles().size()) {
      return true;
    }
    List<VirtualFilePointer> pointers = this.myFileSet.getFiles();
    for (int i = 0; i < pointers.size(); i++) {
      if (!pointers.get(i).getUrl().equals(this.myOriginalSet.getFiles().get(i).getUrl())) {
        return true;
      }
    }
    return !Comparing.haveEqualElements(this.myFileSet.getDependencyFileSets(), this.myOriginalSet.getDependencyFileSets());
  }

  protected void doOKAction() {
    updateFileSet();
    super.doOKAction();
  }

  @Nullable
  public JComponent getPreferredFocusedComponent() {
    return this.myOriginalSet.isNew() ? this.mySetName : this.myFilesTree;
  }

  @Nullable
  protected ValidationInfo doValidate() {
    String name = this.mySetName.getText();
    if (StringUtil.isEmptyOrSpaces(name)) {
      return new ValidationInfo(InfraBundle.message("facet.context.edit.name.validation.not.empty"), this.mySetName);
    }
    if (!this.myOriginalSet.isAutodetected() && this.myExistingContextNames.contains(name)) {
      return new ValidationInfo(InfraBundle.message("facet.context.edit.name.validation.already.exists"), this.mySetName);
    }
    return null;
  }

  private void updateFileSet() {
    this.myFileSet.setName(this.mySetName.getText());
    this.myFilesTree.updateFileSet(this.myFileSet);
    InfraFileSet dependencyFileSet = (InfraFileSet) this.myParentBox.getSelectedItem();
    this.myFileSet.setDependencies(ContainerUtil.createMaybeSingletonList(dependencyFileSet));
    getOKAction().setEnabled(isOKActionEnabled());
  }

  public InfraFileSet getEditedFileSet() {
    return this.myFileSet;
  }
}
