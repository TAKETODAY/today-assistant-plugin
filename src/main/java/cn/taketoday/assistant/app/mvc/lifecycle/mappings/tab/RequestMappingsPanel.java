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

package cn.taketoday.assistant.app.mvc.lifecycle.mappings.tab;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.services.RepaintLinkMouseListenerBase;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.microservices.http.request.NavigatorHttpRequest;
import com.intellij.microservices.http.request.RequestNavigator;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NullableFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.ui.table.BaseTableView;
import com.intellij.ui.table.TableView;
import com.intellij.util.SmartList;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.gutter.LiveRequestMappingsNavigationHandler;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveHandlerMethod;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMapping;
import cn.taketoday.assistant.app.mvc.statistics.InfraMvcUsageTriggerCollector;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfig;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager;
import cn.taketoday.assistant.app.run.lifecycle.InfraWebServerConfig;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraAppBundle.message;

final class RequestMappingsPanel extends JPanel implements Disposable {
  private static final String STORAGE_PREFIX = "RequestMappingsPanel";
  private static final String NOTIFICATION_DISPLAY_ID = "Infra Application Request Mappings";
  private static final Pattern MAPPINGS_DETAILS_START_2_1_1 = Pattern.compile(", [a-z]");

  private final Project project;
  private final ProcessHandler processHandler;
  private final InfraApplicationRunConfig runConfiguration;
  private final TableView<LiveRequestMappingItem> tableView;
  private final ListTableModel<LiveRequestMappingItem> model;
  private final NullableFactory<CommonInfraModel> modelFactory;

  private String defaultPath;
  private final MergingUpdateQueue mergingUpdateQueue;

