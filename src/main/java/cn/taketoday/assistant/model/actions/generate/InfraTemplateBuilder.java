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

package cn.taketoday.assistant.model.actions.generate;

import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.MacroCallNode;
import com.intellij.codeInsight.template.macro.MacroFactory;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomElement;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;

public class InfraTemplateBuilder {
  private final Template template;
  private final Project project;
  private int count;
  private static final Set<String> convertableTypes = Set.of(
          "java.lang.String", "java.lang.Boolean", "java.lang.Character", "java.lang.Byte",
          "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float",
          "java.lang.Double", "java.math.BigDecimal", "java.math.BigInteger",
          "java.lang.Class", InfraConstant.IO_RESOURCE, "java.net.URL",
          "java.io.File", "java.io.InputStream", "java.util.Locale", "java.util.Properties"
  );

  public InfraTemplateBuilder(Project project) {
    this.project = project;
    this.template = TemplateManager.getInstance(project).createTemplate("", "");
    this.template.setToReformat(true);
  }

  private boolean createValue(PsiType type, CommonInfraModel model) {
    if (type instanceof PsiClassType psiClassType) {
      if ("java.lang.Object".equals(type.getCanonicalText())) {
        createAttr("value");
        return false;
      }
      if (InheritanceUtil.isInheritor(psiClassType, "java.util.Properties")) {
        createProperties();
        return true;
      }
      else if (InheritanceUtil.isInheritor(psiClassType, "java.util.Map")) {
        createMap();
        return true;
      }
      else if (InheritanceUtil.isInheritor(psiClassType, "java.util.Set")) {
        createCollection("set");
        return true;
      }
      else if (InheritanceUtil.isInheritor(psiClassType, "java.util.Collection")) {
        createCollection("list");
        return true;
      }
      else {
        createAttr(psiClassType, model, false);
        return false;
      }
    }
    else if (type instanceof PsiArrayType) {
      createCollection("list");
      return true;
    }
    else {
      createAttr("value");
      return false;
    }
  }

  public void createValueAndClose(PsiType type, CommonInfraModel model, String tagName) {
    boolean closingTagNeeded = createValue(type, model);
    if (closingTagNeeded) {
      addTextSegment("</");
      addVariableSegment("NS_PREFIX");
      addTextSegment(tagName + ">");
      return;
    }
    addTextSegment("/>");
  }

  private void createAttr(PsiClassType type, CommonInfraModel model, boolean key) {
    boolean canBeReferenced = !BeanCoreUtils.getBeansByType(type, model).isEmpty();
    if (canBeReferenced || !isConvertable(type)) {
      if (key) {
        createAttr("key-ref");
      }
      else {
        createAttr("ref", true);
      }
      return;
    }
    createAttr(key ? "key-value" : "value");
  }

  private static boolean isConvertable(PsiType type) {
    return convertableTypes.contains(type.getCanonicalText());
  }

  private void createMap() {
    addTextSegment("><");
    addVariableSegment("NS_PREFIX");
    addTextSegment("map>\n<");
    addVariableSegment("NS_PREFIX");
    addTextSegment("entry");
    createAttr("key");
    createAttr("value");
    addTextSegment("/>\n</");
    addVariableSegment("NS_PREFIX");
    addTextSegment("map>\n");
  }

  private void createProperties() {
    addTextSegment(">\n<");
    addVariableSegment("NS_PREFIX");
    addTextSegment("props>\n<");
    addVariableSegment("NS_PREFIX");
    addTextSegment("prop key=\"");
    MacroCallNode node = new MacroCallNode(MacroFactory.createMacro("complete"));
    this.template.addVariable("PROP_KEY", node, node, true);
    addTextSegment("\">");
    this.template.addVariable("PROP_VALUE", node, node, true);
    addTextSegment("</");
    addVariableSegment("NS_PREFIX");
    addTextSegment("prop>\n</");
    addVariableSegment("NS_PREFIX");
    addTextSegment("props>");
  }

  public void createCollection(String name) {
    addTextSegment(">\n<");
    addVariableSegment("NS_PREFIX");
    addTextSegment(name + ">\n");
    addTextSegment("<");
    addVariableSegment("NS_PREFIX");
    addTextSegment("value>");
    MacroCallNode node = new MacroCallNode(MacroFactory.createMacro("complete"));
    Template template = this.template;
    int i = this.count;
    this.count = i + 1;
    template.addVariable(name + i, node, node, true);
    addTextSegment("</");
    addVariableSegment("NS_PREFIX");
    addTextSegment("value>\n");
    addTextSegment("</");
    addVariableSegment("NS_PREFIX");
    addTextSegment(name + ">\n");
  }

  private void createAttr(String name) {
    createAttr(name, false);
  }

  private void createAttr(String name, boolean isSmartCompletion) {
    addTextSegment(" " + name + "=\"");
    MacroCallNode node = new MacroCallNode(MacroFactory.createMacro(isSmartCompletion ? "completeSmart" : "complete"));
    int i = this.count;
    this.count = i + 1;
    this.template.addVariable(name + i, node, node, true);
    addTextSegment("\"");
  }

  public void addTextSegment(String s) {
    this.template.addTextSegment(s);
  }

  public void addVariableSegment(String s) {
    this.template.addVariableSegment(s);
  }

  public void startTemplate(Editor editor) {
    startTemplate(editor, getPredefinedMap());
  }

  protected HashMap<String, String> getPredefinedMap() {
    return new HashMap<>();
  }

  public void startTemplate(Editor editor, Map<String, String> predefinedVarValues) {
    Map<String, String> vars = new HashMap<>();
    vars.put("NS_PREFIX", "beans:");
    TemplateManager.getInstance(this.project).startTemplate(editor, this.template, true, predefinedVarValues, null);
  }

  public static void preparePlace(Editor editor, Project project, DomElement element) {
    DomElement copy = element.createStableCopy();
    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
    XmlTag tag = copy.getXmlTag();
    assert tag != null;
    int offset = tag.getTextOffset();
    editor.getDocument().deleteString(offset, tag.getTextRange().getEndOffset());
    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
    editor.getCaretModel().moveToOffset(offset);
  }

  public static Editor getEditor(ProblemDescriptor descriptor) {
    PsiFile psiFile = descriptor.getPsiElement().getContainingFile();
    Project project = psiFile.getProject();
    VirtualFile virtualFile = psiFile.getVirtualFile();
    return FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, virtualFile, 0), false);
  }
}
