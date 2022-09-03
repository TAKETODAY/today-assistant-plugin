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

package cn.taketoday.assistant.model.jam.javaConfig;

import com.intellij.codeInsight.highlighting.HighlightUsagesDescriptionLocation;
import com.intellij.ide.TypePresentationService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.PomDescriptionProvider;
import com.intellij.pom.PomTarget;
import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.usageView.UsageViewNodeTextLocation;
import com.intellij.usageView.UsageViewTypeLocation;

import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.pom.InfraBeanPomTargetUtils;

import static cn.taketoday.assistant.InfraBundle.message;

public class InfraPomTargetDescriptionProvider extends PomDescriptionProvider {

  public String getElementDescription(PomTarget element, ElementDescriptionLocation location) {
    CommonInfraBean infraBean = InfraBeanPomTargetUtils.getBean(element);
    if (infraBean == null) {
      return null;
    }
    if (location == UsageViewTypeLocation.INSTANCE) {
      return getSpringBeanTypeName(infraBean);
    }
    if (location == UsageViewNodeTextLocation.INSTANCE || location == HighlightUsagesDescriptionLocation.INSTANCE) {
      return getSpringBeanTypeName(infraBean) + " " + infraBean.getBeanName();
    }
    return null;
  }

  private static String getSpringBeanTypeName(CommonInfraBean infraBean) {
    return StringUtil.notNullize(TypePresentationService.getService().getTypeName(infraBean), message("infra.bean"));
  }
}
