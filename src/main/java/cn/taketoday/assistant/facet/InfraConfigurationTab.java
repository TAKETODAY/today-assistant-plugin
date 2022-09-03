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

import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetEditorValidator;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.icons.AllIcons;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DataManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CheckedActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.PlatformEditorBundle;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.ui.treeStructure.AutoExpandSimpleNodeListener;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.intellij.util.ObjectUtils;
import com.intellij.util.concurrency.NonUrgentExecutor;
import com.intellij.util.containers.OrderedSet;
import com.intellij.util.ui.tree.TreeUtil;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.JComponent;
import javax.swing.JPanel;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.facet.beans.CustomSetting;
import cn.taketoday.assistant.facet.editor.FileSetEditor;
import cn.taketoday.assistant.facet.nodes.ConfigFileNode;
import cn.taketoday.assistant.facet.nodes.DependencyNode;
import cn.taketoday.assistant.facet.nodes.FileSetNode;
import cn.taketoday.assistant.facet.nodes.FilesetGroupNode;
import cn.taketoday.assistant.facet.searchers.CodeConfigSearcher;
import cn.taketoday.assistant.facet.searchers.XmlConfigSearcher;
import cn.taketoday.assistant.facet.validation.FileSetDependenciesValidator;
import cn.taketoday.assistant.facet.validation.FilesetContainsConfigFilesInTestsSourceValidator;
import cn.taketoday.assistant.facet.validation.UnmappedConfigurationFilesValidator;
import cn.taketoday.lang.Nullable;

public class InfraConfigurationTab extends FacetEditorTab implements Disposable {
  private final JPanel mainPanel;
  private final SimpleTree tree;
  private final InfraConfigurationTabSettings configurationTabSettings;
  private final InfraFacet facet;
  private final FacetEditorContext context;
  private final FacetValidatorsManager validatorsManager;
  private final StructureTreeModel<SimpleTreeStructure> treeModel;
  private boolean modified;
  private final OrderedSet<InfraFileSet> buffer = new OrderedSet<>();
  private final SimpleNode root = new SimpleNode() {

    @Override
    public SimpleNode[] getChildren() {
      List<SimpleNode> nodes = new ArrayList<>(buffer.size());
      for (InfraFileSet infraFileSet : buffer) {
        if (!infraFileSet.isRemoved()) {
          FileSetNode fileSetNode = new FileSetNode(infraFileSet, configurationTabSettings, this);
          nodes.add(fileSetNode);
        }
      }
      return nodes.toArray(new SimpleNode[0]);
    }

    @Override
    public boolean isAutoExpandNode() {
      return true;
    }
  };

