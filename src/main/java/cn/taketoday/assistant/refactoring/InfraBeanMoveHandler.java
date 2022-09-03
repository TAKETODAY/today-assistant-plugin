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

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.xml.XmlFile;
import com.intellij.refactoring.move.MoveHandlerDelegate;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomService;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.InfraModelVisitor;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.Idref;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraRef;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolder;
import cn.taketoday.assistant.model.xml.beans.RefBase;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public class InfraBeanMoveHandler extends MoveHandlerDelegate {
  private static final Logger LOG = Logger.getInstance(InfraBeanMoveHandler.class);

  public boolean canMove(PsiElement[] elements, @Nullable PsiElement targetContainer, @Nullable PsiReference reference) {
    for (PsiElement element : elements) {
      if (BeanCoreUtils.findBeanByPsiElement(element) == null) {
        return false;
      }
    }
    return targetContainer == null || super.canMove(elements, targetContainer, reference);
  }

  public boolean isValidTarget(PsiElement psiElement, PsiElement[] sources) {
    DomInfraBean infraBean = BeanCoreUtils.findBeanByPsiElement(psiElement);
    return infraBean != null && ((infraBean.getParent() instanceof Beans) || (infraBean.getParent() instanceof InfraValueHolder));
  }

  public boolean tryToMove(PsiElement element, Project project, DataContext dataContext, @Nullable PsiReference reference, Editor editor) {
    DomInfraBean infraBean = BeanCoreUtils.findBeanByPsiElement(element);
    if (infraBean == null) {
      return false;
    }
    PsiFile psiFile = element.getContainingFile();
    if (infraBean.getParent() instanceof Beans) {
      List<DomFileElement<Beans>> elements = DomService.getInstance().getFileElements(Beans.class, project, new ProjectContentScope(project));
      List<XmlFile> files = new ArrayList<>(ContainerUtil.map(elements, DomFileElement::getFile));
      files.remove(psiFile);
      if (files.isEmpty()) {
        HintManager.getInstance().showErrorHint(editor, message("InfraBeanMoveHandler.no.other.files.found"));
        return true;
      }
      JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<>(message("InfraBeanMoveHandler.choose.file"), files) {

        public PopupStep onChosen(XmlFile selectedValue, boolean finalChoice) {
          doMove(selectedValue, infraBean, project);
          return FINAL_CHOICE;
        }

        public Icon getIconFor(XmlFile aValue) {
          return aValue.getIcon(0);
        }

        public String getTextFor(XmlFile value) {
          return value.getName();
        }
      }).showInBestPositionFor(editor);
      return true;
    }
    else if (ApplicationManager.getApplication().isUnitTestMode() || Messages.showYesNoDialog(project, message("do.you.want.to.move.bean.to.the.top.level"),
            message("move.bean.to.the.top.level"), Messages.getQuestionIcon()) == Messages.YES) {
      InfraIntroduceBeanIntention.moveToTheTopLevel(project, editor, infraBean, psiFile);
      return true;
    }
    else {
      return true;
    }
  }

  public static void doMove(XmlFile file, DomInfraBean infraBean, Project project) {
    PsiFile psiFile = infraBean.getXmlTag().getContainingFile();
    WriteCommandAction.writeCommandAction(project, psiFile, file).withName(message("move.bean")).run(() -> {
      DomFileElement<Beans> fileElement = InfraDomUtils.getDomFileElement(file);
      assert fileElement != null;
      Beans beans = fileElement.getRootElement();
      InfraBean bean = beans.addBean();
      InfraModelVisitor.visitBean(new InfraModelVisitor() {
        @Override
        public boolean visitRef(InfraRef ref) {
          visitRefBase(ref);
          return super.visitRef(ref);
        }

        @Override
        public boolean visitIdref(Idref idref) {
          visitRefBase(idref);
          return super.visitIdref(idref);
        }

        private void visitRefBase(RefBase refBase) {
          String local = refBase.getLocal().getStringValue();
          if (local != null) {
            refBase.getBean().setStringValue(local);
            refBase.getLocal().undefine();
          }
        }
      }, infraBean);
      bean.copyFrom(infraBean);
      BeanPointer<?> pointer = InfraBeanService.of().createBeanPointer(infraBean);
      PsiElement element = pointer.getPsiElement();
      assert element != null;
      for (PsiReference psiReference : ReferencesSearch.search(element)) {
        try {
          psiReference.bindToElement(bean.getXmlTag());
        }
        catch (IncorrectOperationException e) {
          LOG.error("Can't bind " + psiReference + " to " + bean, e);
        }
      }
      infraBean.undefine();
    });
  }
}
