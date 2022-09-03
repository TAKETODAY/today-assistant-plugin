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

package cn.taketoday.assistant.app.run.editor;

import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference;
import com.intellij.psi.meta.PsiPresentableMetaData;
import com.intellij.ui.CommonActionsPanel;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.TableUtil;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.table.TableView;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.AbstractTableCellEditor;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.StatusText;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import cn.taketoday.assistant.app.InfraConfigFileConstants;
import cn.taketoday.assistant.app.application.config.InfraHintReferencesProvider;
import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.assistant.app.run.InfraAdditionalParameter;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraAppBundle.message;

class AdditionalParamsTableView extends TableView<InfraAdditionalParameter> {
  private final ListTableModel<InfraAdditionalParameter> myAdditionalParamsModel;
  private final Project myProject;
  @Nullable
  private Module myModule;

  AdditionalParamsTableView(Project project) {
    this.myProject = project;
    Shortcut[] quickDocShortcuts = KeymapUtil.getActiveKeymapShortcuts("QuickJavaDoc").getShortcuts();
    String quickDocShortcutText = quickDocShortcuts.length == 0 ? null : "Full documentation available (" + KeymapUtil.getShortcutText(quickDocShortcuts[0]) + ")";
    this.myAdditionalParamsModel = new ListTableModel<>(new ColumnInfo[] { new ColumnInfo<InfraAdditionalParameter, Boolean>("") {
      public Boolean valueOf(InfraAdditionalParameter parameter) {
        return parameter.isEnabled();
      }

      public Class<?> getColumnClass() {
        return Boolean.class;
      }

      public void setValue(InfraAdditionalParameter parameter, Boolean value) {
        parameter.setEnabled(value);
      }

      public boolean isCellEditable(InfraAdditionalParameter parameter) {
        return true;
      }
    }, new ColumnInfo<InfraAdditionalParameter, String>(message("additional.params.table.name")) {
      private final TableCellRenderer myRenderer = new DefaultTableCellRenderer() {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
          DefaultTableCellRenderer component = (DefaultTableCellRenderer) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
          if (value == null || "".equals(value)) {
            component.setBorder(BorderFactory.createLineBorder(JBUI.CurrentTheme.Focus.errorColor(false), 3));
          }
          return component;
        }
      };

      public TableCellEditor getEditor(InfraAdditionalParameter parameter) {
        if (AdditionalParamsTableView.this.myModule == null || DumbService.isDumb(AdditionalParamsTableView.this.myModule.getProject())) {
          return createDefaultParameterEditor(AdditionalParamsTableView.this.myProject, parameter.getName());
        }
        return new MyInfraNameTableCellEditor(AdditionalParamsTableView.this.myModule);
      }

      @Nullable
      public String valueOf(InfraAdditionalParameter parameter) {
        return parameter.getName();
      }

      public void setValue(InfraAdditionalParameter parameter, String value) {
        parameter.setName(StringUtil.trim(value));
      }

      public boolean isCellEditable(InfraAdditionalParameter parameter) {
        return parameter.isEnabled();
      }

      public TableCellRenderer getRenderer(InfraAdditionalParameter parameter) {
        return this.myRenderer;
      }

      public TableCellRenderer getCustomizedRenderer(InfraAdditionalParameter o, TableCellRenderer renderer) {
        MetaConfigKey key = AdditionalParamsTableView.this.findMetaConfigKey(o);
        if (key != null) {
          String toolTipText = key.getDescriptionText().getFullText();
          if (quickDocShortcutText != null) {
            if (!toolTipText.isEmpty()) {
              toolTipText = toolTipText + "\n\n" + quickDocShortcutText;
            }
            else {
              toolTipText = quickDocShortcutText;
            }
          }
          ((JComponent) renderer).setToolTipText(toolTipText);
        }
        return renderer;
      }

      public String getPreferredStringValue() {
        return "012345678901234";
      }
    }, new ColumnInfo<InfraAdditionalParameter, String>(message("additional.params.table.value")) {
      @Nullable
      public String valueOf(InfraAdditionalParameter parameter) {
        return parameter.getValue();
      }

      public void setValue(InfraAdditionalParameter parameter, String value) {
        parameter.setValue(value);
      }

      public boolean isCellEditable(InfraAdditionalParameter parameter) {
        return parameter.isEnabled();
      }

      public TableCellEditor getEditor(InfraAdditionalParameter parameter) {
        MetaConfigKey key = AdditionalParamsTableView.this.findMetaConfigKey(parameter);
        if (key == null) {
          return createDefaultParameterEditor(AdditionalParamsTableView.this.myProject, parameter.getValue());
        }
        List<Object> variants = AdditionalParamsTableView.this.getCompletionVariants(key, parameter.getName());
        return variants.isEmpty() ? createDefaultParameterEditor(AdditionalParamsTableView.this.myProject,
                parameter.getValue()) : AdditionalParamsTableView.this.createValueTableCellEditor(parameter, variants);
      }
    } });
    setModelAndUpdateColumns(this.myAdditionalParamsModel);
    getTableHeader().setReorderingAllowed(false);
    TableUtil.setupCheckboxColumn(getColumnModel().getColumn(0), getColumnModel().getColumnMargin());
    StatusText emptyText = getEmptyText();
    emptyText.setText(message("additional.params.table.empty.text"));
    emptyText.appendSecondaryText(message("additional.params.table.add"), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        AdditionalParamsTableView.this.addAdditionalParameter();
      }
    });
    ShortcutSet shortcutSet = CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.ADD);
    Shortcut shortcut = ArrayUtil.getFirstElement(shortcutSet.getShortcuts());
    if (shortcut != null) {
      emptyText.appendSecondaryText(" (" + KeymapUtil.getShortcutText(shortcut) + ")", StatusText.DEFAULT_ATTRIBUTES, null);
    }
    setShowGrid(false);
    setColumnSelectionAllowed(false);
    setRowHeight((new LanguageTextField().getPreferredSize().height * 2) + JBUIScale.scale(1));
    setVisibleRowCount(6);
    new TableSpeedSearch(this);
    new AnAction(message("additional.params.table.quick.doc")) {

      public void actionPerformed(AnActionEvent e) {
        if (AdditionalParamsTableView.this.myModule == null) {
          return;
        }
        List<InfraAdditionalParameter> selectedParams = AdditionalParamsTableView.this.getSelectedObjects();
        if (selectedParams.size() != 1) {
          return;
        }
        showHint(AdditionalParamsTableView.this.myModule, selectedParams.get(0).getName());
      }
    }.registerCustomShortcutSet(new CustomShortcutSet(quickDocShortcuts), this);
  }

  List<InfraAdditionalParameter> getAdditionalParameters() {
    return this.myAdditionalParamsModel.getItems();
  }

  void setAdditionalParameters(List<InfraAdditionalParameter> parameters) {
    List<InfraAdditionalParameter> copy = new ArrayList<>();
    for (InfraAdditionalParameter parameter : parameters) {
      copy.add(new InfraAdditionalParameter(parameter.isEnabled(), parameter.getName(), parameter.getValue()));
    }
    this.myAdditionalParamsModel.setItems(copy);
  }

  void addAdditionalParameter() {
    this.stopEditing();
    this.myAdditionalParamsModel.addRow(new InfraAdditionalParameter(true, "", ""));
    int index = this.myAdditionalParamsModel.getRowCount() - 1;
    this.setRowSelectionInterval(index, index);
    SwingUtilities.invokeLater(() -> {
      TableUtil.scrollSelectionToVisible(this);
      TableUtil.editCellAt(this, index, 1);
    });
  }

  void setModule(@Nullable Module module) {
    this.myModule = module;
  }

  protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
    if (e.isAltDown() || e.isMetaDown() || e.getID() != 401) {
      return super.processKeyBinding(ks, e, condition, pressed);
    }
    if (e.isControlDown()) {
      if (e.getKeyCode() == 10 && isEditing()) {
        stopEditing();
      }
      return super.processKeyBinding(ks, e, condition, pressed);
    }
    else if (isEditing() && (e.getKeyCode() == 9 || ((e.getKeyCode() == 38 || e.getKeyCode() == 40) && e.getModifiers() == 0))) {
      int row = getSelectedRow();
      int column = getSelectedColumn();
      boolean result = super.processKeyBinding(ks, e, condition, pressed);
      if (!isEditing() && (row != getSelectedRow() || column != getSelectedColumn())) {
        TableUtil.editCellAt(this, getSelectedRow(), getSelectedColumn());
        e.consume();
        return true;
      }
      return result;
    }
    else if (e.getKeyCode() == 10 && e.getModifiers() == 0) {
      if (isEditing()) {
        stopEditing();
      }
      else {
        TableUtil.editCellAt(this, getSelectedRow(), getSelectedColumn());
      }
      e.consume();
      return true;
    }
    else {
      return super.processKeyBinding(ks, e, condition, pressed);
    }
  }

  private TableCellEditor createValueTableCellEditor(InfraAdditionalParameter parameter, List<Object> variants) {
    TextFieldWithAutoCompletion<Object> field = new TextFieldWithAutoCompletion<>(
            this.myModule.getProject(), new ApplicationPropertiesAutoCompletionListProvider(variants), true,
            parameter.getValue());
    return new AbstractTableCellEditor() {
      public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return field;
      }

      public Object getCellEditorValue() {
        return field.getText();
      }
    };
  }

  private List<Object> getCompletionVariants(MetaConfigKey key, String keyText) {
    ProcessingContext processingContext = new ProcessingContext();
    if (!key.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      processingContext.put(InfraHintReferencesProvider.HINT_REFERENCES_CONFIG_KEY_TEXT, keyText);
    }
    PsiElement dummyValueNode = createDummyValueNode();
    List<TextRange> valueTextRanges = Collections.singletonList(ElementManipulators.getValueTextRange(dummyValueNode));
    PsiReference[] references = InfraHintReferencesProvider.getInstance().getValueReferences(this.myModule, key, null, dummyValueNode, valueTextRanges, processingContext);
    PsiReference reference = ArrayUtil.getFirstElement(references);
    if (reference == null) {
      return Collections.emptyList();
    }
    if (reference instanceof JavaClassReference javaClassReference) {
      List<String> names = javaClassReference.getSuperClasses();
      PsiElement completionContext = javaClassReference.getCompletionContext();
      if (!names.isEmpty() && (completionContext instanceof PsiPackage psiPackage)) {
        List<Object> variants = new ArrayList<>();
        javaClassReference.processSubclassVariants(
                psiPackage, ArrayUtilRt.toStringArray(names), variants::add);
        return variants;
      }
    }
    return ContainerUtil.filter(reference.getVariants(), o -> {
      return !(o instanceof PsiNamedElement) || ((PsiNamedElement) o).getName() != null;
    });
  }

  private PsiElement createDummyValueNode() {
    assert this.myModule != null;
    PsiFile dummyFile = PsiFileFactory.getInstance(this.myModule.getProject()).createFileFromText(
            InfraConfigFileConstants.APPLICATION_PROPERTIES, PropertiesFileType.INSTANCE, "dummyKey=x\n");

    dummyFile.putUserData(ModuleUtilCore.KEY_MODULE, this.myModule);
    PropertiesFile propertiesFile = (PropertiesFile) dummyFile;
    IProperty dummyKey = propertiesFile.findPropertyByKey("dummyKey");

    assert dummyKey != null;

    PropertyImpl dummyProperty = (PropertyImpl) dummyKey.getPsiElement();
    ASTNode valueNode = dummyProperty.getValueNode();

    assert valueNode != null;
    return valueNode.getPsi();
  }

  @Nullable
  private MetaConfigKey findMetaConfigKey(InfraAdditionalParameter parameter) {
    if (this.myModule == null || DumbService.isDumb(this.myModule.getProject())) {
      return null;
    }
    return InfraApplicationMetaConfigKeyManager.getInstance().findApplicationMetaConfigKey(this.myModule, parameter.getName());
  }

  private static PsiElement createDummyKeyNode(Module module, String prefix) {
    PsiFile dummyFile = PsiFileFactory.getInstance(module.getProject()).createFileFromText(
            InfraConfigFileConstants.APPLICATION_PROPERTIES, PropertiesFileType.INSTANCE, prefix);
    dummyFile.putUserData(ModuleUtilCore.KEY_MODULE, module);
    PropertiesFile propertiesFile = (PropertiesFile) dummyFile;
    IProperty dummyKey = propertiesFile.findPropertyByKey(prefix);

    assert dummyKey != null;

    PropertyImpl dummyProperty = (PropertyImpl) dummyKey.getPsiElement();
    ASTNode keyNode = dummyProperty.getKeyNode();
    assert keyNode != null;
    return keyNode.getPsi();
  }

  private static TableCellEditor createDefaultParameterEditor(Project project, String value) {
    LanguageTextField field = new LanguageTextField(PlainTextLanguage.INSTANCE, project, value);
    return new AbstractTableCellEditor() {
      public Component getTableCellEditorComponent(JTable table, Object value2, boolean isSelected, int row, int column) {
        return field;
      }

      public Object getCellEditorValue() {
        return field.getText();
      }
    };
  }

  private static void showHint(Module module, String configKey) {
    MetaConfigKey key = InfraApplicationMetaConfigKeyManager.getInstance().findApplicationMetaConfigKey(module, configKey);
    DocumentationManager documentationManager = DocumentationManager.getInstance(module.getProject());
    documentationManager.restorePopupBehavior();
    if (key == null) {
      PsiFile psiFile = PsiFileFactory.getInstance(module.getProject()).createFileFromText(PlainTextLanguage.INSTANCE, "");
      documentationManager.showJavaDocInfo(psiFile, null, true, null);
      return;
    }
    PsiElement propertyElement = createDummyKeyNode(module, configKey).getParent();
    propertyElement.getContainingFile().putUserData(InfraApplicationMetaConfigKeyManager.ELEMENT_IN_EXTERNAL_CONTEXT, Boolean.TRUE);
    documentationManager.showJavaDocInfo(propertyElement, propertyElement, true, null);
  }

  private static class MyInfraNameTableCellEditor extends AbstractTableCellEditor {
    private final TextFieldWithAutoCompletion<Object> myTextField;

    MyInfraNameTableCellEditor(Module module) {
      List<? extends MetaConfigKey> allKeys = InfraApplicationMetaConfigKeyManager.getInstance().getAllMetaConfigKeys(module);
      List<Object> variants2 = new ArrayList<>(allKeys);
      for (MetaConfigKey key : allKeys) {
        if (key.isAccessType(MetaConfigKey.AccessType.MAP_GROUP)) {
          String prefix = key.getName() + ".";
          PsiElement dummyKeyNode = createDummyKeyNode(module, prefix);
          TextRange textRange = TextRange.from(prefix.length(), 0);
          PsiReference[] references = InfraHintReferencesProvider.getInstance().getKeyReferences(key, dummyKeyNode, textRange, new ProcessingContext());
          PsiReference reference = ArrayUtil.getFirstElement(references);
          SmartList<String> smartList = new SmartList<>();
          if (reference != null) {
            for (Object variant : reference.getVariants()) {
              if (variant instanceof LookupElement) {
                smartList.add(prefix + ((LookupElement) variant).getLookupString());
              }
            }
          }
          if (smartList.isEmpty()) {
            smartList.add(prefix);
          }
          variants2.remove(key);
          for (String hint : smartList) {
            variants2.add(key.getPresentation().getLookupElement(hint));
          }
        }
      }
      this.myTextField = new TextFieldWithAutoCompletion<>(module.getProject(), new ApplicationPropertiesAutoCompletionListProvider(variants2), true, null);
      Shortcut[] shortcuts = KeymapUtil.getActiveKeymapShortcuts("QuickJavaDoc").getShortcuts();
      new AnAction(message("additional.params.table.quick.doc")) {

        public void actionPerformed(AnActionEvent e) {
          showHint(module, MyInfraNameTableCellEditor.this.myTextField.getText());
        }
      }.registerCustomShortcutSet(new CustomShortcutSet(shortcuts), this.myTextField);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      this.myTextField.setText((String) value);
      return this.myTextField;
    }

    public Object getCellEditorValue() {
      return this.myTextField.getText();
    }
  }

  private static class ApplicationPropertiesAutoCompletionListProvider extends TextFieldWithAutoCompletionListProvider<Object> {

    ApplicationPropertiesAutoCompletionListProvider(@Nullable Collection<Object> variants) {
      super(variants);
    }

    protected String getLookupString(Object item) {
      if (item instanceof MetaConfigKey) {
        return ((MetaConfigKey) item).getName();
      }
      else if (item instanceof String str) {
        return str;
      }
      else if (item instanceof LookupElement) {
        return ((LookupElement) item).getLookupString();
      }
      else {
        if (item instanceof PsiNamedElement) {
          Ref<String> name2 = new Ref<>();
          ApplicationManager.getApplication().runReadAction(() -> {
            name2.set(((PsiNamedElement) item).getName());
          });
          if (name2.get() != null) {
            return name2.get();
          }
        }
        return item + " " + item.getClass();
      }
    }

    public LookupElementBuilder createLookupBuilder(Object item) {
      if (item instanceof MetaConfigKey) {
        return ((MetaConfigKey) item).getPresentation().getLookupElement();
      }
      else if (item instanceof LookupElementBuilder lookupElementBuilder) {
        return lookupElementBuilder;
      }
      else {
        return super.createLookupBuilder(item);
      }
    }

    @Nullable
    protected Icon getIcon(Object item) {
      if (item instanceof PsiPresentableMetaData) {
        return ((PsiPresentableMetaData) item).getIcon();
      }
      if (item instanceof Iconable) {
        return ((Iconable) item).getIcon(0);
      }
      return super.getIcon(item);
    }
  }
}
