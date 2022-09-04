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

package cn.taketoday.assistant.model.xml.custom;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.idea.ActionsBundle;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.unscramble.UnscrambleDialog;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.DomFileDescription;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.DomUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.dom.InfraCustomNamespaces;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.assistant.model.xml.DomInfraBean;

import static cn.taketoday.assistant.InfraBundle.message;

public class ParseCustomBeanIntention implements IntentionAction {

  public String getText() {
    return message("parse.custom.bean.intention");
  }

  public String getFamilyName() {
    String text = getText();
    return text;
  }

  public boolean isAvailable(Project project, Editor editor, PsiFile file) {
    DomInfraBean domElement = DomUtil.findDomElement(file.findElementAt(editor.getCaretModel().getOffset()), DomInfraBean.class);
    if (domElement instanceof CustomBeanWrapper) {
      return isUnregisteredNamespace((CustomBeanWrapper) domElement);
    }
    return false;
  }

  public static boolean isUnregisteredNamespace(CustomBeanWrapper wrapper) {
    String tagNamespace = wrapper.getXmlElementNamespace();
    for (InfraCustomNamespaces namespaces : InfraCustomNamespaces.EP_NAME.getExtensions()) {
      InfraCustomNamespaces.NamespacePolicies policies = namespaces.getNamespacePolicies();
      if (policies.isRegistered(tagNamespace)) {
        return false;
      }
    }
    PsiFile file = wrapper.getContainingFile();
    DomFileDescription<?> description = DomManager.getDomManager(wrapper.getManager().getProject()).getDomFileDescription((XmlFile) file);
    if (description == null) {
      return true;
    }
    List<String> namespaces2 = description.getAllowedNamespaces(tagNamespace, (XmlFile) file);
    return namespaces2.isEmpty();
  }

  public void invoke(Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    CustomBeanWrapper wrapper = DomUtil.findDomElement(file.findElementAt(editor.getCaretModel().getOffset()), CustomBeanWrapper.class);
    invokeCustomBeanParsers(project, Collections.singletonList(wrapper.getXmlTag()));
  }

  public static void invokeCustomBeanParsers(Project project, Collection<XmlTag> tags) {
    String message;
    String idAttr;
    Ref<CustomBeanRegistry.ParseResult> ref = Ref.create(null);
    ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
      ApplicationManager.getApplication().runReadAction(() -> {
        ref.set(CustomBeanRegistry.getInstance(project).parseBeans(tags));
      });
    }, message("parsing.custom.bean"), false, project);
    ApplicationManager.getApplication().runWriteAction(() -> {
      PsiManager.getInstance(project).dropPsiCaches();
      InfraModificationTrackersManager.from(project).fireCustomBeanParserChanged();
    });
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }
    CustomBeanRegistry.ParseResult result = ref.get();
    String message2 = result.getErrorMessage();
    if (message2 != null) {
      NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup("Infra custom beans");
      group.createNotification(message2, NotificationType.ERROR).notify(project);
      return;
    }
    String trace = result.getStackTrace();
    if (trace != null) {
      String shortTrace = trace;
      int i = 0;
      for (int j = 0; j < 10; j++) {
        if (i >= 0) {
          i = trace.indexOf(10, i + 1);
        }
      }
      if (i >= 0) {
        shortTrace = trace.substring(0, i) + "\n\t...";
      }
      int exitCode = Messages.showOkCancelDialog(project, shortTrace, message("parse.custom.bean.error"), CommonBundle.getOkButtonText(),
              ActionsBundle.message("action.Unscramble.text"), Messages.getErrorIcon());
      if (exitCode != 0) {
        UnscrambleDialog dialog = new UnscrambleDialog(project);
        dialog.setText(trace);
        dialog.show();
        return;
      }
      return;
    }
    List<CustomBeanInfo> infos = result.getBeans();
    assert infos != null;
    String beansText = StringUtil.join(infos,
            customBeanInfo -> "  id = " + customBeanInfo.beanName + "; class = " + customBeanInfo.beanClassName, "\n");
    if (infos.size() == 1 && tags.size() == 1 && (idAttr = infos.get(0).idAttribute) != null) {
      XmlTag tag = tags.iterator().next();
      String ns = tag.getNamespace();
      String localName = tag.getLocalName();
      String inductMessage = message("parse.these.beans.induct", beansText, ns, localName, idAttr);
      if (Messages.showOkCancelDialog(project, inductMessage, message("parse.custom.bean.success"),
              message("parse.these.beans.induct.only.this"), message("parse.these.beans.induct.all.beans"),
              Messages.getInformationIcon()) != Messages.OK) {
        cn.taketoday.assistant.model.xml.custom.CustomBeanInfo beanInfo = new CustomBeanInfo(infos.get(0));
        beanInfo.beanName = null;
        CustomBeanRegistry.getInstance(project).addBeanPolicy(ns, localName, beanInfo);
        return;
      }
      return;
    }
    if (!infos.isEmpty()) {
      message = message("parse.these.beans", beansText);
    }
    else if (result.hasInfrastructureBeans()) {
      message = message("parse.only.infrastructure.beans");
    }
    else {
      message = message("parse.no.custom.beans");
    }
    String parsedMessage = message;
    Messages.showInfoMessage(project, parsedMessage, message("parse.custom.bean.success"));
  }

  public boolean startInWriteAction() {
    return false;
  }
}
