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

package cn.taketoday.assistant.model.highlighting.jam;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.jam.javaConfig.InfraJavaExternalBean;
import cn.taketoday.assistant.model.jam.javaConfig.InfraOldJavaConfigurationUtil;

public class JavaBeanReferencesFindUsagesHandler extends FindUsagesHandler {
  private final CommonInfraBean infraBean;
  private final BeanPointer<?> pointer;

  public JavaBeanReferencesFindUsagesHandler(CommonInfraBean springBean) {
    super(springBean.getIdentifyingPsiElement());
    this.infraBean = springBean;
    this.pointer = InfraBeanService.of().createBeanPointer(this.infraBean);
  }

  public PsiElement[] getSecondaryElements() {
    List<InfraJavaExternalBean> list = InfraOldJavaConfigurationUtil.findExternalBeanReferences(this.infraBean);
    Set<PsiElement> psiElements = new HashSet<>();
    for (InfraJavaExternalBean externalBean : list) {
      PsiMethod method = externalBean.getPsiElement();
      psiElements.add(method);
    }
    return PsiUtilCore.toPsiElementArray(psiElements);
  }

  public boolean processElementUsages(PsiElement element, Processor<? super UsageInfo> processor, FindUsagesOptions options) {
    return super.processElementUsages(element, processor, options)
            && AutowiredBeanFindUsagesHandler.processAutowiredBeans(element, processor, options, this.pointer);
  }
}
