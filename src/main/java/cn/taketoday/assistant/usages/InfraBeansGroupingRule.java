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

package cn.taketoday.assistant.usages;

import com.intellij.navigation.NavigationItemFileStatus;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.UsageView;
import com.intellij.usages.rules.PsiElementUsage;
import com.intellij.usages.rules.SingleParentUsageGroupingRule;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.lang.Nullable;

public class InfraBeansGroupingRule extends SingleParentUsageGroupingRule {
  private static final Logger LOG = Logger.getInstance(InfraBeansGroupingRule.class);

  @Nullable
  protected UsageGroup getParentGroupFor(Usage usage, UsageTarget[] targets) {
    DomElement domElement;
    DomInfraBean infraBean;
    if (usage instanceof PsiElementUsage) {
      PsiElement psiElement = ((PsiElementUsage) usage).getElement();
      PsiFile containingFile = psiElement.getContainingFile();
      if ((containingFile instanceof XmlFile xmlFile) && InfraDomUtils.isInfraXml(xmlFile) && (domElement = DomUtil.getDomElement(
              psiElement)) != null && (infraBean = domElement.getParentOfType(DomInfraBean.class, false)) != null) {
        return new BeansUsageGroup(infraBean);
      }
      return null;
    }
    return null;
  }

  private static class BeansUsageGroup implements UsageGroup, DataProvider {
    private final String myName;
    private final DomInfraBean myBean;

    BeansUsageGroup(DomInfraBean bean) {
      this.myBean = bean;
      String beanName = bean.getPresentation().getElementName();
      this.myName = beanName == null ? InfraBundle.message("bean.with.unknown.name") : beanName;
      update();
    }

    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      BeansUsageGroup that = (BeansUsageGroup) o;
      return this.myName.equals(that.myName) && isValid() && that.isValid() && this.myBean.equals(that.myBean);
    }

    public int hashCode() {
      int result = this.myName.hashCode();
      return (31 * result) + this.myBean.hashCode();
    }

    public Icon getIcon() {
      if (isValid()) {
        return InfraPresentationProvider.getInfraIcon(this.myBean);
      }
      return null;
    }

    public String getPresentableGroupText() {
      return myName;
    }

    public DomInfraBean getBean() {
      return this.myBean;
    }

    public FileStatus getFileStatus() {
      if (isValid()) {
        return NavigationItemFileStatus.get(DomUtil.getFile(myBean));
      }
      return null;
    }

    public boolean isValid() {
      return myBean.isValid();
    }

    public void navigate(boolean focus) throws UnsupportedOperationException {
      if (canNavigate()) {
        InfraDomUtils.navigate(this.myBean);
      }
    }

    public boolean canNavigate() {
      return isValid();
    }

    public boolean canNavigateToSource() {
      return canNavigate();
    }

    public int compareTo(UsageGroup usageGroup) {
      if (!(usageGroup instanceof BeansUsageGroup)) {
        LOG.error("SpringBeansUsageGroup expected but " + usageGroup.getClass() + " found");
        return 0;
      }
      return this.myName.compareToIgnoreCase(((BeansUsageGroup) usageGroup).myName);
    }

    @Nullable
    public Object getData(String dataId) {
      XmlElement psiElement;
      if (!isValid()) {
        return null;
      }
      if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
        XmlElement element = getPsiElement();
        if (element != null && element.isValid()) {
          return element;
        }
        return null;
      }
      else if (UsageView.USAGE_INFO_KEY.is(dataId) && (psiElement = getPsiElement()) != null && psiElement.isValid()) {
        return new UsageInfo(psiElement);
      }
      else {
        return null;
      }
    }

    @Nullable
    private XmlElement getPsiElement() {
      return myBean.getXmlElement();
    }
  }
}
