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
package cn.taketoday.assistant.model;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiType;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.model.jam.JamBeanPointerImpl;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.xml.BeanDomPointerImpl;
import cn.taketoday.assistant.model.xml.CustomBean;
import cn.taketoday.assistant.model.xml.CustomBeanPointer;
import cn.taketoday.assistant.model.xml.DomBeanPointerImpl;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraBean;

/**
 * @author Yann C&eacute;bron
 */
public class InfraBeanService {
  private final Function<CommonInfraBean, BeanPointer<?>> myBeanToPointer = this::createBeanPointer;

  public static InfraBeanService of() {
    return ApplicationManager.getApplication().getService(InfraBeanService.class);
  }

  /**
   * @return bean effective types
   * 1. if bean is factory bean: bean type AND product type will be collected
   * 2. if factory product proxies multiple interfaces all multiple values(types) will be collected
   * @see BeanEffectiveTypeProvider
   * @see BeanPointer#getEffectiveBeanTypes()
   */
  public PsiType[] getEffectiveBeanTypes(CommonInfraBean bean) {
    Set<PsiType> effectiveTypes = new LinkedHashSet<>(1);
    ContainerUtil.addIfNotNull(effectiveTypes, bean.getBeanType());
    var collectProcessor = new CommonProcessors.CollectProcessor<PsiType>();
    for (BeanEffectiveTypeProvider provider : BeanEffectiveTypeProvider.array()) {
      if (!provider.processEffectiveTypes(bean, collectProcessor)) {
        break;
      }
    }
    ContainerUtil.addAllNotNull(effectiveTypes, collectProcessor.getResults());
    return effectiveTypes.toArray(PsiType.EMPTY_ARRAY);
  }

  public BeanPointer<?> createBeanPointer(CommonInfraBean infraBean) {
    if (infraBean instanceof DomInfraBean) {
      if (infraBean instanceof InfraBean) {
        return new BeanDomPointerImpl((InfraBean) infraBean);
      }
      return new DomBeanPointerImpl((DomInfraBean) infraBean);
    }
    else if (infraBean instanceof JamPsiMemberInfraBean) {
      return new JamBeanPointerImpl((JamPsiMemberInfraBean) infraBean);
    }
    else {
      if (infraBean instanceof CustomBean) {
        return CustomBeanPointer.createCustomBeanPointer((CustomBean) infraBean);
      }
      else if (infraBean instanceof InfraImplicitBeanWithDefinition) {
        return new SimpleBeanPointer<>(infraBean);
      }
      else {
        throw new AssertionError("Unknown bean type: " + infraBean);
      }
    }
  }

  /**
   * @param infraBean only {@link DomInfraBean}
   * @return target instance
   */
  public BeanPsiTarget createBeanPsiTarget(CommonInfraBean infraBean) {
    if (infraBean instanceof DomInfraBean) {
      return DomBeanPointerImpl.createBeanPsiTarget((DomInfraBean) infraBean);
    }
    throw new IllegalArgumentException("Unsupported bean type: " + infraBean);
  }

  /**
   * @see BeanPointer#TO_BEAN
   */
  public Set<BeanPointer<?>> mapBeans(Collection<? extends CommonInfraBean> beans) {
    if (beans.isEmpty()) {
      return Collections.emptySet();
    }
    Set<BeanPointer<?>> set = new LinkedHashSet<>(beans.size());
    for (CommonInfraBean bean : beans) {
      if (bean.isValid()) {
        set.add(myBeanToPointer.fun(bean));
      }
    }
    return set;
  }
}
