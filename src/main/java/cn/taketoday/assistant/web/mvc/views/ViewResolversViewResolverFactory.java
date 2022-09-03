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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.SmartList;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomJavaUtil;
import com.intellij.util.xml.DomUtil;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraRef;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.model.xml.UrlViewResolverType;
import cn.taketoday.assistant.web.mvc.model.xml.ViewResolverBeanName;
import cn.taketoday.assistant.web.mvc.model.xml.ViewResolverContentNegotiation;
import cn.taketoday.assistant.web.mvc.model.xml.ViewResolverFreeMarker;
import cn.taketoday.assistant.web.mvc.model.xml.ViewResolverGroovy;
import cn.taketoday.assistant.web.mvc.model.xml.ViewResolverJsp;
import cn.taketoday.assistant.web.mvc.model.xml.ViewResolverTiles;
import cn.taketoday.assistant.web.mvc.model.xml.ViewResolverVelocity;
import cn.taketoday.assistant.web.mvc.model.xml.ViewResolvers;
import cn.taketoday.lang.Nullable;

public class ViewResolversViewResolverFactory extends ViewResolverFactory {

  @Override
  protected String getBeanClass() {
    return null;
  }

  @Override
  protected boolean isMine(@Nullable CommonInfraBean bean, PsiClass beanClass) {
    return bean instanceof ViewResolvers;
  }

  @Override

  protected Set<ViewResolver> createViewResolvers(@Nullable CommonInfraBean bean, CommonInfraModel model) {
    ViewResolvers viewResolvers = (ViewResolvers) bean;
    List<ViewResolver> resolvers = determineViewResolvers(viewResolvers, model);
    return Collections.singleton(new CompositeViewResolver(resolvers));
  }

  private static List<ViewResolver> determineViewResolvers(ViewResolvers viewResolvers, CommonInfraModel model) {
    PsiClass psiClass;
    SmartList<ViewResolver> smartList = new SmartList<>();
    for (DomElement springBean : DomUtil.getDefinedChildren(viewResolvers, true, false)) {
      if (!(springBean instanceof ViewResolverContentNegotiation)) {
        ViewResolver resolver = createBuiltinResolver(springBean, model);
        if (resolver != null) {
          smartList.add(resolver);
        }
        else {
          if (springBean instanceof InfraBean bean) {
            psiClass = PsiTypesUtil.getPsiClass(bean.getBeanType());
          }
          else if (springBean instanceof InfraRef ref) {
            BeanPointer<?> beanPointer = ref.getBean().getValue();
            psiClass = beanPointer != null ? beanPointer.getBeanClass() : null;
          }
          else if (springBean instanceof ViewResolverTiles) {
            psiClass = DomJavaUtil.findClass(InfraMvcConstant.TILES_3_VIEW_RESOLVER_CLASS, springBean);
          }
          else {
            psiClass = null;
          }
          if (psiClass != null) {
            findResolversByClass(psiClass, viewResolvers, model, smartList, factory -> {
              return !(factory instanceof ViewResolversViewResolverFactory);
            });
          }
        }
      }
    }
    return smartList;
  }

  @Nullable
  private static ViewResolver createBuiltinResolver(DomElement domElement, CommonInfraModel model) {
    if (domElement instanceof ViewResolverBeanName) {
      Module module = model.getModule();
      if (module != null) {
        return new BeanNameViewResolverFactory.BeanNameViewResolver(module, "ViewResolverBeanName");
      }
      return null;
    }
    else if (!(domElement instanceof UrlViewResolverType urlViewResolverType)) {
      return null;
    }
    else {
      if (domElement instanceof ViewResolverJsp) {
        return createUrlBasedViewResolver(urlViewResolverType, InfraMvcConstant.URL_BASED_VIEW_RESOLVER, model, InfraMvcConstant.WEB_INF, ".jsp");
      }
      if (domElement instanceof ViewResolverVelocity) {
        return createUrlBasedViewResolver(urlViewResolverType, InfraMvcConstant.VELOCITY_VIEW_RESOLVER, model, "", ".vm");
      }
      if (domElement instanceof ViewResolverFreeMarker) {
        return createUrlBasedViewResolver(urlViewResolverType, InfraMvcConstant.FREEMARKER_VIEW_RESOLVER, model, "", ".ftl");
      }
      if (domElement instanceof ViewResolverGroovy) {
        return createUrlBasedViewResolver(urlViewResolverType, InfraMvcConstant.URL_BASED_VIEW_RESOLVER, model, "", ".tpl");
      }
      return null;
    }
  }

  @Nullable
  private static ViewResolver createUrlBasedViewResolver(UrlViewResolverType urlViewResolverType, String viewResolverClassName, CommonInfraModel model,
          String defaultPrefix, String defaultSuffix) {
    PsiClass viewResolverClass;
    Module module = model.getModule();
    if (module == null || (viewResolverClass = DomJavaUtil.findClass(viewResolverClassName, urlViewResolverType)) == null) {
      return null;
    }
    Ref<ViewResolver> result = new Ref<>();
    processAllMineFactories(null, viewResolverClass, factory -> {
      for (ViewResolver resolver : factory.createResolvers(null, viewResolverClass, model)) {
        if (resolver instanceof UrlBasedViewResolver) {
          String prefix = StringUtil.defaultIfEmpty(urlViewResolverType.getPrefix().getStringValue(), defaultPrefix);
          String suffix = StringUtil.defaultIfEmpty(urlViewResolverType.getSuffix().getStringValue(), defaultSuffix);
          result.set(new UrlBasedViewResolver(module, urlViewResolverType.getXmlElementName(),
                  ((UrlBasedViewResolver) resolver).getPath(),
                  StringUtil.defaultIfEmpty(((UrlBasedViewResolver) resolver).getPrefix(), prefix),
                  StringUtil.defaultIfEmpty(((UrlBasedViewResolver) resolver).getSuffix(), suffix)));
          return false;
        }
      }
      return true;
    });
    if (result.isNull()) {
      throw new IllegalArgumentException("no registered resolver for " + viewResolverClassName + " and " + urlViewResolverType);
    }
    return result.get();
  }
}
