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

package cn.taketoday.assistant.beans.stereotype;

import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.spring.SpringModificationTrackersManager;
import com.intellij.spring.model.custom.ComponentScanExtender;
import com.intellij.util.SmartList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.JavaeeConstant;
import cn.taketoday.assistant.beans.stereotype.javaee.SpringCdiJakartaNamed;
import cn.taketoday.assistant.beans.stereotype.javaee.SpringCdiJavaxNamed;
import cn.taketoday.assistant.beans.stereotype.javaee.SpringJakartaManagedBean;
import cn.taketoday.assistant.beans.stereotype.javaee.SpringJavaxManagedBean;
import cn.taketoday.assistant.util.JamAnnotationTypeUtil;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 15:01
 */
public class SpringJamModel {
  private final Module myModule;
  private final JamService myJamService;

  public SpringJamModel(Module module) {
    this.myModule = module;
    this.myJamService = JamService.getJamService(myModule.getProject());
  }

  public List<InfraStereotypeElement> getStereotypeComponents() {
    Project project = this.myModule.getProject();
    if (DumbService.isDumb(project)) {
      return Collections.emptyList();
    }
    else {
      return CachedValuesManager.getManager(project).getCachedValue(myModule, () -> {
        List<InfraStereotypeElement> components = this.getStereotypeComponents(GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(this.myModule));
        return CachedValueProvider.Result.create(components, SpringModificationTrackersManager.getInstance(project).getOuterModelsDependencies());
      });
    }
  }

  public List<InfraStereotypeElement> getStereotypeComponents(GlobalSearchScope scope) {
    List<InfraStereotypeElement> stereotypeElements = new ArrayList<>();
    stereotypeElements.addAll(getComponents(scope));
    stereotypeElements.addAll(getControllers(scope));
    stereotypeElements.addAll(getRepositories(scope));
    stereotypeElements.addAll(getServices(scope));
    stereotypeElements.addAll(getConfigurations(scope));
    stereotypeElements.addAll(getCdiJavaxNamed(scope));
    stereotypeElements.addAll(getCdiJakartaNamed(scope));
    stereotypeElements.addAll(getJavaxManagedBeans(scope));
    stereotypeElements.addAll(getJakartaManagedBeans(scope));
    stereotypeElements.addAll(getCustomStereotypeComponents(scope));
    stereotypeElements.addAll(getStereotypeComponentExtensions(scope));
    return stereotypeElements;
  }

  private List<CustomComponent> getCustomStereotypeComponents(GlobalSearchScope scope) {
    SmartList smartList = new SmartList();
    for (String anno : JamAnnotationTypeUtil.getUserDefinedCustomComponentAnnotations(myModule)) {
      smartList.addAll(myJamService.getJamClassElements(CustomComponent.JAM_KEY, anno, scope));
    }
    return smartList;
  }

  private List<InfraStereotypeElement> getStereotypeComponentExtensions(GlobalSearchScope scope) {
    SmartList smartList = new SmartList();
    for (ComponentScanExtender extender : ComponentScanExtender.EP_NAME.getExtensionList()) {
      smartList.addAll(extender.getComponents(scope, this.myModule));
    }
    return smartList;
  }

  private List<Component> getComponents(GlobalSearchScope scope) {
    return this.myJamService.getJamClassElements(Component.META, AnnotationConstant.COMPONENT, scope);
  }

  private List<SpringCdiJavaxNamed> getCdiJavaxNamed(GlobalSearchScope scope) {
    return this.myJamService.getJamClassElements(SpringCdiJavaxNamed.META, JavaeeConstant.JAVAX_NAMED, scope);
  }

  private List<SpringCdiJakartaNamed> getCdiJakartaNamed(GlobalSearchScope scope) {
    return this.myJamService.getJamClassElements(SpringCdiJakartaNamed.META, JavaeeConstant.JAKARTA_NAMED, scope);
  }

  public List<Controller> getControllers(GlobalSearchScope scope) {
    SmartList smartList = new SmartList();
    Collection<String> controllerAnnotations = Controller.getControllerAnnotations().fun(this.myModule);
    for (String anno : controllerAnnotations) {
      smartList.addAll(this.myJamService.getJamClassElements(Controller.META, anno, scope));
    }
    return smartList;
  }

  private List<Repository> getRepositories(GlobalSearchScope scope) {
    SmartList smartList = new SmartList();
    Collection<String> repositoryAnnotations = Repository.getRepositoryAnnotations().fun(this.myModule);
    for (String anno : repositoryAnnotations) {
      smartList.addAll(this.myJamService.getJamClassElements(Repository.META, anno, scope));
    }
    return smartList;
  }

  private List<Service> getServices(GlobalSearchScope scope) {
    SmartList smartList = new SmartList();
    Collection<String> serviceAnnotations = Service.getServiceAnnotations().fun(this.myModule);
    for (String anno : serviceAnnotations) {
      smartList.addAll(this.myJamService.getJamClassElements(Service.META, anno, scope));
    }
    return smartList;
  }

  private List<SpringJavaxManagedBean> getJavaxManagedBeans(GlobalSearchScope scope) {
    return this.myJamService.getJamClassElements(SpringJavaxManagedBean.META, JavaeeConstant.JAVAX_MANAGED_BEAN, scope);
  }

  private List<SpringJakartaManagedBean> getJakartaManagedBeans(GlobalSearchScope scope) {
    return this.myJamService.getJamClassElements(SpringJakartaManagedBean.META, JavaeeConstant.JAKARTA_MANAGED_BEAN, scope);
  }

  public List<Configuration> getConfigurations(GlobalSearchScope scope) {
    if (DumbService.isDumb(this.myModule.getProject())) {
      return Collections.emptyList();
    }
    SmartList smartList = new SmartList();
    Collection<String> configurationAnnotations = Configuration.getAnnotations().fun(this.myModule);
    for (String anno : configurationAnnotations) {
      smartList.addAll(this.myJamService.getJamClassElements(Configuration.META, anno, scope));
    }
    return smartList;
  }

  public static SpringJamModel from(Module module) {
    return module.getService(SpringJamModel.class);
  }

}
