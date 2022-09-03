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

package cn.taketoday.assistant.app.mvc.providers;

import com.intellij.openapi.module.Module;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.UtilKt;

import java.util.List;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.InfraConfigValueSearcher;
import cn.taketoday.assistant.web.mvc.config.WebApplicationPortProvider;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

public final class WebBootApplicationPortProvider implements WebApplicationPortProvider {

  public List<String> getApplicationPort(Module module) {
    Intrinsics.checkNotNullParameter(module, "module");
    if (!InfraLibraryUtil.hasFrameworkLibrary(module)) {
      return CollectionsKt.emptyList();
    }
    List<String> smartList = new SmartList<>();

    String serverPortProperty = "server.port";
    String defaultServerPort = "8080";
    final String randomPortValue = "0";
    InfraConfigValueSearcher.productionForAllProfiles(module, "server.port").process(new Processor<ConfigurationValueResult>() {
      public boolean process(ConfigurationValueResult configValue) {
        String str;
        List list = smartList;
        String it = configValue.getValueText();
        if (it != null) {
          list = list;
          str = !Intrinsics.areEqual(StringsKt.trim(it).toString(), randomPortValue) ? it : null;
        }
        else {
          str = null;
        }
        UtilKt.addIfNotNull(list, str);
        return true;
      }
    });
    if (smartList.isEmpty()) {
      return CollectionsKt.listOf("8080");
    }
    return smartList;
  }
}
