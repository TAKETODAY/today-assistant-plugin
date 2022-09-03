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

package cn.taketoday.assistant.model.structure;

import com.intellij.icons.AllIcons;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.beanProperties.BeanProperty;
import com.intellij.util.xml.DomElementNavigationProvider;
import com.intellij.util.xml.DomElementsNavigationManager;
import com.intellij.util.xml.GenericDomValue;

import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolderDefinition;
import cn.taketoday.lang.Nullable;

public class InfraInjectionTreeElement implements StructureViewTreeElement, ItemPresentation {
  private final InfraValueHolderDefinition myInjection;

  private static final String CONSTRUCTOR_ARG = "constructor-arg";

  private static final String PROPERTY_TAG = "property";

  public InfraInjectionTreeElement(InfraValueHolderDefinition injection) {
    this.myInjection = injection;
  }

  public Object getValue() {
    return this.myInjection.getXmlElement();
  }

  public ItemPresentation getPresentation() {
    return this;
  }

  public String getPresentableText() {
    if (isConstructorArg()) {
      return CONSTRUCTOR_ARG;
    }
    InfraProperty springProperty = (InfraProperty) this.myInjection;
    String propertyName = springProperty.getName().getStringValue();
    if (propertyName == null) {
      propertyName = PROPERTY_TAG;
    }
    PsiType type = getPropertyType(springProperty);
    if (type != null) {
      propertyName = propertyName + ": " + type.getCanonicalText();
    }
    return propertyName;
  }

  @Nullable
  private static PsiType getPropertyType(InfraProperty property) {
    List<BeanProperty> value = property.getName().getValue();
    if (value != null && !value.isEmpty()) {
      return value.get(0).getPropertyType();
    }
    return property.guessTypeByValue();
  }

  public String getLocationString() {
    String value = getValueText();
    if (value != null) {
      return "value=\"" + value + "\"";
    }
    String value2 = getRefText();
    if (value2 == null) {
      return null;
    }
    return "ref=\"" + value2 + "\"";
  }

  public Icon getIcon(boolean open) {
    return isConstructorArg() ? AllIcons.Nodes.Method : Icons.SpringProperty;
  }

  public TreeElement[] getChildren() {
    return EMPTY_ARRAY;
  }

  public void navigate(boolean requestFocus) {
    DomElementNavigationProvider navigationProvider = DomElementsNavigationManager.getManager(this.myInjection.getManager().getProject())
            .getDomElementsNavigateProvider(DomElementsNavigationManager.DEFAULT_PROVIDER_NAME);
    navigationProvider.navigate(this.myInjection, requestFocus);
  }

  public boolean canNavigate() {
    return this.myInjection.isValid();
  }

  public boolean canNavigateToSource() {
    return this.myInjection.isValid();
  }

  public boolean isConstructorArg() {
    return this.myInjection instanceof ConstructorArg;
  }

  @Nullable
  private String getValueText() {
    return this.myInjection.getValueAsString();
  }

  @Nullable
  private String getRefText() {
    GenericDomValue<BeanPointer<?>> refElement = this.myInjection.getRefElement();
    if (refElement != null) {
      return refElement.getStringValue();
    }
    return null;
  }
}
