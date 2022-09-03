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

package cn.taketoday.assistant.refactoring;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.jsp.JspFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlChildRole;
import com.intellij.psi.xml.XmlDoctype;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlProlog;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.DomFileElement;

import java.util.Objects;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraModelVisitor;
import cn.taketoday.assistant.model.scope.BeanScope;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;

public final class InfraUpdateSchemaIntention implements IntentionAction {
  private static final String BEANS = """
          beans xmlns="http://www.springframework.org/schema/beans"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd"
          """;

  public String getText() {
    return InfraBundle.message("update.schema.intention");
  }

  public String getFamilyName() {
    return getText();
  }

  public boolean isAvailable(Project project, Editor editor, PsiFile file) {
    if ((file instanceof XmlFile) && !(file instanceof JspFile) && InfraDomUtils.isInfraXml((XmlFile) file)) {
      int offset = editor.getCaretModel().getOffset();
      PsiElement psiElement = file.findElementAt(offset);
      if (PsiTreeUtil.getParentOfType(psiElement, XmlProlog.class) != null || PsiTreeUtil.getParentOfType(psiElement, XmlDoctype.class) != null) {
        return isUpdateNeeded((XmlFile) file);
      }
      XmlTag tag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
      if (tag != null && tag.getParentTag() == null) {
        return isUpdateNeeded((XmlFile) file);
      }
      return false;
    }
    return false;
  }

  public void invoke(Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    updateSchema((XmlFile) file);
  }

  public static boolean requestSchemaUpdate(XmlFile file) throws IncorrectOperationException {
    if (!isUpdateNeeded(file)) {
      return true;
    }
    boolean unitTestMode = ApplicationManager.getApplication().isUnitTestMode();
    if (!unitTestMode && !MessageDialogBuilder.yesNo(InfraBundle.message("xml.schema.update.is.required"),
                    InfraBundle.message("xml.schema.will.be.updated"))
            .ask(file.getProject())) {
      return false;
    }
    boolean success = updateSchema(file);
    if (!success && !unitTestMode) {
      Messages.showErrorDialog(file.getProject(), InfraBundle.message("UpdateSchemaIntention.schema.update.failed.for.invalid.file", file.getName()),
              InfraBundle.message("UpdateSchemaIntention.schema.update"));
    }
    return success;
  }

  private static boolean updateSchema(XmlFile file) throws IncorrectOperationException {
    DomFileElement<Beans> element;
    XmlTag tag;
    ASTNode node;
    ASTNode child;
    Project project = file.getProject();
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    Document doc = documentManager.getDocument(file);
    if (doc == null || (element = InfraDomUtils.getDomFileElement(
            file)) == null || (tag = element.getRootTag()) == null || (node = tag.getNode()) == null || (child = XmlChildRole.START_TAG_NAME_FINDER.findChild(node)) == null) {
      return false;
    }
    TextRange range = child.getTextRange();
    doc.replaceString(range.getStartOffset(), range.getEndOffset(), BEANS);
    documentManager.commitDocument(doc);
    CodeStyleManager.getInstance(project).reformatRange(tag, range.getStartOffset(), range.getStartOffset() + BEANS.length());
    InfraModelVisitor.visitBeans(new InfraModelVisitor() {
      @Override
      public boolean visitBean(CommonInfraBean bean) {
        Boolean value;
        if ((bean instanceof InfraBean) && (value = ((InfraBean) bean).getSingleton().getValue()) != null) {
          ((InfraBean) bean).getSingleton().undefine();
          ((InfraBean) bean).getScope().setValue(value ? BeanScope.SINGLETON_SCOPE : BeanScope.PROTOTYPE_SCOPE);
          return true;
        }
        return true;
      }
    }, element.getRootElement());
    XmlProlog prolog = Objects.requireNonNull(file.getDocument()).getProlog();
    XmlDoctype doctype = prolog == null ? null : prolog.getDoctype();
    if (doctype != null) {
      doctype.delete();
      return true;
    }
    return true;
  }

  public boolean startInWriteAction() {
    return true;
  }

  private static boolean isUpdateNeeded(XmlFile config) {
    XmlDocument document = config.getDocument();
    XmlTag tag = document.getRootTag();
    return !tag.getNamespace().equals(InfraConstant.BEANS_XSD);
  }
}