  public InfraConfigurationTab(FacetEditorContext context, FacetValidatorsManager validatorsManager) {
    this.facet = (InfraFacet) context.getFacet();
    this.configurationTabSettings = InfraConfigurationTabSettings.getInstance(context.getProject());
    this.context = context;
    this.validatorsManager = validatorsManager;
    SimpleTreeStructure structure = new SimpleTreeStructure() {
      @Override
      public Object getRootElement() {
        return InfraConfigurationTab.this.root;
      }
    };
    this.treeModel = new StructureTreeModel<>(structure, this);
    this.tree = new SimpleTree();
    this.tree.setRootVisible(false);
    this.tree.getEmptyText().setText(InfraBundle.message("config.no.contexts.defined"));
    AsyncTreeModel asyncTreeModel = new AsyncTreeModel(this.treeModel, this);
    asyncTreeModel.addTreeModelListener(new AutoExpandSimpleNodeListener(this.tree));
    this.tree.setModel(asyncTreeModel);
    this.mainPanel = new JPanel(new BorderLayout());
    JBLoadingPanel loadingPanel = new JBLoadingPanel(new BorderLayout(), this);
    loadingPanel.setLoadingText(InfraBundle.message("config.detecting.contexts"));
    loadingPanel.startLoading();
    this.mainPanel.add(loadingPanel, "Center");

    ReadAction.nonBlocking(() -> InfraFileSetService.of().getAllSets(this.facet))
            .finishOnUiThread(ModalityState.any(), allFileSets -> {
              ToolbarDecorator decorator = ToolbarDecorator.createDecorator(this.tree)
                      .setAddAction(new CreateApplicationContextAction())
                      .setRemoveAction(new RemoveSelectedNodesAction())
                      .setRemoveActionUpdater(e -> getCurrentFileSet() != null)
                      .setEditAction(new EditApplicationContextAction(this.facet))
                      .setEditActionUpdater(e2 -> {
                        InfraFileSet fileSet = getCurrentFileSet();
                        return fileSet != null && !fileSet.isAutodetected();
                      })
                      .disableUpDownActions();

              ActionGroup additionalSettingsActionGroup = getAdditionalSettingsActionGroup(allFileSets);
              if (additionalSettingsActionGroup != null) {
                decorator.addExtraAction(AnActionButton.fromAction(additionalSettingsActionGroup));
              }
              for (InfraFileSetEditorCustomization customization : InfraFileSetEditorCustomization.array()) {
                Stream<InfraFileSet> stream = allFileSets.stream();
                Objects.requireNonNull(customization);
                if (stream.anyMatch(customization::isApplicable)) {
                  for (AnAction extraAction : customization.getExtraActions()) {
                    AnActionButton.AnActionButtonWrapper anActionButtonWrapper = new AnActionButton.AnActionButtonWrapper(extraAction.getTemplatePresentation(), extraAction) {
                      @Override
                      public void actionPerformed(AnActionEvent e3) {
                        InfraFileSet set = getCurrentFileSet();
                        DataManager.getInstance().saveInDataContext(e3.getDataContext(), InfraFileSetEditorCustomization.EXTRA_ACTION_FILESET, set);

                        super.actionPerformed(e3);
                        int idx = buffer.indexOf(set);
                        buffer.remove(set);
                        facet.getConfiguration().setModified();
                        List<InfraFileSet> modelProviderSets = InfraFileSetService.of().getModelProviderSets(facet);

                        for (InfraFileSet fileSet : modelProviderSets) {
                          if (fileSet.getId().equals(set.getId())) {
                            buffer.add(idx, fileSet);
                            modified = true;
                            treeModel.invalidate().onSuccess(o -> {
                              selectFileSet(fileSet);
                            });
                            break;
                          }
                        }
                        validateFileSetConfiguration();
                      }
                    };
                    anActionButtonWrapper.addCustomUpdater(e3 -> {
                      InfraFileSet currentFileSet = getCurrentFileSet();
                      if (currentFileSet == null || !currentFileSet.isAutodetected()) {
                        return false;
                      }
                      return customization.isApplicable(currentFileSet);
                    });
                    decorator.addExtraAction(anActionButtonWrapper);
                  }
                }
              }
              JPanel decoratorPanel = decorator.createPanel();
              ActionToolbar displaySettingsToolbar = ActionManager.getInstance()
                      .createActionToolbar("InfraFacetDisplaySettingsToolbar", getDisplaySettingsActionGroup(), false);
              displaySettingsToolbar.setTargetComponent(decoratorPanel);
              decoratorPanel.add(displaySettingsToolbar.getComponent(), "East");
              loadingPanel.stopLoading();
              this.mainPanel.remove(loadingPanel);
              this.mainPanel.add(decoratorPanel, "Center");
            })
            .submit(NonUrgentExecutor.getInstance());
    FacetEditorValidator dependenciesValidator = new FileSetDependenciesValidator(Collections.unmodifiableSet(this.buffer));
    this.validatorsManager.registerValidator(dependenciesValidator);
    FacetEditorValidator unmappedConfigurationFilesValidator = new UnmappedConfigurationFilesValidator(Collections.unmodifiableSet(this.buffer), this.context.getModule());
    this.validatorsManager.registerValidator(unmappedConfigurationFilesValidator);
    FacetEditorValidator containsTestConfigsValidator = new FilesetContainsConfigFilesInTestsSourceValidator(this.context.getModule(), this.buffer);
    this.validatorsManager.registerValidator(containsTestConfigsValidator);
  }

  private Set<CustomSetting> getCustomSettings() {
    return this.facet.getConfiguration().getCustomSettings();
  }

  @Nullable
  private ActionGroup getAdditionalSettingsActionGroup(Set<InfraFileSet> allFileSets) {
    Set<CustomSetting> allCustomSettings = new LinkedHashSet<>(getCustomSettings());
    for (InfraFileSetEditorCustomization customization : InfraFileSetEditorCustomization.array()) {
      List<CustomSetting> customSettings = customization.getCustomSettings();
      customSettings.forEach(allCustomSettings::remove);

      for (InfraFileSet set : allFileSets) {
        if (customization.isApplicable(set)) {
          allCustomSettings.addAll(customSettings);
          break;
        }
      }

    }
    MyCheckedActionGroup actionGroup = new MyCheckedActionGroup();
    for (CustomSetting setting : allCustomSettings) {
      if (setting instanceof CustomSetting.BOOLEAN) {
        actionGroup.add(new CheckAction((CustomSetting.BOOLEAN) setting));
      }
    }
    if (actionGroup.getChildrenCount() == 0) {
      return null;
    }
    actionGroup.getTemplatePresentation().setText(InfraBundle.messagePointer("configuration.tab.additional.settings"));
    actionGroup.getTemplatePresentation().setIcon(AllIcons.General.Settings);
    return actionGroup;
  }

