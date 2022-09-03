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

package cn.taketoday.assistant.app.mvc.lifecycle.mappings.gutter;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.microservices.http.request.NavigatorHttpRequest;
import com.intellij.microservices.http.request.RequestNavigator;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.WizardPopup;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.ui.popup.list.PopupListElementRenderer;
import com.intellij.util.SmartList;
import com.intellij.util.ui.JBUI;

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMapping;
import cn.taketoday.assistant.app.mvc.statistics.InfraMvcUsageTriggerCollector;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationUrlUtil;
import cn.taketoday.lang.Nullable;

public class LiveRequestMappingsNavigationHandler implements GutterIconNavigationHandler<PsiElement> {
  private final List<MethodNavigationItem> myItems;

  public LiveRequestMappingsNavigationHandler(List<MethodNavigationItem> items) {
    this.myItems = items;
  }

  String getLiveMarkerInfoTooltipText() {
    if (this.myItems.size() == 1) {
      MethodNavigationItem item = this.myItems.get(0);
      if (item.mappings.size() == 1) {
        return getMappingUrl(item.applicationUrl, item.servletPath, item.mappings.get(0)) + " [" + item.info.getRunProfile().getName() + "]";
      }
      return item.info.getRunProfile().getName();
    }
    return InfraAppBundle.message("infra.live.request.mapping.gutter.name");
  }

  public void navigate(MouseEvent e, PsiElement element) {
    navigate(new RelativePoint(e));
  }

  public void navigate(RelativePoint relativePoint) {
    if (this.myItems.isEmpty()) {
      return;
    }
    if (this.myItems.size() == 1) {
      MethodNavigationItem item = this.myItems.get(0);
      if (item.mappings.size() == 1) {
        RequestNavigatorPopupStep navigatorsStep = navigateToMapping(item, item.mappings.get(0));
        if (navigatorsStep != null) {
          JBPopupFactory.getInstance().createListPopup(navigatorsStep).show(relativePoint);
          return;
        }
        return;
      }
      showPopup(relativePoint, createMappingsPopup(item.project, null, item), item.project);
      return;
    }
    Project project = this.myItems.get(0).project;
    showPopup(relativePoint, createApplicationsPopup(project, this.myItems), project);
  }

  private static void showPopup(RelativePoint relativePoint, ListPopup popup, Project project) {
    NavigationUtil.hidePopupIfDumbModeStarts(popup, project);
    popup.show(relativePoint);
  }

  private static ListPopup createApplicationsPopup(Project project, List<MethodNavigationItem> items) {
    return new ListPopupImpl(project, new ApplicationsPopupStep(items)) {
      protected WizardPopup createPopup(WizardPopup parent, PopupStep step, Object parentValue) {
        if (!(step instanceof ListPopupStep)) {
          throw new IllegalArgumentException("Step: " + step.getClass().toString());
        }
        if (!(parentValue instanceof MethodNavigationItem)) {
          throw new IllegalArgumentException("Parent value: " + step.getClass().toString());
        }
        return createMappingsPopup(getProject(), parent, (MethodNavigationItem) parentValue);
      }
    };
  }

  private static ListPopupImpl createMappingsPopup(Project project, WizardPopup parent, MethodNavigationItem item) {
    MappingsPopupStep step = new MappingsPopupStep(item);
    return new ListPopupImpl(project, parent, step, item) {
      protected ListCellRenderer<?> getListElementRenderer() {
        return new LiveRequestMappingPopupRenderer(this, step, item.applicationUrl, item.servletPath);
      }
    };
  }

  private static RequestNavigatorPopupStep navigateToMapping(MethodNavigationItem item, LiveRequestMapping mapping) {
    NavigatorHttpRequest request = createRequest(item.applicationUrl, item.servletPath, mapping);
    List<RequestNavigator> navigators = RequestNavigator.getRequestNavigators(request);
    if (navigators.isEmpty()) {
      return null;
    }
    if (navigators.size() == 1) {
      navigators.get(0).navigate(item.project, request, item.info.getRunProfile().getName());
      triggerUsage(item.project, navigators.get(0), item.place);
      return null;
    }
    return new RequestNavigatorPopupStep(item, request, navigators);
  }