  RequestMappingsPanel(Project project, InfraApplicationRunConfig runConfiguration, ProcessHandler processHandler, List<AnAction> customActions) {
    super(new BorderLayout());
    this.mergingUpdateQueue = new MergingUpdateQueue("RequestMappingsTableView", 100, true, this, this);
    this.project = project;
    this.runConfiguration = runConfiguration;
    this.processHandler = processHandler;
    this.modelFactory = () -> {
      Module module = this.runConfiguration.getModule();
      if (module == null) {
        return null;
      }
      return InfraManager.from(this.project).getCombinedModel(module);
    };
    List<ColumnInfo> columns = new ArrayList<>();
    columns.add(new RequestMappingsColumnInfo<LiveRequestMapping>(message("infra.application.endpoints.mappings.path")) {
      public LiveRequestMapping valueOf(LiveRequestMappingItem item) {
        return item.getMapping();
      }

      @Override
      protected String getItemTooltipText(LiveRequestMapping mapping) {
        InfraApplicationInfo info;
        String applicationUrl;
        String tooltip = StringUtil.trimEnd(StringUtil.trimStart(mapping.getMapping(), "{"), "}").replaceAll("],", "]<br>");
        if (!tooltip.startsWith("[/")) {
          Matcher matcher = MAPPINGS_DETAILS_START_2_1_1.matcher(tooltip);
          if (matcher.find()) {
            tooltip = tooltip.substring(0, matcher.start()) + "<br>" + tooltip.substring(matcher.end() - 1);
          }
        }
        if (canNavigate(mapping)
                && (info = InfraApplicationLifecycleManager.from(RequestMappingsPanel.this.project)
                .getInfraApplicationInfo(RequestMappingsPanel.this.processHandler)) != null && (applicationUrl = info.getApplicationUrl().getValue()) != null) {
          InfraWebServerConfig applicationServerConfiguration = info.getServerConfig().getValue();
          String servletPath = applicationServerConfiguration == null ? null : applicationServerConfiguration.servletPath();
          String mappingUrl = LiveRequestMappingsNavigationHandler.getMappingUrl(applicationUrl, servletPath, mapping);
          tooltip = mappingUrl + "<br>" + tooltip;
        }
        return String.format("<html><body>%s</body></html>", tooltip);
      }

      @Nullable
      public Comparator<LiveRequestMappingItem> getComparator() {
        return Comparator.comparing(item -> item.getMapping().getPath(), StringUtil::naturalCompare);
      }
    });
    columns.add(new RequestMappingsStringColumnInfo(message("infra.application.endpoints.mappings.method")) {
      @Nullable
      public String valueOf(LiveRequestMappingItem item) {
        LiveHandlerMethod method = item.getMapping().getMethod();
        if (method == null) {
          return null;
        }
        return method.getDisplayName();
      }

      @Override
      @Nullable
      protected String getItemTooltipText(LiveRequestMapping mapping) {
        LiveHandlerMethod method = mapping.getMethod();
        if (method == null) {
          return null;
        }
        return StringUtil.escapeXmlEntities(method.getRawMethod());
      }
    });

    Ref<Boolean> isSB20 = new Ref<>(Boolean.TRUE);

    if (!isSB20.get()) {
      columns.add(new RequestMappingsStringColumnInfo(message("infra.application.endpoints.mappings.bean")) {
        @Nullable
        public String valueOf(LiveRequestMappingItem item) {
          return item.getMapping().getBean();
        }
      });
    }

    this.model = new ListTableModel<>(columns.toArray(ColumnInfo.EMPTY_ARRAY));
    this.tableView = new TableView<>(this.model);
    this.tableView.getSelectionModel().setSelectionMode(0);
    RequestMappingLinkMouseListener linkMouseListener = new RequestMappingLinkMouseListener();
    linkMouseListener.installOn(this.tableView);
    new TableSpeedSearch(this.tableView);
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.tableView, 20, 31);
    add(scrollPane, "Center");
    installTableActions(customActions, isSB20.get());
    installDoubleClickListener();
    BaseTableView.restore(PropertiesComponent.getInstance(project), STORAGE_PREFIX, this.tableView);
  }

  public void dispose() {
    this.mergingUpdateQueue.cancelAllUpdates();
    BaseTableView.store(PropertiesComponent.getInstance(this.project), STORAGE_PREFIX, this.tableView);
  }

  void setItems(List<LiveRequestMapping> mappings) {
    tableView.setPaintBusy(true);
    RequestMappingsEndpointTabSettings settings = RequestMappingsEndpointTabSettings.getInstance(this.project);
    boolean showLibraryMappings = settings.isShowLibraryMappings();
    Set<String> filteredRequestMethods = settings.getFilteredRequestMethods().stream().map(Enum::name).collect(Collectors.toSet());

    mergingUpdateQueue.queue(new Update("update") {
      @Override
      public void run() {
        int oldIndex = tableView.getSelectedRow();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {

          DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            try {
              GlobalSearchScope searchScope = runConfiguration.getSearchScope();
              List<RequestMappingsPanel.LiveRequestMappingItem> items = mappings.stream().map((mapping) -> {
                LiveHandlerMethod mappingMethod = mapping.getMethod();
                VirtualFile file = mappingMethod == null ? null : getContainingFile(mappingMethod, searchScope);
                return new RequestMappingsPanel.LiveRequestMappingItem(mapping, file);
              }).filter((item) -> {
                List<String> requestMethods = item.getMapping().getRequestMethods();
                if (!requestMethods.isEmpty() && filteredRequestMethods.containsAll(item.getMapping().getRequestMethods())) {
                  return false;
                }
                else {
                  return showLibraryMappings
                          || item.getContainingFile() == null
                          || runConfiguration.getModule().getModuleWithDependenciesScope().contains(item.getContainingFile());
                }
              }).collect(Collectors.toList());

              AppUIUtil.invokeLaterIfProjectAlive(project, () -> {
                model.setItems(items);
                if (oldIndex >= 0 && oldIndex < tableView.getRowCount()) {
                  tableView.setRowSelectionInterval(oldIndex, oldIndex);
                }
              });
            }
            finally {
              tableView.setPaintBusy(false);
            }
          });
        });
      }
    });
  }

  void setDefaultPath(String defaultPath) {
    this.defaultPath = defaultPath;
    this.tableView.revalidate();
    this.tableView.repaint();
  }

  @Nullable
  LiveRequestMapping getSelectedMapping() {
    LiveRequestMappingItem item = this.tableView.getSelectedObject();
    if (item == null) {
      return null;
    }
    return item.getMapping();
  }

  private void installTableActions(List<AnAction> customActions, boolean isSB20) {
    DefaultActionGroup contextActionGroup = new DefaultActionGroup();
    AnAction editAction = new AnAction(message("infra.application.endpoints.mappings.edit.action.name"), null, AllIcons.Actions.Edit) {

      public void update(AnActionEvent e) {
        LiveRequestMapping mapping = getSelectedMapping();
        LiveHandlerMethod mappingMethod = mapping != null ? mapping.getMethod() : null;
        e.getPresentation().setEnabled(mappingMethod != null);
      }

      public void actionPerformed(AnActionEvent e) {
        performEditAction(e.getDataContext());
      }
    };
    editAction.registerCustomShortcutSet(CommonShortcuts.ENTER, this.tableView);
    contextActionGroup.add(editAction);
    if (!isSB20) {
      contextActionGroup.addSeparator();
      AnAction beanAction = new AnAction(message("infra.application.endpoints.mappings.bean.action.name"), null, Icons.SpringBean) {

        public void update(AnActionEvent e) {
          LiveRequestMapping mapping = getSelectedMapping();
          String beanName = mapping != null ? mapping.getBean() : null;
          e.getPresentation().setEnabled(beanName != null && InfraUtils.hasFacet(runConfiguration.getModule()));
        }

        public void actionPerformed(AnActionEvent e) {
          performNavigateToBeanAction(e.getDataContext());
        }
      };
      beanAction.registerCustomShortcutSet(ActionManager.getInstance().getAction("EditSource").getShortcutSet(), this.tableView);
      contextActionGroup.add(beanAction);
    }
    contextActionGroup.addSeparator();
    contextActionGroup.addAll(customActions);
    PopupHandler.installPopupMenu(this.tableView, contextActionGroup, "WebMvcRequestMappingPopup");
  }

  private void installDoubleClickListener() {
    new DoubleClickListener() {

      protected boolean onDoubleClick(MouseEvent event) {
        performEditAction(DataManager.getInstance().getDataContext(tableView));
        return true;
      }
    }.installOn(this.tableView);
  }

  private void performEditAction(DataContext dataContext) {
    if (DumbService.isDumb(this.project)) {
      return;
    }
    LiveRequestMappingItem item = this.tableView.getSelectedObject();
    LiveHandlerMethod mappingMethod = item != null ? item.getMapping().getMethod() : null;
    if (mappingMethod == null) {
      return;
    }
    InfraMvcUsageTriggerCollector.trigger(this.project, "EditLiveRequestMapping", "ToolwindowContent");
    GlobalSearchScope searchScope = this.runConfiguration.getSearchScope();
    VirtualFile file = getContainingFile(mappingMethod, searchScope);
    if (!Comparing.equal(file, item.getContainingFile())) {
      item.setContainingFile(file);
      this.tableView.revalidate();
      this.tableView.repaint();
    }
    PsiMethod method = mappingMethod.findMethod(this.project, searchScope);
    if (method != null && method.canNavigate()) {
      method.navigate(true);
    }
    else {
      showBalloon(dataContext, message("infra.application.endpoints.mappings.method.not.found"));
    }
  }

  private void performNavigateToBeanAction(DataContext dataContext) {
    BeanPointer<?> pointer;
    if (DumbService.isDumb(this.project)) {
      return;
    }
    LiveRequestMapping mapping = getSelectedMapping();
    String beanName = mapping != null ? mapping.getBean() : null;
    if (beanName == null) {
      return;
    }
    CommonInfraModel model = this.modelFactory.create();
    if (model != null && (pointer = InfraModelSearchers.findBean(model, beanName)) != null && pointer.isValid()) {
      PsiElement psiElement = pointer.getPsiElement();
      if (psiElement instanceof Navigatable navigatable) {
        if (navigatable.canNavigate()) {
          navigatable.navigate(true);
          return;
        }
      }
    }
    showBalloon(dataContext, message("infra.application.endpoints.mappings.bean.not.found"));
  }

  private void showBalloon(DataContext dataContext, String message) {
    ToolWindow toolWindow = dataContext.getData(PlatformDataKeys.TOOL_WINDOW);
    NotificationGroup.toolWindowGroup(NOTIFICATION_DISPLAY_ID, toolWindow.getId(), false)
            .createNotification(message, MessageType.WARNING)
            .notify(this.project);
  }

  @Nullable
  private VirtualFile getContainingFile(LiveHandlerMethod method, GlobalSearchScope searchScope) {
    PsiClass psiClass = method.findContainingClass(this.project, searchScope);
    if (psiClass != null && psiClass.getContainingFile() != null) {
      return psiClass.getContainingFile().getVirtualFile();
    }
    return null;
  }

  @Nullable
  static String getDisplayRequestMethods(LiveRequestMapping mapping) {
    List<String> requestMethods = mapping.getRequestMethods();
    if (requestMethods.isEmpty()) {
      return null;
    }
    return " [" + StringUtil.join(requestMethods, "|") + "]";
  }

  private static boolean canNavigate(LiveRequestMapping mapping) {
    return mapping.getMethod() != null && mapping.getPath().startsWith("/");
  }

  private abstract class RequestMappingsColumnInfo<T> extends ColumnInfo<LiveRequestMappingItem, T> {
    private final RequestMappingsTableCellRenderer myRenderer;

    RequestMappingsColumnInfo(@NlsContexts.ColumnName String name) {
      super(name);
      this.myRenderer = new RequestMappingsTableCellRenderer(project, processHandler);
    }

    @Nullable
    public TableCellRenderer getRenderer(LiveRequestMappingItem item) {
      this.myRenderer.setToolTipText(getItemTooltipText(item.getMapping()));
      this.myRenderer.setDefault(item.getMapping().getPath().equals(defaultPath));
      this.myRenderer.setFile(item.getContainingFile());
      return this.myRenderer;
    }

    @Nullable
    protected String getItemTooltipText(LiveRequestMapping mapping) {
      return null;
    }
  }

  private abstract class RequestMappingsStringColumnInfo extends RequestMappingsColumnInfo<String> {
    RequestMappingsStringColumnInfo(@NlsContexts.ColumnName String name) {
      super(name);
    }

    @Nullable
    public Comparator<LiveRequestMappingItem> getComparator() {
      return Comparator.comparing(this::valueOf, StringUtil::naturalCompare);
    }
  }

  private static class RequestMappingsTableCellRenderer extends ColoredTableCellRenderer {
    private final Project myProject;
    private final ProcessHandler myProcessHandler;
    private boolean myDefault;
    private VirtualFile myFile;

    RequestMappingsTableCellRenderer(Project project, ProcessHandler processHandler) {
      this.myProject = project;
      this.myProcessHandler = processHandler;
    }

    void setDefault(boolean defaultPath) {
      this.myDefault = defaultPath;
    }

    void setFile(VirtualFile file) {
      this.myFile = file;
    }

    protected void customizeCellRenderer(JTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
      SimpleTextAttributes pathAttributes;
      InfraApplicationInfo info;
      String applicationUrl;
      if (value instanceof LiveRequestMapping mapping) {
        PathLinkListener linkListener = null;
        if (canNavigate(mapping) && (info = InfraApplicationLifecycleManager.from(this.myProject)
                .getInfraApplicationInfo(this.myProcessHandler)) != null && (applicationUrl = info.getApplicationUrl().getValue()) != null) {
          InfraWebServerConfig applicationServerConfiguration = info.getServerConfig().getValue();
          String servletPath = applicationServerConfiguration == null ? null : applicationServerConfiguration.servletPath();
          NavigatorHttpRequest request = LiveRequestMappingsNavigationHandler.createRequest(applicationUrl, servletPath, mapping);
          List<RequestNavigator> navigators = RequestNavigator.getRequestNavigators(request);
          if (!navigators.isEmpty()) {
            LiveRequestMappingsNavigationHandler.MethodNavigationItem navigationItem = new LiveRequestMappingsNavigationHandler.MethodNavigationItem(this.myProject, info, applicationUrl, servletPath,
                    new SmartList<>(mapping), "ToolwindowContent");
            LiveRequestMappingsNavigationHandler navigationHandler = new LiveRequestMappingsNavigationHandler(new SmartList(navigationItem));
            linkListener = new PathLinkListener(mapping, navigationHandler);
          }
        }
        if (linkListener != null) {
          boolean isActive = selected || linkListener.equals(ComponentUtil.getClientProperty(table, RepaintLinkMouseListenerBase.ACTIVE_TAG));
          int linkStyle = 0;
          if (isActive) {
            linkStyle = 16;
          }
          if (this.myDefault) {
            linkStyle |= 1;
          }
          Color linkColor = isActive ? JBUI.CurrentTheme.Link.Foreground.HOVERED : JBUI.CurrentTheme.Link.Foreground.ENABLED;
          pathAttributes = new SimpleTextAttributes(linkStyle, linkColor);
        }
        else {
          pathAttributes = this.myDefault ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES;
        }
        String mappingPath = mapping.getPath();
        append(mappingPath, pathAttributes, linkListener);
        String requestMethods = getDisplayRequestMethods(mapping);
        if (requestMethods != null) {
          append(requestMethods, this.myDefault ? SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES : SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
      }
      else if (value instanceof String) {
        append((String) value, this.myDefault ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);
      }
      if (!selected && this.myFile != null) {
        setBackground(VfsPresentationUtil.getFileBackgroundColor(this.myProject, this.myFile));
      }
      SpeedSearchUtil.applySpeedSearchHighlighting(table, this, true, selected);
    }
  }

  private static class LiveRequestMappingItem {
    private final LiveRequestMapping myMapping;
    private VirtualFile myContainingFile;

    LiveRequestMappingItem(LiveRequestMapping mapping, @Nullable VirtualFile containingFile) {
      this.myMapping = mapping;
      this.myContainingFile = containingFile;
    }

    LiveRequestMapping getMapping() {
      return this.myMapping;
    }

    VirtualFile getContainingFile() {
      return this.myContainingFile;
    }

    void setContainingFile(@Nullable VirtualFile containingFile) {
      this.myContainingFile = containingFile;
    }
  }

  private static class RequestMappingLinkMouseListener extends RepaintLinkMouseListenerBase<PathLinkListener> {

    protected void repaintComponent(MouseEvent e) {
      JTable table = (JTable) e.getSource();
      int row = table.rowAtPoint(e.getPoint());
      int column = table.columnAtPoint(e.getPoint());
      if (row == -1 || column == -1) {
        table.repaint();
      }
      else {
        ((AbstractTableModel) table.getModel()).fireTableCellUpdated(row, column);
      }
    }

    @Nullable
    public PathLinkListener getTagAt(MouseEvent e) {
      JTable table = (JTable) e.getSource();
      int row = table.rowAtPoint(e.getPoint());
      int column = table.columnAtPoint(e.getPoint());
      if (row == -1 || column == -1) {
        return null;
      }
      Object tag = null;
      TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
      if (cellRenderer instanceof ColoredTableCellRenderer renderer) {
        renderer.getTableCellRendererComponent(table, table.getValueAt(row, column), false, false, row, column);
        Rectangle rectangle = table.getCellRect(row, column, false);
        tag = renderer.getFragmentTagAt(e.getX() - rectangle.x);
      }
      if (tag instanceof PathLinkListener) {
        return (PathLinkListener) tag;
      }
      return null;
    }

    public void handleTagClick(@Nullable PathLinkListener tag, MouseEvent e) {
      if (tag != null) {
        tag.navigate(e);
      }
    }
  }

  private static class PathLinkListener {
    private final LiveRequestMapping myMapping;
    private final LiveRequestMappingsNavigationHandler myNavigationHandler;

    PathLinkListener(LiveRequestMapping mapping, LiveRequestMappingsNavigationHandler navigationHandler) {
      this.myMapping = mapping;
      this.myNavigationHandler = navigationHandler;
    }

    void navigate(MouseEvent e) {
      this.myNavigationHandler.navigate(e, null);
    }

    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o != null && getClass() == o.getClass()) {
        return this.myMapping.equals(((PathLinkListener) o).myMapping);
      }
      return false;
    }

    public int hashCode() {
      return this.myMapping.hashCode();
    }
  }
}
