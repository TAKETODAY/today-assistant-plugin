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

package cn.taketoday.assistant.web.mvc.views.resolvers;

import com.intellij.javaee.web.WebDirectoryElement;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.web.mvc.services.WebMvcUtils;
import cn.taketoday.assistant.web.mvc.views.BeanNameViewResolverFactory;

public class XmlViewResolver extends BeanNameViewResolverFactory.BeanNameViewResolver {
  private final String myLocation;

  public XmlViewResolver(Module module, CommonInfraBean bean) {
    super(module, "XmlViewResolver[" + bean.getBeanName() + "]");
    String location = InfraPropertyUtils.getPropertyStringValue(bean, "location");
    this.myLocation = location == null ? "/WEB-INF/views.xml" : location;
  }

  @Override

  public String handleElementRename(String newElementName) {
    return newElementName;
  }

  @Override

  protected Collection<InfraModel> getModels() {
    Collection<InfraModel> models = new HashSet<>();
    for (WebDirectoryElement element : WebMvcUtils.findWebDirectoryElements(this.myLocation, getModule())) {
      PsiFile file = element.getOriginalFile();
      if (!(file instanceof XmlFile)) {
        List emptyList = Collections.emptyList();
        return emptyList;
      }
      ContainerUtil.addIfNotNull(models, InfraManager.from(file.getProject()).getInfraModelByFile(file));
    }
    return models;
  }
}