  public static NavigatorHttpRequest createRequest(String applicationUrl, String servletPath, LiveRequestMapping mapping) {
    String url = getMappingUrl(applicationUrl, servletPath, mapping);
    List<String> requestMethods = mapping.getRequestMethods();
    String requestMethod = (requestMethods.isEmpty() || requestMethods.contains("GET")) ? "GET" : requestMethods.get(0);
    List<Pair<String, String>> headers = mapping.getHeaders().stream().filter(header -> {
      return !header.first.startsWith("!");
    }).collect(Collectors.toCollection(SmartList::new));
    mapping.getProduces().stream().filter(value -> {
      return !value.startsWith("!");
    }).findFirst().ifPresent(value2 -> {
      headers.add(Pair.create("Accept", value2));
    });
    mapping.getConsumes().stream().filter(value3 -> {
      return !value3.startsWith("!");
    }).findFirst().ifPresent(value4 -> {
      headers.add(Pair.create("Content-Type", value4));
    });
    List<Pair<String, String>> params = mapping.getParams().stream().filter(param -> {
      return !param.first.startsWith("!");
    }).collect(Collectors.toCollection(SmartList::new));
    return new NavigatorHttpRequest(url, requestMethod, headers, params);
  }

  public static String getMappingUrl(String applicationUrl, String servletPath, LiveRequestMapping mapping) {
    String servletMappingPath = mapping.getDispatcherServlet().getServletMappingPath();
    if (servletMappingPath != null) {
      servletPath = servletMappingPath;
    }
    return InfraApplicationUrlUtil.getInstance().getMappingUrl(applicationUrl, servletPath, mapping.getPath());
  }

  private static void triggerUsage(Project project, RequestNavigator navigator, String place) {
    String event = PluginInfoDetectorKt.getPluginInfo(navigator.getClass()).isDevelopedByJetBrains() ? navigator.getId() : "third.party";
    InfraMvcUsageTriggerCollector.trigger(project, event, place);
  }

  private static class ApplicationsPopupStep extends BaseListPopupStep<MethodNavigationItem> {

    ApplicationsPopupStep(List<MethodNavigationItem> items) {
      super(InfraAppBundle.message("infra.live.request.mapping.gutter.popup.title"), items);
    }

    public String getTextFor(MethodNavigationItem item) {
      String name = item.info.getRunProfile().getName();
      return name;
    }

    public boolean isSpeedSearchEnabled() {
      return true;
    }

    public boolean hasSubstep(MethodNavigationItem item) {
      return true;
    }

    public Icon getIconFor(MethodNavigationItem item) {
      return Icons.Today;
    }

    public PopupStep<?> onChosen(MethodNavigationItem item, boolean finalChoice) {
      return new MappingsPopupStep(item);
    }
  }

  private static class MappingsPopupStep extends BaseListPopupStep<LiveRequestMapping> {
    private final MethodNavigationItem myItem;

    MappingsPopupStep(MethodNavigationItem item) {
      super(item.info.getRunProfile().getName(), item.mappings);
      this.myItem = item;
    }

    public boolean isSpeedSearchEnabled() {
      return true;
    }

    public boolean hasSubstep(LiveRequestMapping mapping) {
      NavigatorHttpRequest request = createRequest(this.myItem.applicationUrl, this.myItem.servletPath, mapping);
      return RequestNavigator.getRequestNavigators(request).size() > 1;
    }

    public boolean isSelectable(LiveRequestMapping mapping) {
      NavigatorHttpRequest request = createRequest(this.myItem.applicationUrl, this.myItem.servletPath, mapping);
      return !RequestNavigator.getRequestNavigators(request).isEmpty();
    }

    public PopupStep<?> onChosen(LiveRequestMapping mapping, boolean finalChoice) {
      return navigateToMapping(this.myItem, mapping);
    }
  }

  private static class RequestNavigatorPopupStep extends BaseListPopupStep<RequestNavigator> {
    private final MethodNavigationItem myItem;
    private final NavigatorHttpRequest myRequest;

    RequestNavigatorPopupStep(MethodNavigationItem item, NavigatorHttpRequest request, List<RequestNavigator> navigators) {
      super(null, navigators);
      this.myItem = item;
      this.myRequest = request;
    }

