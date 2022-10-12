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

package cn.taketoday.assistant.web.mvc.model;

import com.intellij.ide.presentation.Presentation;
import com.intellij.microservices.url.HttpUrlResolver;
import com.intellij.microservices.url.UrlPath;
import com.intellij.microservices.url.UrlResolveRequest;
import com.intellij.microservices.url.UrlResolver;
import com.intellij.microservices.url.UrlResolverFactory;
import com.intellij.microservices.url.UrlTargetInfo;
import com.intellij.microservices.url.references.UrlPathContextKt;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.web.mvc.WebMvcLibraryUtil;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.assistant.web.mvc.presentation.WebMvcPresentationConstant;
import cn.taketoday.assistant.web.mvc.providers.ApplicationPathUrlMapping;
import cn.taketoday.assistant.web.mvc.services.WebMvcUtils;
import cn.taketoday.assistant.web.mvc.utils.WebMvcUrlUtils;
import cn.taketoday.lang.Nullable;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;
import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;

@Presentation(typeName = WebMvcPresentationConstant.Web_MVC, icon = "cn.taketoday.assistant.Icons.RequestMapping")
public final class InfraMvcUrlResolver extends HttpUrlResolver {
  private final Project project;

  public static Iterable<UrlMappingElement> getAppPathUrlMappingElements(Module module, Iterable<UrlMappingElement> iterable) {
    List<String> applicationPaths = WebMvcUrlUtils.getApplicationPaths(module);
    if (applicationPaths.isEmpty()) {
      return iterable;
    }
    List appPathItems = new ArrayList();
    for (String applicationPath : applicationPaths) {
      for (UrlMappingElement urlMappingElement : iterable) {
        appPathItems.add(new ApplicationPathUrlMapping(applicationPath, urlMappingElement));
      }
    }
    return appPathItems;
  }

  public InfraMvcUrlResolver(Project project) {
    Intrinsics.checkNotNullParameter(project, "project");
    this.project = project;
  }

  public ModulesSorter getModules() {
    return ModulesSorter.getInstance(project);
  }

  public Iterable<UrlTargetInfo> resolve(UrlResolveRequest request) {
    return SequencesKt.asIterable(SequencesKt.flatMap(SequencesKt.filter(getModules(), new Function1<Module, Boolean>() {
              @Override
              public Boolean invoke(Module p1) {
                boolean isMvcCapableModule;
                Intrinsics.checkNotNullParameter(p1, "p1");
                isMvcCapableModule = isMvcCapableModule(p1);
                return isMvcCapableModule;
              }
            }),
            new Function1<Module, Sequence<? extends UrlTargetInfo>>() {
              @Override
              public Sequence<WebMvcUrlTargetInfo> invoke(Module module) {
                Intrinsics.checkNotNullParameter(module, "module");
                return SequencesKt.flatMap(UrlPath.Companion.combinations(UrlPathContextKt.chopLeadingEmptyBlock(request.getPath())),
                        new Function1<UrlPath, Sequence<? extends WebMvcUrlTargetInfo>>() {
                          @Override
                          public Sequence<WebMvcUrlTargetInfo> invoke(UrlPath path) {
                            Collection<UrlMappingElement> matchingUrlMappings = WebMvcUtils.getMatchingUrlMappings(module, path);
                            return SequencesKt.map(CollectionsKt.asSequence(getAppPathUrlMappingElements(module, matchingUrlMappings)),
                                    new Function1<UrlMappingElement, WebMvcUrlTargetInfo>() {
                                      @Override
                                      public WebMvcUrlTargetInfo invoke(UrlMappingElement resolved) {
                                        Intrinsics.checkNotNullParameter(resolved, "resolved");
                                        return new WebMvcUrlTargetInfo(getSupportedSchemes(), resolved,
                                                WebMvcUrlUtils.getAuthoritiesByModule(module));
                                      }
                                    });
                          }
                        });
              }
            }

    ));
  }

  public Iterable<UrlTargetInfo> getVariants() {
    return SequencesKt.asIterable(SequencesKt.flatMap(SequencesKt.filter(getModules(), new Function1<Module, Boolean>() {
      @Override
      public Boolean invoke(Module module) {
        return isMvcCapableModule(module);
      }
    }), new Function1<Module, Sequence<? extends UrlTargetInfo>>() {
      @Override
      public Sequence<? extends UrlTargetInfo> invoke(Module module) {
        ModulesSorter modules;
        modules = getModules();
        Function2<Long, Integer, Unit> listener = modules.listener(module);
        BiConsumer<Long, Integer> listener1 = new BiConsumer<>() {
          @Override
          public void accept(Long p0, Integer p1) {
            listener.invoke(p0, p1);
          }
        };

        Iterable<UrlMappingElement> urlMappingsWithoutInfraModel = WebMvcUtils.getUrlMappingsWithoutInfraModel(module, listener1);
        return SequencesKt.map(CollectionsKt.asSequence(getAppPathUrlMappingElements(module, urlMappingsWithoutInfraModel)),
                new Function1<UrlMappingElement, UrlTargetInfo>() {
                  @Override
                  public WebMvcUrlTargetInfo invoke(UrlMappingElement urlPath) {
                    return new WebMvcUrlTargetInfo(getSupportedSchemes(), urlPath, WebMvcUrlUtils.getAuthoritiesByModule(module));
                  }
                });

      }
    }));
  }

  public boolean isMvcCapableModule(Module it) {
    return InfraLibraryUtil.hasWebMvcLibrary(it) || WebMvcLibraryUtil.hasWebfluxLibrary(it);
  }

  public static final class Factory implements UrlResolverFactory {
    @Nullable
    public UrlResolver forProject(Project project) {
      Intrinsics.checkNotNullParameter(project, "project");
      if (InfraLibraryUtil.hasLibrary(project)) {
        return new InfraMvcUrlResolver(project);
      }
      return null;
    }
  }

}
