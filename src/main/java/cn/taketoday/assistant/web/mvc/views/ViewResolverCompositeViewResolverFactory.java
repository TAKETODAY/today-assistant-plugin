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

package cn.taketoday.assistant.web.mvc.views;

import com.intellij.psi.PsiClass;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Collections;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.jam.JamPsiMethodInfraBean;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.lang.Nullable;

public class ViewResolverCompositeViewResolverFactory extends ViewResolverFactory {
  private static final ViewResolver DUMMY_RESOLVER = new CompositeViewResolver(Collections.emptyList());
  private static final Set<ViewResolver> OUR_RESOLVERS = Collections.singleton(DUMMY_RESOLVER);

  @Override
  protected boolean isMine(@Nullable CommonInfraBean bean, PsiClass beanClass) {
    return (bean instanceof JamPsiMethodInfraBean) && isInWebMvcSupportConfigurationSupport((JamPsiMethodInfraBean) bean);
  }

  @Override
  @Nullable
  protected String getBeanClass() {
    return InfraMvcConstant.VIEW_RESOLVER_COMPOSITE;
  }

  @Override
  protected Set<ViewResolver> createViewResolvers(@Nullable CommonInfraBean bean, CommonInfraModel model) {
    return OUR_RESOLVERS;
  }

  private static boolean isInWebMvcSupportConfigurationSupport(JamPsiMethodInfraBean bean) {
    PsiClass configurationClass;
    return "mvcViewResolver".equals(bean.getBeanName())
            && (configurationClass = PsiTreeUtil.getParentOfType(bean.getPsiElement(), PsiClass.class)) != null
            && InfraMvcConstant.WEB_MVC_CONFIGURATION_SUPPORT.equals(configurationClass.getQualifiedName());
  }
}
