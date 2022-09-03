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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.javaee.web.WebDirectoryElement;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.converters.InfraConverterUtil;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.services.WebMvcService;
import cn.taketoday.assistant.web.mvc.services.WebMvcUtils;
import cn.taketoday.lang.Nullable;

public class BeanNameViewResolverFactory extends ViewResolverFactory {

  @Override
  public String getBeanClass() {
    return "cn.taketoday.web.servlet.view.BeanNameViewResolver";
  }

  @Override

  public Set<ViewResolver> createViewResolvers(@Nullable CommonInfraBean bean, CommonInfraModel model) {
    Module module = model.getModule();
    if (module == null) {
      return Collections.emptySet();
    }
    BeanNameViewResolver beanNameViewResolver = new BeanNameViewResolver(module, bean.getBeanName() + "[" + bean.getContainingFile().getName() + "]");
    return Collections.singleton(beanNameViewResolver);
  }

  public static class BeanNameViewResolver extends ViewResolver {

    private final Module myModule;

    private final String myID;

    public BeanNameViewResolver(Module module, String ID) {
      this.myModule = module;
      this.myID = ID;
    }

    public String toString() {
      return "BeanNameViewResolver{id='" + this.myID + "'}";
    }

    @Override
    @TestOnly

    public String getID() {
      return this.myID;
    }

    @Override

    public Set<PsiElement> resolveView(String viewName) {
      if (StringUtil.isEmpty(viewName)) {
        return Collections.emptySet();
      }
      Set<PsiElement> elements = new HashSet<>();
      Collection<InfraModel> models = getModels();
      for (InfraModel model : models) {
        BeanPointer<?> beanPointer = InfraModelSearchers.findBean(model, viewName);
        if (beanPointer != null) {
          CommonInfraBean bean = beanPointer.getBean();
          String url = InfraPropertyUtils.getPropertyStringValue(bean, "url");
          if (url != null) {
            for (WebDirectoryElement webDirectoryElement : WebMvcUtils.findWebDirectoryElements(url, getModule())) {
              elements.add(webDirectoryElement.getOriginalFile());
            }
          }
          PsiElement pointerPsiElement = beanPointer.getPsiElement();
          if (pointerPsiElement != null) {
            elements.add(pointerPsiElement);
          }
        }
      }
      return elements;
    }

    @Override

    public List<LookupElement> getAllResolverViews() {
      PsiClass servletView = InfraUtils.findLibraryClass(getModule(), InfraMvcConstant.VIEW);
      if (servletView == null) {
        List<LookupElement> emptyList = Collections.emptyList();
        return emptyList;
      }
      ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byClass(servletView).withInheritors();
      List<LookupElement> variants = new ArrayList<>();
      for (InfraModel model : getModels()) {
        List<BeanPointer<?>> beanPointers = InfraModelSearchers.findBeans(model, searchParameters);
        variants.addAll(ContainerUtil.mapNotNull(beanPointers, InfraConverterUtil::createCompletionVariant));
      }
      return variants;
    }

    @Override
    public String bindToElement(PsiElement element) {
      return null;
    }

    @Override
    public String handleElementRename(String newElementName) {
      return newElementName;
    }

    protected Collection<InfraModel> getModels() {
      Set<InfraModel> servletModels = WebMvcService.getServletModels(getModule());
      return servletModels;
    }

    public Module getModule() {
      Module module = this.myModule;
      return module;
    }
  }
}
