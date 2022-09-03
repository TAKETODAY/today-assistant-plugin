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

package cn.taketoday.assistant.app.run.lifecycle.beans.gutter;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.intellij.xml.util.XmlTagUtil;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.run.InfraRunIcons;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBean;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveResource;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.xml.beans.InfraBean;

final class LiveBeansDomLineMarkerProvider extends LineMarkerProviderDescriptor {

  public String getId() {
    return "LiveBeansDomLineMarkerProvider";
  }

  public String getName() {
    return InfraAppBundle.message("runtime.beans.xml.gutter.icon.name");
  }

  public Icon getIcon() {
    return InfraRunIcons.Gutter.LiveBean;
  }

  public LineMarkerInfo<?> getLineMarkerInfo(PsiElement element) {
    return null;
  }

  public void collectSlowLineMarkers(List<? extends PsiElement> elements, Collection<? super LineMarkerInfo<?>> result) {
    PsiElement psiElement = ContainerUtil.getFirstItem(elements);
    if (psiElement == null) {
      return;
    }
    PsiFile file = elements.get(0).getContainingFile();
    if (!isSpringXml(file) || !cn.taketoday.assistant.app.run.lifecycle.beans.gutter.LiveBeansNavigationHandler.hasLiveBeansModels(psiElement.getProject())) {
      return;
    }
    for (PsiElement element : elements) {
      annotate(element, result);
    }
  }

  private static void annotate(PsiElement psiElement, Collection<? super LineMarkerInfo<?>> result) {
    String beanName;
    if (!(psiElement instanceof XmlToken)) {
      return;
    }
    PsiElement parent = psiElement.getParent();
    if (!(parent instanceof XmlTag tag)) {
      return;
    }
    if (XmlTagUtil.getStartTagNameElement(tag) != psiElement) {
      return;
    }
    DomElement domElement = DomManager.getDomManager(psiElement.getProject()).getDomElement(tag);
    if (!(domElement instanceof InfraBean bean) || (beanName = bean.getBeanName()) == null) {
      return;
    }
    Predicate<LiveBean> beanMatcher = liveBean -> {
      LiveResource liveResource = liveBean.getResource();
      PsiFile containingResource = psiElement.getContainingFile();
      return liveResource != null && liveResource.matchesXmlConfig((XmlFile) containingResource);
    };
    LiveBeansNavigationHandler.addLiveBeansGutterIcon(beanName, beanMatcher, psiElement.getProject(), psiElement, result);
  }

  private static boolean isSpringXml(PsiFile file) {
    if (!(file instanceof XmlFile xmlFile)) {
      return false;
    }
    return InfraDomUtils.isInfraXml(xmlFile);
  }
}
