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

package cn.taketoday.assistant.context.chooser;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.LightColors;
import com.intellij.ui.components.JBList;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.Nls;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.editor.InfraEditorNotificationPanel;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public final class InfraMultipleContextsPanel extends InfraEditorNotificationPanel {
  private final PsiFile myPsiFile;
  private final List<InfraContextDescriptor> myDescriptors;

  InfraMultipleContextsPanel(FileEditor fileEditor, PsiFile psiFile, List<InfraContextDescriptor> descriptors,
          InfraContextDescriptor currentContext) {

    super(fileEditor, LightColors.SLIGHTLY_GREEN);
    this.myPsiFile = psiFile;
    this.myDescriptors = descriptors;
    Set<Module> modules = getModules(descriptors);
    JPanel myDescriptorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUIScale.scale(3), JBUIScale.scale(5)));
    this.add(myDescriptorPanel, "West");
    myDescriptorPanel.setToolTipText(message("multiple.context.tooltip.text"));
    myDescriptorPanel.add(this.addChangeContextLabel(currentContext));
    if (modules.size() > 0) {
      addModuleLabel(myDescriptorPanel, currentContext);
    }

    addTotalContextsDescriptionLabel(myDescriptorPanel, descriptors, modules);
    myDescriptorPanel.setBackground(this.getBackground());
    this.installOpenSettingsButton(psiFile.getProject());
  }

  private static void addTotalContextsDescriptionLabel(JPanel descriptorPanel, List<InfraContextDescriptor> descriptors, Set<Module> modules) {
    String message = modules.size() > 1 ? message("multiple.context.description.in.modules.label.text", descriptors.size(), modules.size()) : message(
            "multiple.context.description.label.text", descriptors.size());
    addLabel(descriptorPanel, message, null);
  }

  private static void addModuleLabel(JPanel descriptorPanel, InfraContextDescriptor currentContext) {
    Module module = currentContext.getModule();
    if (module != null) {
      addLabel(descriptorPanel, message("multiple.context.description.in.module.text", module.getName()), null);
    }

  }

  private static Set<Module> getModules(List<InfraContextDescriptor> descriptors) {
    Set<Module> modules = new LinkedHashSet();

    for (InfraContextDescriptor descriptor : descriptors) {
      Module module = descriptor.getModule();
      if (module != null) {
        modules.add(module);
      }
    }

    return modules;
  }

  private HyperlinkLabel addChangeContextLabel(InfraContextDescriptor currentContext) {
    HyperlinkLabel label = new HyperlinkLabel(currentContext.getName(), this.getBackground());
    label.setIcon(Icons.FileSet);
    label.addHyperlinkListener(new HyperlinkAdapter() {
      public void hyperlinkActivated(HyperlinkEvent e) {

        JBList<InfraContextDescriptor> list = new JBList(InfraMultipleContextsPanel.this.myDescriptors);
        list.installCellRenderer((contextDescriptor) -> {
          JPanel panel = new JPanel(new BorderLayout());
          Icon icon = contextDescriptor.equals(currentContext) ? AllIcons.Actions.Checked : EmptyIcon.create(AllIcons.Actions.Checked);
          String name1 = contextDescriptor.getName();
          if (contextDescriptor.isPredefinedContext()) {
            name1 = String.format("<html><b>%s</b></html>", name1);
          }

          JLabel descriptorLabel = new JLabel(name1, icon, SwingConstants.LEFT);
          descriptorLabel.setBorder(JBUI.Borders.emptyRight(10));
          panel.add(descriptorLabel, "West");
          Module module = contextDescriptor.getModule();
          if (module != null) {
            JPanel modulePanel = new JPanel(new BorderLayout());
            modulePanel.setOpaque(false);
            modulePanel.add(new JLabel("    " + module.getName()), "West");
            JLabel iconLabel = new JLabel("", ModuleType.get(module).getIcon(), SwingConstants.LEFT);
            iconLabel.setBorder(JBUI.Borders.emptyRight(2));
            modulePanel.add(iconLabel, "East");
            panel.add(modulePanel, "East");
          }

          panel.setBorder(JBUI.Borders.empty(1));
          return panel;
        });
        JBPopup popup = JBPopupFactory.getInstance().createListPopupBuilder(list).setItemChoosenCallback(() -> {
          InfraContextDescriptor value = list.getSelectedValue();
          if (value != null) {
            InfraMultipleContextsPanel.this.saveSelectedContext(value);
            Project project = InfraMultipleContextsPanel.this.myPsiFile.getProject();
            EditorNotifications.getInstance(project).updateAllNotifications();
            InfraModificationTrackersManager.from(project).fireMultipleContextsChanged();
            PsiManager.getInstance(project).dropPsiCaches();
          }

        }).setFilteringEnabled((o) -> {
          if (!(o instanceof InfraContextDescriptor descriptor)) {
            return null;
          }
          else {
            Module module = descriptor.getModule();
            return module != null ? descriptor.getName() + " " + module.getName() : descriptor.getName();
          }
        }).createPopup();
        DataContext context = DataManager.getInstance().getDataContext(label);
        popup.showInBestPositionFor(context);
      }
    });
    return label;
  }

  private void saveSelectedContext(InfraContextDescriptor value) {

    InfraMultipleContextsManager.of().persistDescriptor(this.myPsiFile, value);
  }

  private static void addLabel(JPanel panel, @Nls(capitalization = Nls.Capitalization.Sentence) String s, @Nullable Icon icon) {
    panel.add(new JLabel(s, icon, SwingConstants.LEFT));
  }
}
