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

package cn.taketoday.assistant.beans;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.semantic.SemService;
import com.intellij.spring.CommonSpringModel;
import com.intellij.spring.boot.model.testing.jam.SpringBootstrapWithTest;
import com.intellij.spring.boot.model.testing.jam.mock.CustomJamMockBean;
import com.intellij.spring.boot.model.testing.jam.mock.MockBean;
import com.intellij.spring.contexts.model.BeansSpringModel;
import com.intellij.spring.model.BeanService;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.SpringImplicitBean;
import com.intellij.spring.model.jam.testContexts.ContextConfiguration;
import com.intellij.spring.model.jam.testContexts.SpringTestingImplicitContextsProvider;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.beans.mock.JamMockBean;
import cn.taketoday.assistant.beans.mock.JamMockBeans;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 15:48
 */
public class TestingImplicitContextsProvider extends SpringTestingImplicitContextsProvider {

  @Override
  public Collection<CommonSpringModel> getModels(
          @Nullable Module module, ContextConfiguration configuration, Set<String> activeProfiles) {
    LinkedHashSet<CommonSpringModel> models = new LinkedHashSet<>();

    GlobalSearchScope searchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, true);
    if (isAnnotated(configuration, module, "cn.taketoday.test.context.web.WebAppConfiguration")) {
      models.add(new BeansSpringModel(module, NotNullLazyValue.lazy(() -> {
        Collection<SpringBeanPointer<?>> pointers = new SmartList<>();
        ContainerUtil.addIfNotNull(pointers, createMockBean(module, searchScope, "mockServletContext", "cn.taketoday.mock.web.MockServletContext"));
        ContainerUtil.addIfNotNull(pointers, createMockBean(module, searchScope, "mockHttpSession", "cn.taketoday.mock.web.MockHttpSession"));
        ContainerUtil.addIfNotNull(pointers, createMockBean(module, searchScope, "mockHttpServletRequest", "cn.taketoday.mock.web.MockHttpServletRequest"));
        return pointers;
      })));
    }

    //
    PsiClass psiClass = configuration.getPsiElement();
    List<CommonSpringBean> beans = new ArrayList<>(getMockBeanImpliciteBeans(getMockBeans(psiClass)));
    JamMockBeans mockBeans = JamMockBeans.META.getJamElement(psiClass);
    if (mockBeans != null) {
      List mockBeans1 = mockBeans.getMockBeans();
      beans.addAll(getMockBeanImpliciteBeans(mockBeans1));
    }

    if (!beans.isEmpty()) {
      models.add(new BeansSpringModel(module, NotNullLazyValue.lazy(() -> BeanService.getInstance().mapSpringBeans(beans))));
    }

    // 
    models.addAll(getApplicationTestModels(module, configuration));
    return models;
  }

  //

  public Collection<CommonSpringModel> getApplicationTestModels(
          @Nullable Module module, ContextConfiguration configuration) {

    if (module == null) {
      return Collections.emptyList();
    }
    else {
      List<CommonSpringModel> models = new SmartList<>();
      GlobalSearchScope searchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, true);
      JavaPsiFacade facade = JavaPsiFacade.getInstance(module.getProject());
      if (configuration instanceof SpringBootstrapWithTest) {
        PsiAnnotation annotation = configuration.getAnnotation();
        if (annotation != null && "cn.taketoday.framework.test.context.ApplicationTest".equals(annotation.getQualifiedName())) {
          PsiAnnotationMemberValue attrValue = annotation.findAttributeValue("webEnvironment");
          if (attrValue != null) {
            WebEnvironmentType environmentType = JamCommonUtil.getEnumValue(attrValue, WebEnvironmentType.class);
            if (environmentType == WebEnvironmentType.RANDOM_PORT) {
              models.add(new BeansSpringModel(
                      module, NotNullLazyValue.lazy(() -> BeanService.getInstance().mapSpringBeans(getImplicitBeans(facade, searchScope)))));
            }
          }
        }
        else {
          models.add(new BeansSpringModel(
                  module, NotNullLazyValue.lazy(() -> BeanService.getInstance().mapSpringBeans(getImplicitBeans(facade, searchScope)))));
        }
      }
      return models;
    }
  }

  private static List<CommonSpringBean> getImplicitBeans(JavaPsiFacade facade, GlobalSearchScope searchScope) {
    List<CommonSpringBean> beans = new SmartList<>();
    ContainerUtil.addIfNotNull(beans, SpringImplicitBean.create("Testing Beans", facade.findClass("cn.taketoday.boot.test.web.client.TestRestTemplate", searchScope), "testRestTemplate"));
    ContainerUtil.addIfNotNull(beans, SpringImplicitBean.create("Testing Beans", facade.findClass("cn.taketoday.test.web.reactive.server.WebTestClient", searchScope), "webTestClient"));

    return beans;
  }

  private static enum WebEnvironmentType {
    MOCK(false),
    RANDOM_PORT(true),
    DEFINED_PORT(true),
    NONE(false);

    private boolean myEmbedded;

    private WebEnvironmentType(boolean embedded) {
      this.myEmbedded = embedded;
    }
  }

  private static @Nullable SpringBeanPointer<?> createMockBean(Module module,
          GlobalSearchScope searchScope, String beanName, String beanClass) {
    SpringImplicitBean bean = SpringImplicitBean.create("MVC Testing Beans", JavaPsiFacade.getInstance(module.getProject())
            .findClass(beanClass, searchScope), beanName);
    return bean != null ? BeanService.getInstance().createSpringBeanPointer(bean) : null;
  }

  public List<MockBean> getMockBeans(PsiClass psiClass) {
    List<MockBean> elements = new SmartList<>();
    SemService service = SemService.getSemService(psiClass.getProject());

    PsiAnnotation annotation = AnnotationUtil.findAnnotation(psiClass, "cn.taketoday.framework.test.mock.mockito.MockBean");
    if (annotation != null) {
      ContainerUtil.addAllNotNull(elements, service.getSemElements(JamMockBean.JAM_KEY, annotation));
    }
    ContainerUtil.addAllNotNull(elements, service.getSemElements(CustomJamMockBean.JAM_KEY, psiClass));
    return elements;
  }

  private static List<CommonSpringBean> getMockBeanImpliciteBeans(List<MockBean> mockBeans) {
    List<CommonSpringBean> beans = new SmartList<>();
    for (MockBean bean : mockBeans) {
      for (PsiClass aClass : bean.getMockClasses()) {
        beans.add(SpringImplicitBean.create("Mock Beans", aClass, StringUtil.decapitalize(StringUtil.notNullize(aClass.getName()))));
      }
    }

    return beans;
  }
}
