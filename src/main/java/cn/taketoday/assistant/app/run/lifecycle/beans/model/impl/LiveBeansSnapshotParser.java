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

package cn.taketoday.assistant.app.run.lifecycle.beans.model.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.ContainerUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBeansModel;

public final class LiveBeansSnapshotParser {
  private static final Logger LOG = Logger.getInstance(LiveBeansSnapshotParser.class);
  private static final Context[] EMPTY_CONTEXTS = new Context[0];
  private static final Bean[] EMPTY_BEANS = new Bean[0];

  public LiveBeansModel parseEndpoint(Object data) {
    Gson gson = new GsonBuilder().create();
    JsonElement jsonElement = gson.toJsonTree(data);
    EndpointApplication application = gson.fromJson(jsonElement, EndpointApplication.class);
    Context[] contexts = ContainerUtil.map2Array(application.contexts.entrySet(), EMPTY_CONTEXTS, entry -> {
      EndpointContext endpointContext = entry.getValue();
      Context context = new Context();
      context.context = entry.getKey();
      context.parent = endpointContext.parentId;
      context.beans = ContainerUtil.map2Array(endpointContext.beans.entrySet(), EMPTY_BEANS, beanEntry -> {
        Bean bean = beanEntry.getValue();
        bean.bean = beanEntry.getKey();
        return bean;
      });
      return context;
    });
    return parse(contexts);
  }

  public LiveBeansModel parse(String snapshot) {
    Gson gson = new GsonBuilder().create();
    Context[] contexts = gson.fromJson(snapshot, Context[].class);
    return parse(contexts);
  }

  private static LiveBeansModel parse(Context[] contexts) {
    Map<Context, LiveContextImpl> liveContexts = new LinkedHashMap<>();
    Map<String, LiveContextImpl> liveContextsByName = new HashMap<>();
    Map<Context, Map<String, LiveBeanImpl>> contextLiveBeans = new HashMap<>();
    Map<String, LiveBeanImpl> allBeans = new HashMap<>();
    for (Context context : contexts) {
      if (context.context != null) {
        LiveContextImpl liveContext = new LiveContextImpl(context.context);
        liveContexts.put(context, liveContext);
        liveContextsByName.putIfAbsent(context.context, liveContext);
        Map<String, LiveResourceImpl> liveResources = new HashMap<>();
        Map<String, LiveBeanImpl> liveBeans = new HashMap<>();
        contextLiveBeans.put(context, liveBeans);
        for (Bean bean : context.beans) {
          if (bean.bean != null) {
            String resource = bean.resource;
            if (resource == null) {
              resource = "null";
            }
            LiveResourceImpl liveResource = liveResources.get(resource);
            if (liveResource == null) {
              liveResource = new LiveResourceImpl(resource, liveContext);
              liveResources.put(resource, liveResource);
            }
            LiveBeanImpl liveBean = LiveBeanImpl.createLiveBean(bean.bean, bean.scope,
                    bean.type, liveResource);
            liveResource.addBean(liveBean);
            liveBeans.put(bean.bean, liveBean);
            allBeans.put(bean.bean, liveBean);
          }
        }
        liveContext.addResources(liveResources.values());
      }
    }
    for (Context context2 : contexts) {
      if (context2.context != null) {
        if (context2.parent != null) {
          LiveContextImpl liveContext2 = liveContexts.get(context2);
          LOG.assertTrue(liveContext2 != null);
          liveContext2.setParent(liveContextsByName.get(context2.parent));
        }
        for (Bean bean2 : context2.beans) {
          if (bean2.bean != null) {
            Map<String, LiveBeanImpl> liveBeans2 = contextLiveBeans.get(context2);
            LOG.assertTrue(liveBeans2 != null);
            LiveBeanImpl liveBean2 = liveBeans2.get(bean2.bean);
            LOG.assertTrue(liveBean2 != null);
            for (String dependency : bean2.dependencies) {
              if (dependency != null) {
                LiveBeanImpl dependencyBean = liveBeans2.get(dependency);
                if (dependencyBean == null) {
                  dependencyBean = allBeans.get(dependency);
                  if (dependencyBean == null) {
                    dependencyBean = LiveBeanImpl.createInnerBean(dependency);
                  }
                }
                liveBean2.addDependency(dependencyBean);
              }
            }
          }
        }
      }
    }
    return new LiveBeansModelImpl(liveContexts.values());
  }

  private static class Context {
    public String context;
    public String parent;
    public Bean[] beans = EMPTY_BEANS;

  }

  private static class Bean {
    public String bean;
    public String scope;
    public String type;
    public String resource;
    public String[] dependencies = ArrayUtilRt.EMPTY_STRING_ARRAY;

  }

  private static class EndpointApplication {
    public Map<String, EndpointContext> contexts;
  }

  private static class EndpointContext {
    public Map<String, Bean> beans;
    public String parentId;

  }
}
