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
package cn.taketoday.assistant.model.config.autoconfigure;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import cn.taketoday.assistant.model.jam.InfraOrder;

/**
 * @author Konstantin Aleev
 */
public class InfraOrderClassSorter {

  public static final Comparator<PsiClass> CLASS_NAME_COMPARATOR =
          (o1, o2) -> StringUtil.compare(o1.getQualifiedName(), o2.getQualifiedName(), false);

  private static final Comparator<PsiClass> ORDER_COMPARATOR = new Comparator<>() {

    @Override
    public int compare(PsiClass o1, PsiClass o2) {
      int order1 = getOrderValue(o1);
      int order2 = getOrderValue(o2);
      return Comparing.compare(order1, order2);
    }

    private int getOrderValue(PsiClass psiClass) {
      InfraOrder order = InfraOrder.CLASS_META.getJamElement(psiClass);
      if (order == null)
        return InfraOrder.LOWEST_PRECEDENCE;
      return ObjectUtils.notNull(order.getValue(), InfraOrder.LOWEST_PRECEDENCE);
    }
  };

  private final List<? extends PsiClass> allConfigs;

  public InfraOrderClassSorter(List<? extends PsiClass> allConfigs) {
    this.allConfigs = allConfigs;
  }

  public List<PsiClass> getSortedConfigs() {
    // sort by FQN
    List<PsiClass> configs = new ArrayList<>(allConfigs);
    configs.sort(CLASS_NAME_COMPARATOR);

    // sort by @Order
    configs.sort(ORDER_COMPARATOR);

    return configs;
  }
}
