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

package cn.taketoday.assistant.app.mvc.lifecycle.mappings;

import com.intellij.execution.filters.HyperlinkInfoBase;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.impl.EditorHyperlinkSupport;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.microservices.http.request.NavigatorHttpRequest;
import com.intellij.microservices.http.request.RequestNavigator;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.AppUIExecutor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.SmartList;

import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.gutter.LiveRequestMappingsNavigationHandler;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMapping;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMappingsModel;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.impl.LiveRequestMappingsParser;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.tab.RequestMappingsTab;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfig;
import cn.taketoday.assistant.app.run.lifecycle.CodeAnalyzerLivePropertyListener;
import cn.taketoday.assistant.app.run.lifecycle.Endpoint;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraWebServerConfig;
import cn.taketoday.assistant.app.run.lifecycle.Property;
import cn.taketoday.assistant.app.run.lifecycle.tabs.EndpointTab;
import cn.taketoday.lang.Nullable;

public final class RequestMappingsEndpoint extends Endpoint<LiveRequestMappingsModel> {
  private static final String ENDPOINT_ID = "mappings";
  private static final String ENDPOINT_NAME = "requestMappingEndpoint";

  public RequestMappingsEndpoint() {
    super(ENDPOINT_ID, ENDPOINT_NAME);
  }

  @Override
  public LiveRequestMappingsModel parseData(@Nullable Object data) {
    return new LiveRequestMappingsParser().parse((Map) data);
  }

  @Override
  public EndpointTab<LiveRequestMappingsModel> createEndpointTab(InfraApplicationRunConfig runConfiguration, ProcessHandler processHandler) {
    return new RequestMappingsTab(this, runConfiguration, processHandler);
  }

  @Override
  public void infoCreated(Project project, ProcessHandler processHandler, InfraApplicationInfo info) {
    Property<LiveRequestMappingsModel> endpointData = info.getEndpointData(this);
    Property.PropertyListener analyzerRestarter = new CodeAnalyzerLivePropertyListener(project);
    endpointData.addPropertyListener(analyzerRestarter);
    info.getApplicationUrl().addPropertyListener(analyzerRestarter);
    RequestMappingLinkListener linkListener = new RequestMappingLinkListener(project, processHandler, info);
    endpointData.addPropertyListener(linkListener);
    Disposer.register(info, () -> {
      AppUIUtil.invokeOnEdt(linkListener::clearLinks);
    });
  }

  @Override
  public boolean isAvailable(@Nullable Module module) {
    return InfraLibraryUtil.hasRequestMappings(module);
  }

  public static Endpoint<LiveRequestMappingsModel> getInstance() {
    return Endpoint.EP_NAME.findExtension(RequestMappingsEndpoint.class);
  }

  private class RequestMappingLinkListener implements Property.PropertyListener {
    private final Project myProject;
    private final ProcessHandler myProcessHandler;
    private final InfraApplicationInfo myInfo;
    private Disposable myLinksDisposer;

    RequestMappingLinkListener(Project project, ProcessHandler processHandler, InfraApplicationInfo info) {
      this.myProject = project;
      this.myProcessHandler = processHandler;
      this.myInfo = info;
    }

    @Override
    public void propertyChanged() {
      String applicationUrl = this.myInfo.getApplicationUrl().getValue();
      if (applicationUrl == null) {
        return;
      }

      ExecutionConsole console = this.findConsole();
      if (console instanceof ConsoleViewImpl consoleView) {
        Condition<?> disposed = (o) -> {
          if (Disposer.isDisposed(this.myInfo)) {
            return true;
          }
          else {
            Editor editor = consoleView.getEditor();
            return editor == null || editor.isDisposed();
          }
        };

        InfraWebServerConfig configuration = myInfo.getServerConfig().getValue();
        String servletPath = configuration == null ? null : configuration.servletPath();
        AppUIExecutor.onUiThread().submit(() -> {
          if (!disposed.value(null)) {
            LiveRequestMappingsModel model = myInfo.getEndpointData(RequestMappingsEndpoint.this).getValue();
            if (model != null) {

              consoleView.performWhenNoDeferredOutput(() -> {
                if (!disposed.value(null)) {
                  clearLinks();

                  String text = consoleView.getText();
                  EditorHyperlinkSupport hyperlinkSupport = consoleView.getHyperlinks();
                  List<RangeHighlighter> highlighters = new SmartList<>();
                  List<LiveRequestMapping> mappings = model.getRequestMappings();
                  for (LiveRequestMapping mapping : mappings) {
                    if (mapping.getMethod() != null && mapping.getPath().startsWith("/")) {
                      int index = text.indexOf(mapping.getMapping());
                      if (index >= 0) {
                        NavigatorHttpRequest request = LiveRequestMappingsNavigationHandler.createRequest(applicationUrl, servletPath, mapping);
                        List<RequestNavigator> navigators = RequestNavigator.getRequestNavigators(request);
                        if (!navigators.isEmpty()) {
                          LiveRequestMappingsNavigationHandler.MethodNavigationItem navigationItem = new LiveRequestMappingsNavigationHandler.MethodNavigationItem(this.myProject, this.myInfo,
                                  applicationUrl, servletPath, new SmartList<>(mapping), "EditorPopup");
                          LiveRequestMappingsNavigationHandler navigationHandler = new LiveRequestMappingsNavigationHandler(new SmartList<>(navigationItem));
                          int offset = text.indexOf(mapping.getPath(), index);
                          if (offset >= 0) {
                            RangeHighlighter highlighter = hyperlinkSupport.createHyperlink(
                                    offset, offset + mapping.getPath().length(), SimpleTextAttributes.LINK_ATTRIBUTES.toTextAttributes(),
                                    new HyperlinkInfoBase() {
                                      public void navigate(Project project, @Nullable RelativePoint hyperlinkLocationPoint) {
                                        navigationHandler.navigate(hyperlinkLocationPoint);
                                      }
                                    });
                            highlighters.add(highlighter);
                          }
                        }
                      }
                    }
                  }

                  this.myLinksDisposer = () -> {
                    if (this.myLinksDisposer != null) {
                      this.myLinksDisposer = null;
                    }
                    else {
                      for (RangeHighlighter highlighter : highlighters) {
                        hyperlinkSupport.removeHyperlink(highlighter);
                      }
                    }
                  };

                  Disposer.register(consoleView, this.myLinksDisposer);
                }
              });
            }
          }
        });
      }
    }

    private ExecutionConsole findConsole() {
      for (RunContentDescriptor descriptor : RunContentManager.getInstance(this.myProject).getAllDescriptors()) {
        if (descriptor.getProcessHandler() == this.myProcessHandler) {
          return descriptor.getExecutionConsole();
        }
      }
      return null;
    }

    public void clearLinks() {
      Disposable linksDisposer = this.myLinksDisposer;
      if (linksDisposer != null) {
        this.myLinksDisposer = null;
        Disposer.dispose(linksDisposer);
      }
    }
  }
}
