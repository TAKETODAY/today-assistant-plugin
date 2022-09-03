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

package cn.taketoday.assistant.model.actions.patterns.frameworks.ui;

import com.intellij.DynamicBundle;
import com.intellij.facet.ui.FacetEditorsFactory;
import com.intellij.facet.ui.libraries.LibrariesValidationComponent;
import com.intellij.openapi.Disposable;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.BrowserLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.UIUtil;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ChooseTemplatesForm implements Disposable {
  private JPanel myChoosePanel;
  private JPanel myTableViewPanel;
  private JPanel myLibsPanel;
  private final List<cn.taketoday.assistant.model.actions.patterns.frameworks.ui.TemplateInfo> myTemplateInfos;
  private final cn.taketoday.assistant.model.actions.patterns.frameworks.ui.LibrariesInfo myLibInfo;
  private LibrariesValidationComponent myLibrariesValidationComponent;

  private static final String JAVADOC = "Javadoc";

  private static final String DETAILS = "Details";

  private void $$$setupUI$$$() {
    JPanel jPanel = new JPanel();
    this.myChoosePanel = jPanel;
    jPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1, false, false));
    jPanel.add(new Spacer(), new GridConstraints(2, 0, 1, 1, 0, 2, 1, 6, null, null, null));
    JPanel jPanel2 = new JPanel();
    this.myTableViewPanel = jPanel2;
    jPanel2.setLayout(new GridLayoutManager(1, 1, new Insets(5, 5, 5, 5), -1, -1, false, false));
    jPanel2.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithIndent");
    jPanel.add(jPanel2, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
    jPanel2.setBorder(IdeBorderFactory.PlainSmallWithIndent.createTitledBorder(BorderFactory.createEtchedBorder(),
            DynamicBundle.getBundle("messages/InfraBundle", ChooseTemplatesForm.class).getString("choose.bean.templates.dialog.beans"), 0, 0, null, null));
    JPanel jPanel3 = new JPanel();
    this.myLibsPanel = jPanel3;
    jPanel3.setLayout(new BorderLayout(0, 0));
    jPanel.add(jPanel3, new GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
  }

  public JComponent $$$getRootComponent$$$() {
    return this.myChoosePanel;
  }

  public ChooseTemplatesForm(List<cn.taketoday.assistant.model.actions.patterns.frameworks.ui.TemplateInfo> templates, LibrariesInfo libInfo) {
    this.myLibInfo = libInfo;
    $$$setupUI$$$();
    this.myTableViewPanel.setLayout(new GridLayout(templates.size(), 1));
    this.myTemplateInfos = templates;
    for (cn.taketoday.assistant.model.actions.patterns.frameworks.ui.TemplateInfo template : this.myTemplateInfos) {
      JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
      JCheckBox checkBox = new JCheckBox(template.getName());
      checkBox.setSelected(template.isAccepted());
      checkBox.addActionListener(e -> template.setAccepted(checkBox.isSelected()));
      checkBoxPanel.add(checkBox);
      if (template.getApiLink() != null || template.getReferenceLink() != null) {
        JBLabel jBLabel = new JBLabel("(", UIUtil.ComponentStyle.SMALL);
        int width = jBLabel.getFontMetrics(jBLabel.getFont()).stringWidth("(");
        int height = jBLabel.getFontMetrics(jBLabel.getFont()).getHeight();
        jBLabel.setPreferredSize(new Dimension(width, height));
        checkBoxPanel.add(jBLabel);
        if (template.getApiLink() != null) {
          checkBoxPanel.add(getLink(template.getApiLink(), JAVADOC));
          if (template.getReferenceLink() != null) {
            checkBoxPanel.add(new JBLabel(",", UIUtil.ComponentStyle.SMALL));
          }
        }
        checkBoxPanel.add(getLink(template.getReferenceLink(), DETAILS));
        checkBoxPanel.add(new JBLabel(")", UIUtil.ComponentStyle.SMALL));
      }
      JPanel comboPanelWrapper = new JPanel(new BorderLayout());
      comboPanelWrapper.add(checkBoxPanel, "West");
      this.myTableViewPanel.add(comboPanelWrapper);
      FacetEditorsFactory facetEditorsFactory = FacetEditorsFactory.getInstance();
      this.myLibrariesValidationComponent = facetEditorsFactory.createLibrariesValidationComponent(this.myLibInfo.getLibs(), this.myLibInfo.getModule(), this.myLibInfo.getName());
      this.myLibsPanel.add(this.myLibrariesValidationComponent.getComponent(), "Center");
      this.myLibrariesValidationComponent.addValidityListener(isValid -> {
      });
      this.myLibrariesValidationComponent.validate();
    }
  }

  private static JComponent getLink(String linkTarget, String linkText) {
    if (linkTarget == null) {
      return new JLabel();
    }
    BrowserLink hyperlinkLabel = new BrowserLink(linkText, linkTarget);
    hyperlinkLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
    return hyperlinkLabel;
  }

  public void dispose() {
  }

  public List<TemplateInfo> getTemplateInfos() {
    return this.myTemplateInfos;
  }

  public JComponent getComponent() {
    return this.myChoosePanel;
  }

  public LibrariesValidationComponent getLibrariesValidationComponent() {
    return this.myLibrariesValidationComponent;
  }
}