    public Icon getIconFor(RequestNavigator navigator) {
      return navigator.getIcon();
    }

    public String getTextFor(RequestNavigator navigator) {
      return navigator.getDisplayText();
    }

    public PopupStep<?> onChosen(RequestNavigator navigator, boolean finalChoice) {
      return this.doFinalStep(() -> {
        navigator.navigate(this.myItem.project, this.myRequest, this.myItem.info.getRunProfile().getName());
        triggerUsage(this.myItem.project, navigator, this.myItem.place);
      });
    }
  }

  private static class LiveRequestMappingPopupRenderer extends PopupListElementRenderer<LiveRequestMapping> {
    private final MappingsPopupStep myPopupStep;
    private final String myApplicationUrl;
    private final String myServletPath;
    private SimpleColoredComponent myLabel;

    LiveRequestMappingPopupRenderer(ListPopupImpl popup, MappingsPopupStep popupStep, String applicationUrl, String servletPath) {
      super(popup);
      this.myPopupStep = popupStep;
      this.myApplicationUrl = applicationUrl;
      this.myServletPath = servletPath;
    }

    protected JComponent createItemComponent() {
      createLabel();
      this.myLabel = new SimpleColoredComponent();
      this.myLabel.setBorder(JBUI.Borders.emptyBottom(1));
      return layoutComponent(this.myLabel);
    }

    protected void customizeComponent(JList<? extends LiveRequestMapping> list, LiveRequestMapping value, boolean isSelected) {
      super.customizeComponent(list, value, isSelected);
      this.myLabel.clear();
      boolean isSelectable = this.myPopupStep.isSelectable(value);
      this.myLabel.setEnabled(isSelectable);
      this.myLabel.setIcon(Icons.RequestMapping);
      String mappingUrl = getMappingUrl(this.myApplicationUrl, this.myServletPath, value);
      this.myLabel.append(mappingUrl, SimpleTextAttributes.REGULAR_ATTRIBUTES);
      java.util.List<String> requestMethods = value.getRequestMethods();
      if (!requestMethods.isEmpty()) {
        this.myLabel.append(" [" + StringUtil.join(requestMethods, "|") + "]", SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }
    }
  }

  public List<GotoRelatedItem> getRelatedItems(PsiElement element) {
    return this.myItems.stream().flatMap(item -> {
      return item.mappings.stream().map(mapping -> {
        return Pair.create(item, createRequest(item.applicationUrl, item.servletPath, mapping));
      });
    }).flatMap(itemAndRequest -> {
      return RequestNavigator.getRequestNavigators().stream().filter(navigator -> {
        return navigator.accept(itemAndRequest.getSecond()) && navigator.hasTarget();
      }).map(navigator2 -> {
        return Trinity.create(itemAndRequest.first, itemAndRequest.second, navigator2);
      });
    }).map(itemAndRequestAndNavigator -> {
      return createGotoRelatedItem(element, itemAndRequestAndNavigator.getFirst(), itemAndRequestAndNavigator.getSecond(),
              itemAndRequestAndNavigator.getThird());
    }).collect(Collectors.toList());
  }

  public static GotoRelatedItem createGotoRelatedItem(PsiElement element, MethodNavigationItem item, NavigatorHttpRequest request, RequestNavigator navigator) {
    return new GotoRelatedItem(element, navigator.getNavigationGroupName()) {

      public String getCustomName() {
        return navigator.getNavigationMessage(request);
      }

      @Nullable
      public Icon getCustomIcon() {
        return navigator.getIcon();
      }

      @Nullable
      public PsiElement getElement() {
        return null;
      }

      public void navigate() {
        navigator.navigate(element.getProject(), request, item.info.getRunProfile().getName());
      }
    };
  }

  public static class MethodNavigationItem {
    final Project project;
    final InfraApplicationInfo info;
    final String applicationUrl;
    final String servletPath;
    final List<LiveRequestMapping> mappings;
    final String place;

    public MethodNavigationItem(Project project, InfraApplicationInfo info, String applicationUrl, String servletPath, List<LiveRequestMapping> mappings, String place) {
      this.project = project;
      this.info = info;
      this.applicationUrl = applicationUrl;
      this.servletPath = servletPath;
      this.mappings = mappings;
      this.place = place;
    }
  }
}
