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

package cn.taketoday.assistant.app.run.lifecycle.health.tab;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTreeStructure;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.app.run.InfraRunBundle;

class HealthTreeStructure extends SimpleTreeStructure {
  static final String STATUS_UP = "UP";
  static final String STATUS_DOWN = "DOWN";
  static final String STATUS_OUT_OF_SERVICE = "OUT_OF_SERVICE";
  static final String STATUS_UNKNOWN = "UNKNOWN";
  static final String APPLICATION_NODE_NAME = "application";
  static final String STATUS_KEY = "status";
  static final String DETAILS_KEY = "details";
  static final String COMPONENTS_KEY = "components";

  private final SimpleNode myRootNode;
  private StatusNode myApplicationStatusNode;

  HealthTreeStructure(Project project) {
    this.myRootNode = new SimpleNode(project) {

      public SimpleNode[] getChildren() {
        if (HealthTreeStructure.this.myApplicationStatusNode == null) {
          return NO_CHILDREN;
        }
        return new SimpleNode[] { HealthTreeStructure.this.myApplicationStatusNode };
      }
    };
  }

  public Object getRootElement() {
    return this.myRootNode;
  }

  public boolean isToBuildChildrenInBackground(Object element) {
    return true;
  }

  void setHealth(Map health) {
    if (health == null) {
      this.myApplicationStatusNode = null;
      return;
    }
    if (health.size() == 2) {
      Object applicationHealthIndicator = health.get("application");
      if (applicationHealthIndicator instanceof Map) {
        this.myApplicationStatusNode = getDisabledIndicatorsApplicationStatusNode((Map) applicationHealthIndicator);
        return;
      }
      Object healthDetails = health.get(DETAILS_KEY);
      if (healthDetails == null) {
        healthDetails = health.get(COMPONENTS_KEY);
      }
      if (healthDetails instanceof Map) {
        Map<?, ?> details = (Map) healthDetails;
        if (details.size() == 1) {
          Object applicationHealthIndicator2 = details.get("application");
          if (applicationHealthIndicator2 instanceof Map) {
            this.myApplicationStatusNode = getDisabledIndicatorsApplicationStatusNode((Map) applicationHealthIndicator2);
            return;
          }
        }
      }
    }
    this.myApplicationStatusNode = new StatusNode(this.myRootNode.getProject(), this.myRootNode, "application", health);
  }

  private StatusNode getDisabledIndicatorsApplicationStatusNode(Map<?, ?> applicationHealthIndicator) {
    return new StatusNode(this.myRootNode.getProject(), this.myRootNode, "application", applicationHealthIndicator,
            InfraRunBundle.message("infra.application.endpoints.health.indicators.disabled"));
  }

  private static class StatusNode extends SimpleNode {
    private final Map<Object, Object> myDetails;
    private SimpleNode[] myChildren;

    StatusNode(Project project, SimpleNode parent, String name, Map<?, ?> details) {
      this(project, parent, name, details, null);
    }

    StatusNode(Project project, SimpleNode parent, String name, Map<?, ?> details, String tailText) {
      super(project, parent);
      this.myName = name;
      this.myDetails = new HashMap();
      details.forEach((key, value) -> {
        if ((DETAILS_KEY.equals(key) || COMPONENTS_KEY.equals(key)) && (value instanceof Map)) {
          this.myDetails.putAll((Map) value);
        }
        else {
          this.myDetails.put(key, value);
        }
      });
      PresentationData presentationData = getTemplatePresentation();
      presentationData.addText(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);
      Object status = details.get(STATUS_KEY);
      if (status instanceof String) {
        presentationData.setTooltip(InfraRunBundle.message("infra.application.endpoints.health.status", status));
        if (STATUS_UP.equals(status)) {
          setIcon(AllIcons.RunConfigurations.TestPassed);
        }
        else if (STATUS_DOWN.equals(status)) {
          setIcon(AllIcons.RunConfigurations.TestFailed);
        }
        else if (STATUS_OUT_OF_SERVICE.equals(status)) {
          setIcon(AllIcons.RunConfigurations.TestError);
        }
        else if (STATUS_UNKNOWN.equals(status)) {
          setIcon(AllIcons.RunConfigurations.TestUnknown);
        }
        else {
          presentationData.addText(" [" + status + "]", SimpleTextAttributes.GRAY_ATTRIBUTES);
          setIcon(AllIcons.RunConfigurations.TestCustom);
        }
      }
      if (tailText != null) {
        presentationData.addText(" [" + tailText + "]", SimpleTextAttributes.GRAY_ATTRIBUTES);
      }
    }

    public SimpleNode[] getChildren() {
      if (this.myChildren != null) {
        return this.myChildren;
      }
      List<SimpleNode> children = new ArrayList<>();
      this.myDetails.forEach((key, value) -> {
        if (children == null || value == null || STATUS_KEY.equals(children)) {
          return;
        }
        if ((value instanceof Map) && !((Map) value).isEmpty()) {
          children.add(new StatusNode(getProject(), this, children.toString(), (Map) value));
        }
        else {
          children.add(new DetailsNode(getProject(), this, children.toString(), value));
        }
      });
      children.sort((o1, o2) -> {
        if (o1 instanceof StatusNode) {
          if (o2 instanceof DetailsNode) {
            return 1;
          }
          return StringUtil.naturalCompare(o1.getName(), o2.getName());
        }
        else if (o2 instanceof StatusNode) {
          return -1;
        }
        else {
          return StringUtil.naturalCompare(o1.getName(), o2.getName());
        }
      });
      this.myChildren = children.toArray(NO_CHILDREN);
      return this.myChildren;
    }

    public Object[] getEqualityObjects() {
      return new Object[] { this.myName, getParent() };
    }

    public boolean isAutoExpandNode() {
      SimpleNode parent = getParent();
      if (!(parent instanceof StatusNode)) {
        return true;
      }
      return !(parent.getParent() instanceof StatusNode) && parent.getChildCount() == 1;
    }
  }

  private static class DetailsNode extends SimpleNode {

    DetailsNode(Project project, SimpleNode parent, String key, Object value) {
      super(project, parent);
      this.myName = key;
      PresentationData presentationData = getTemplatePresentation();
      presentationData.addText(key + ": ", SimpleTextAttributes.GRAY_ATTRIBUTES);
      String valueString = value.toString();
      if (value instanceof Number) {
        try {
          valueString = NumberFormat.getInstance().format(value);
        }
        catch (IllegalArgumentException ignored) { }
      }
      presentationData.addText(valueString, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    public SimpleNode[] getChildren() {
      return NO_CHILDREN;
    }

    public boolean isAlwaysLeaf() {
      return true;
    }

    public Object[] getEqualityObjects() {
      return new Object[] { this.myName, getParent() };
    }
  }
}
