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

package cn.taketoday.assistant.model.utils;

import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiTarget;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.index.InfraXmlBeansIndex;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.BeanPsiTarget;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.custom.CustomModuleComponentsDiscoverer;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.utils.search.BeanSearchParameters;
import cn.taketoday.assistant.model.xml.CustomBean;
import cn.taketoday.assistant.model.xml.CustomBeanPsiElement;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.assistant.model.xml.DomBeanContainer;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

public class InfraBeanUtilsImpl extends InfraBeanUtils {

  @Override
  @Nullable
  public CommonInfraBean findBean(@Nullable PomTarget target) {
    if (target instanceof BeanPsiTarget) {
      return ((BeanPsiTarget) target).getInfraBean();
    }
    if (target instanceof PsiTarget) {
      return findBean(PomService.convertToPsi((PsiTarget) target));
    }
    return null;
  }

  @Override
  @Nullable
  public CommonInfraBean findBean(@Nullable PsiElement identifyingPsiElement) {
    if (identifyingPsiElement == null) {
      return null;
    }
    if (identifyingPsiElement instanceof CustomBeanPsiElement) {
      return ((CustomBeanPsiElement) identifyingPsiElement).getBean();
    }
    if (identifyingPsiElement instanceof PomTargetPsiElement pomTargetPsiElement) {
      PomTarget target = pomTargetPsiElement.getTarget();
      if (target instanceof BeanPsiTarget beanPsiTarget) {
        return beanPsiTarget.getInfraBean();
      }
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(identifyingPsiElement);
    if (module == null || module.isDisposed() || !InfraLibraryUtil.hasLibrary(module)) {
      return null;
    }
    JamPsiMemberInfraBean bean = JamService.getJamService(identifyingPsiElement.getProject())
            .getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, identifyingPsiElement);
    if (bean != null) {
      return bean;
    }
    for (BeanPointer<?> pointer : CustomModuleComponentsDiscoverer.getCustomBeansModel(module).getAllCommonBeans()) {
      if (pointer.isValid() && identifyingPsiElement.equals(pointer.getBean().getIdentifyingPsiElement())) {
        return pointer.getBean();
      }
    }
    return null;
  }

  @Override
  @Nullable
  public BeanPointer<?> findBean(CommonInfraModel model, String beanName) {
    String beanReferenceName = beanName.startsWith("&") ? beanName.substring(1) : beanName;
    return InfraModelSearchers.findBean(model, beanReferenceName);
  }

  @Override
  public Set<String> findBeanNames(CommonInfraBean bean) {
    String beanName = bean.getBeanName();
    if (beanName == null) {
      return Collections.emptySet();
    }
    else if (bean instanceof InfraBean) {
      CommonInfraModel infraModel = InfraModelService.of().getModelByBean(bean);
      return InfraModelVisitorUtils.getAllBeanNames(infraModel, InfraBeanService.of().createBeanPointer(bean));
    }
    else {
      Set<String> names = new HashSet<>();
      names.add(beanName);
      for (String s : bean.getAliases()) {
        if (StringUtil.isNotEmpty(s)) {
          names.add(s);
        }
      }
      return names;
    }
  }

  @Override
  public boolean processChildBeans(DomElement parent, boolean includeParsedCustomBeanWrappers, Processor<CommonInfraBean> processor) {
    for (DomInfraBean bean : DomUtil.getChildrenOf(parent, DomInfraBean.class)) {
      if (bean instanceof CustomBeanWrapper) {
        if ((includeParsedCustomBeanWrappers || !((CustomBeanWrapper) bean).isParsed()) && !processor.process(bean)) {
          return false;
        }
        List<CustomBean> customBeans = ((CustomBeanWrapper) bean).getCustomBeans();
        for (CustomBean customBean : customBeans) {
          if (!processor.process(customBean)) {
            return false;
          }
        }
      }
      else if (!processor.process(bean)) {
        return false;
      }
    }
    for (DomBeanContainer container : DomUtil.getChildrenOf(parent, DomBeanContainer.class)) {
      if (!addContainerBeans(container, processor)) {
        return false;
      }
    }
    return true;
  }

  private static boolean addContainerBeans(DomBeanContainer container, Processor<CommonInfraBean> processor) {
    Collection<DomInfraBean> children = container.getChildren();
    for (DomInfraBean child : children) {
      if (!processor.process(child)) {
        return false;
      }
    }
    for (DomInfraBean child2 : children) {
      if ((child2 instanceof DomBeanContainer) && !addContainerBeans((DomBeanContainer) child2, processor)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean processXmlFactoryBeans(Project project, GlobalSearchScope scope, Processor<BeanPointer<?>> processor) {
    BeanSearchParameters params = new BeanSearchParameters(project);
    params.setSearchScope(scope);
    if (!InfraXmlBeansIndex.processFactoryBeans(params, processor)) {
      return false;
    }
    return InfraXmlBeansIndex.processFactoryMethods(params, processor);
  }
}
