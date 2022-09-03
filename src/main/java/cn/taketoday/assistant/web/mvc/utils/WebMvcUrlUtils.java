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

package cn.taketoday.assistant.web.mvc.utils;

import com.intellij.microservices.url.Authority;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.web.mvc.config.WebApplicationPathProvider;
import cn.taketoday.assistant.web.mvc.config.WebApplicationPortProvider;
import cn.taketoday.lang.Nullable;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;

public final class WebMvcUrlUtils {

  public static final ExtensionPointName<WebApplicationPortProvider> APP_PORT_EP_NAME
          = ExtensionPointName.create("cn.taketoday.assistant.applicationPortProvider");

  public static final ExtensionPointName<WebApplicationPathProvider> APP_PATH_EP_NAME
          = ExtensionPointName.create("cn.taketoday.assistant.applicationPathProvider");
  private static final List<String> LOCALHOST = CollectionsKt.listOf("localhost", "127.0.0.1");

  public static List<Authority> getAuthoritiesByModule(@Nullable Module module) {
    if (module == null) {
      return CollectionsKt.emptyList();
    }
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, new CachedValueProvider<>() {
      @Nullable
      public CachedValueProvider.Result<List<Authority>> compute() {
        String name = module.getName();
        List result = CollectionsKt.mutableListOf(new WebMvcAuthorityPlaceholder(name));
        Iterable extensionList = APP_PORT_EP_NAME.getExtensionList();
        Collection destination$iv$iv = new ArrayList();
        for (Object element$iv$iv : extensionList) {
          WebApplicationPortProvider portProvider = (WebApplicationPortProvider) element$iv$iv;
          Iterable $this$flatMap$iv2 = portProvider.getApplicationPort(module);
          Collection destination$iv$iv2 = new ArrayList();
          for (Object element$iv$iv2 : $this$flatMap$iv2) {
            String port = (String) element$iv$iv2;
            ArrayList<String> collection = new ArrayList<>(Math.max(LOCALHOST.size(), 10));
            for (String host : LOCALHOST) {
              collection.add(host + ":" + port);
            }
            CollectionsKt.addAll(destination$iv$iv2, (Iterable) collection);
          }
          CollectionsKt.addAll(destination$iv$iv, destination$iv$iv2);
        }
        Iterable $this$mapTo$iv = destination$iv$iv;
        for (Object item$iv : $this$mapTo$iv) {
          String it = (String) item$iv;
          result.add(new Authority.Exact(it));
        }
        return Result.create(CollectionsKt.toList(result), new Object[] { PsiModificationTracker.MODIFICATION_COUNT });
      }
    });
  }

  public static List<String> getApplicationPaths(@Nullable Module module) {
    if (module == null) {
      return CollectionsKt.emptyList();
    }
    Object cachedValue = CachedValuesManager.getManager(module.getProject()).getCachedValue(module, new CachedValueProvider() {
      public CachedValueProvider.Result<List<String>> compute() {
        List result = new ArrayList();
        for (WebApplicationPathProvider pathProvider : APP_PATH_EP_NAME.getExtensionList()) {
          result.addAll(pathProvider.getApplicationPaths(module));
        }
        return Result.create(CollectionsKt.toList(result), new Object[] { PsiModificationTracker.MODIFICATION_COUNT });
      }
    });
    Intrinsics.checkNotNullExpressionValue(cachedValue, "CachedValuesManager.getM….MODIFICATION_COUNT)\n  })");
    return (List) cachedValue;
  }
}
