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
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.jsp.JspFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.CollectionElements;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraRef;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolder;

public class InfraIntroduceBeanIntention implements IntentionAction {
  private static final Logger LOG = Logger.getInstance(InfraIntroduceBeanIntention.class);

  public String getText() {
    return InfraBundle.message("introduce.bean.intention");
  }

  public String getFamilyName() {
    return getText();
  }

  public boolean isAvailable(Project project, Editor editor, PsiFile file) {
    if (!(file instanceof XmlFile) || (file instanceof JspFile) || !InfraDomUtils.isInfraXml((XmlFile) file)) {
      return false;
    }
    DomInfraBean bean = BeanCoreUtils.getBeanForCurrentCaretPosition(editor, file);
    return (bean instanceof InfraBean) && (bean.getParent() instanceof InfraValueHolder);
  }

  public void invoke(Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    DomInfraBean bean = BeanCoreUtils.getBeanForCurrentCaretPosition(editor, file);
    moveToTheTopLevel(project, editor, bean);
  }

  public static void moveToTheTopLevel(Project project, Editor editor, DomInfraBean infraBean) {
    if (infraBean == null) {
      return;
    }
    DomInfraBean topLevelBean = BeanCoreUtils.getTopLevelBean(infraBean);
    DomInfraBean newBean = DomUtil.addElementAfter(topLevelBean);
    newBean.copyFrom(infraBean);
    String id = newBean.getId().getValue();
    if (id == null) {
      try {
        XmlAttribute attribute = XmlElementFactory.getInstance(project).createXmlAttribute("id", "");
        XmlTag tag = newBean.getXmlTag();
        PsiElement[] attributes = tag.getAttributes();
        if (attributes.length > 0) {
          tag.addBefore(attribute, attributes[0]);
        }
        else {
          tag.add(attribute);
        }
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }
    DomElement holder = infraBean.getParent();
    if (holder instanceof InfraValueHolder) {
      ((InfraValueHolder) holder).getRefAttr().setStringValue(id == null ? "" : id);
    }
    else if (holder instanceof CollectionElements) {
      InfraRef ref = ((CollectionElements) holder).addRef();
      ref.getBean().setStringValue(id == null ? "" : id);
    }
    else {
      LOG.error("Unexpected parent type: " + holder);
      return;
    }
    infraBean.undefine();
    holder.getXmlTag().collapseIfEmpty();
    if (id != null) {
      return;
    }
    InfraBean topLevelBeanCopy = topLevelBean.createStableCopy();
    InfraBean newBeanCopy = newBean.createStableCopy();
    InfraValueHolder holderCopy = holder.createStableCopy();
    Document document = editor.getDocument();
    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document);
    int start = topLevelBeanCopy.getXmlTag().getTextOffset();
    int end = newBeanCopy.getXmlTag().getTextRange().getEndOffset();
    TemplateManager templateManager = TemplateManager.getInstance(project);
    Template template = templateManager.createTemplate("", "");
    template.setToReformat(true);
    String text = document.getText();
    int refOffset = holderCopy.getRefAttr().getXmlAttributeValue().getTextOffset();
    int idOffset = newBeanCopy.getId().getXmlAttributeValue().getTextOffset();
    template.addTextSegment(text.substring(start, refOffset));
    String[] names = BeanCoreUtils.suggestBeanNames(newBean);
    ConstantNode withLookupStrings = new ConstantNode((Result) null).withLookupStrings(names);
    template.addVariable("id", withLookupStrings, withLookupStrings, true);
    template.addTextSegment(text.substring(refOffset, idOffset));
    template.addVariableSegment("id");
    template.addTextSegment(text.substring(idOffset, end));
    document.deleteString(start, end);
    templateManager.startTemplate(editor, template);
  }

  public boolean startInWriteAction() {
    return true;
  }

  public static void moveToTheTopLevel(Project project, Editor editor, DomInfraBean infraBean, PsiFile psiFile) {
    WriteCommandAction.writeCommandAction(project, psiFile).withName(InfraBundle.message("move.bean.to.the.top.level")).run(() -> {
      moveToTheTopLevel(project, editor, infraBean);
    });
  }
}
