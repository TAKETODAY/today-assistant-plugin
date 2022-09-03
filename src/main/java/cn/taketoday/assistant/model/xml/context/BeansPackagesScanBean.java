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
package cn.taketoday.assistant.model.xml.context;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiPackage;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Stubbed;

import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.model.converters.PackageListConverter;
import cn.taketoday.assistant.model.xml.BeanName;
import cn.taketoday.assistant.model.xml.BeanNameProvider;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.lang.Nullable;

@BeanName(provider = BeansPackagesScanBean.BeansPackagesScanBeanNameProvider.class, displayOnly = true)
public interface BeansPackagesScanBean extends DomInfraBean, InfraBeansPackagesScan {

  /**
   * Returns name of provider do distinguish component-scans from different namespaces (Context, Data, ..).
   *
   * @return Name.
   */
  String getProviderName();

  class BeansPackagesScanBeanNameProvider implements BeanNameProvider<BeansPackagesScanBean> {

    @Nullable
    @Override
    public String getBeanName(BeansPackagesScanBean bean) {
      return "(" + StringUtil.notNullize(bean.getBasePackage().getRawText()) + ") [" + bean.getProviderName() + "]";
    }
  }

  @Required
  @Convert(PackageListConverter.class)
  @Stubbed
  GenericAttributeValue<Collection<PsiPackage>> getBasePackage();

  List<Filter> getIncludeFilters();

  List<Filter> getExcludeFilters();
}
