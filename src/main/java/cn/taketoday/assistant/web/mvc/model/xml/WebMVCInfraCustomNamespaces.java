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

package cn.taketoday.assistant.web.mvc.model.xml;

import com.intellij.util.xml.reflect.DomExtensionsRegistrar;

import cn.taketoday.assistant.dom.CustomNamespaceRegistrar;
import cn.taketoday.assistant.dom.InfraCustomNamespaces;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;

public class WebMVCInfraCustomNamespaces extends InfraCustomNamespaces {

  public NamespacePolicies getNamespacePolicies() {
    return NamespacePolicies.simple(InfraMvcConstant.MVC_NAMESPACE_KEY, InfraMvcConstant.MVC_NAMESPACE);
  }

  public int getModelVersion() {
    return 3;
  }

  public int getStubVersion() {
    return 3;
  }

  public void registerExtensions(DomExtensionsRegistrar registrar) {
    CustomNamespaceRegistrar.create(registrar, InfraMvcConstant.MVC_NAMESPACE_KEY)
            .add("annotation-driven", AnnotationDriven.class)
            .add("content-negotiation", ViewResolverContentNegotiation.class)
            .add("cors", Cors.class)
            .add("default-servlet-handler", DefaultServletHandler.class)
            .add("interceptors", Interceptors.class)
            .add("redirect-view-controller", RedirectViewController.class)
            .add("resources", Resources.class)
            .add("status-controller", StatusController.class)
            .add("view-controller", ViewController.class)
            .add("view-resolvers", ViewResolvers.class)
            .add("freemarker-configurer", FreeMarkerConfigurer.class)
            .add("groovy-configurer", GroovyConfigurer.class)
            .add("tiles-configurer", TilesConfigurer.class)
            .add("velocity-configurer", VelocityConfigurer.class);
  }
}