  private ActionGroup getDisplaySettingsActionGroup() {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new ToggleAction(PlatformEditorBundle.message("action.sort.alphabetically"),
            PlatformEditorBundle.message("action.sort.alphabetically.description"), AllIcons.ObjectBrowser.Sorted) {

      @Override
      public boolean isSelected(AnActionEvent e) {
        return configurationTabSettings.isSortAlpha();
      }

      @Override
      public void setSelected(AnActionEvent e, boolean state) {
        configurationTabSettings.setSortAlpha(state);
        treeModel.invalidate();
      }

    });
    group.addSeparator();
    CommonActionsManager actionManager = CommonActionsManager.getInstance();
    DefaultTreeExpander defaultTreeExpander = new DefaultTreeExpander(this.tree);
    group.add(actionManager.createExpandAllAction(defaultTreeExpander, this.tree));
    group.add(actionManager.createCollapseAllAction(defaultTreeExpander, this.tree));
    return group;
  }

  private void remove() {
    List<SimpleNode> nodes = TreeUtil.collectSelectedObjects(this.tree, path -> {
      return TreeUtil.getLastUserObject(SimpleNode.class, path);
    });
    for (SimpleNode node : nodes) {
      if (node instanceof DependencyNode) {
        getFileSetForNode(node).removeDependency(((DependencyNode) node).getFileSet());
      }
      else if (node instanceof FileSetNode) {
        InfraFileSet fileSet = ((FileSetNode) node).getFileSet();
        int result = Messages.showYesNoDialog(this.mainPanel, InfraBundle.message("facet.context.remove.message", fileSet.getName()),
                InfraBundle.message("facet.context.remove.title"), Messages.getQuestionIcon());
        if (result == 0) {
          if (fileSet.isAutodetected()) {
            fileSet.setRemoved(true);
            this.buffer.add(fileSet);
          }
          else {
            this.buffer.remove(fileSet);
          }
          for (InfraFileSet set : this.buffer) {
            set.removeDependency(fileSet);
          }
        }
      }
      else if (node instanceof FilesetGroupNode) {
        InfraFileSet fileSet2 = getFileSetForNode(node);
        if (!fileSet2.isAutodetected()) {
          Set<VirtualFilePointer> filePointers = ((FilesetGroupNode) node).getFilePointers();
          for (VirtualFilePointer filePointer : filePointers) {
            fileSet2.removeFile(filePointer);
          }
        }
      }
      else if (node instanceof ConfigFileNode) {
        InfraFileSet fileSet3 = getFileSetForNode(node);
        if (!fileSet3.isAutodetected()) {
          VirtualFilePointer filePointer2 = ((ConfigFileNode) node).getFilePointer();
          fileSet3.removeFile(filePointer2);
        }
      }
    }
    this.modified = true;
    this.treeModel.invalidate().onSuccess(o -> {
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
        IdeFocusManager.getGlobalInstance().requestFocus(this.tree, true);
      });
    });
  }

  private static InfraFileSet getFileSetForNode(SimpleNode node) {
    FileSetNode fileSetNode = getFileSetNodeFor(node);
    assert fileSetNode != null;

    return fileSetNode.getFileSet();
  }

  @Nullable
  private InfraFileSet getCurrentFileSet() {
    SimpleNode node = this.tree.getSelectedNode();
    FileSetNode currentFileSetNode = getFileSetNodeFor(node);
    if (currentFileSetNode == null) {
      return null;
    }
    return currentFileSetNode.getFileSet();
  }

  @Nullable
  private static FileSetNode getFileSetNodeFor(@Nullable SimpleNode node) {
    while (node != null) {
      if (node instanceof FileSetNode) {
        return (FileSetNode) node;
      }
      node = node.getParent();
    }
    return null;
  }

  public String getHelpTopic() {
    return "reference.settings.project.modules.today.facet";
  }

  public String getDisplayName() {
    return InfraBundle.message("config.display.name");
  }

  public JComponent createComponent() {
    return DumbService.getInstance(this.context.getProject()).wrapGently(this.mainPanel, this);
  }

  public boolean isModified() {
    for (CustomSetting setting : getCustomSettings()) {
      if (setting.isModified()) {
        return true;
      }
    }
    return this.modified;
  }

  public void apply() {
    if (this.facet.isDisposed()) {
      return;
    }
    this.facet.removeFileSets();
    for (InfraFileSet fileSet : this.buffer) {
      if (!fileSet.isAutodetected() || (fileSet.isAutodetected() && fileSet.isRemoved())) {
        this.facet.addFileSet(fileSet);
      }
    }
    this.facet.getConfiguration().setModified();
    for (CustomSetting setting : getCustomSettings()) {
      setting.apply();
    }
    this.modified = false;
    validateFileSetConfiguration();
  }

  public void reset() {
    this.buffer.clear();
    this.tree.setPaintBusy(true);
    ReadAction.nonBlocking(() -> InfraFileSetService.of().getAllSets(this.facet)).finishOnUiThread(ModalityState.any(), fileSets -> {
      for (InfraFileSet fileSet : fileSets) {
        this.buffer.add(new InfraFileSetImpl(fileSet));
      }
      this.treeModel.invalidate();
      this.tree.setPaintBusy(false);
      this.tree.setSelectionRow(0);
      validateFileSetConfiguration();
    }).submit(NonUrgentExecutor.getInstance());
    for (CustomSetting setting : getCustomSettings()) {
      setting.reset();
    }
    this.modified = false;
  }

  public void disposeUIResources() {
    Disposer.dispose(this);
  }

  private void selectFileSet(InfraFileSet fileSet) {
    TreeUtil.promiseSelect(this.tree, path -> {
      Object object = TreeUtil.getLastUserObject(path);
      if (this.root.equals(object)) {
        return TreeVisitor.Action.CONTINUE;
      }
      FileSetNode node = ObjectUtils.tryCast(object, FileSetNode.class);
      if (node != null && node.getFileSet().getId().equals(fileSet.getId())) {
        return TreeVisitor.Action.INTERRUPT;
      }
      return TreeVisitor.Action.SKIP_CHILDREN;
    });
  }

  @Override
  public void dispose() { }

  private void validateFileSetConfiguration() {
    this.validatorsManager.validate();
  }

  private class CreateApplicationContextAction implements AnActionButtonRunnable {
    private CreateApplicationContextAction() {
    }

    public void run(AnActionButton button) {
      InfraFileSetService fileSetService = InfraFileSetService.of();
      String uniqueId = fileSetService.getUniqueId(buffer);
      String uniqueName = fileSetService.getUniqueName(InfraBundle.message("facet.context.default.name"), buffer);
      InfraFileSet fileSet = new InfraFileSetImpl(uniqueId, uniqueName, facet) {
        @Override
        public boolean isNew() {
          return true;
        }
      };
      FileSetEditor editor = createFileSetEditor(fileSet);
      editor.show();
      if (editor.getExitCode() == 0) {
        InfraFileSet editedFileSet = editor.getEditedFileSet();
        Disposer.register(facet, editedFileSet);
        buffer.add(editedFileSet);
        modified = true;
        treeModel.invalidate().onSuccess(o -> {
          selectFileSet(fileSet);
        });
        validateFileSetConfiguration();
      }
    }
  }

  private final class EditApplicationContextAction implements AnActionButtonRunnable {
    private final InfraFacet myInfraFacet;

    private EditApplicationContextAction(InfraFacet infraFacet) {
      this.myInfraFacet = infraFacet;
    }

    public void run(AnActionButton button) {
      InfraFileSet fileSet = getCurrentFileSet();
      if (fileSet != null) {
        FileSetEditor editor = createFileSetEditor(fileSet);
        editor.show();
        if (editor.getExitCode() == 0) {
          modified = true;
          int idx = buffer.indexOf(fileSet);
          buffer.remove(fileSet);
          InfraFileSet edited = editor.getEditedFileSet();
          Disposer.register(this.myInfraFacet, edited);
          buffer.add(idx, edited);
          edited.setAutodetected(false);
          treeModel.invalidate().onSuccess(o -> {
            selectFileSet(edited);
          });
          validateFileSetConfiguration();
        }
      }
    }
  }

  private FileSetEditor createFileSetEditor(InfraFileSet fileSet) {
    return new FileSetEditor(this.mainPanel, this.context.getModule(), fileSet, this.buffer, new XmlConfigSearcher(this.context.getModule()),
            new CodeConfigSearcher(this.context.getModule()));
  }

  private class RemoveSelectedNodesAction implements AnActionButtonRunnable {

    public void run(AnActionButton button) {
      remove();
      validateFileSetConfiguration();
    }
  }

  public static class MyCheckedActionGroup extends DefaultActionGroup implements CheckedActionGroup {
    MyCheckedActionGroup(CheckAction... actions) {
      super(actions);
      setPopup(true);
    }
  }

  public static final class CheckAction extends ToggleAction {
    private final CustomSetting.BOOLEAN myBean;

    private CheckAction(CustomSetting.BOOLEAN bean) {
      super(bean.getDescription());
      this.myBean = bean;
    }

    public CustomSetting.BOOLEAN getBean() {
      return this.myBean;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      Boolean value = this.myBean.getValue();
      return value == null ? this.myBean.getDefaultValue() : value;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      this.myBean.setBooleanValue(state);
    }
  }
}
