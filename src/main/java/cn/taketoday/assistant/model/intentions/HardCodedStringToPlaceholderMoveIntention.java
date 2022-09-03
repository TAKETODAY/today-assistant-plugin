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

package cn.taketoday.assistant.model.intentions;

import com.intellij.codeInspection.i18n.JavaCreatePropertyFix;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.jsp.JspFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ResolvedConstructorArgs;
import cn.taketoday.assistant.model.converters.ConstructorArgIndexConverterImpl;
import cn.taketoday.assistant.model.converters.InfraConverterUtil;
import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraEntry;
import cn.taketoday.assistant.model.xml.beans.InfraInjection;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.model.xml.beans.InfraValue;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolder;
import cn.taketoday.assistant.model.xml.beans.Prop;
import cn.taketoday.lang.Nullable;

public class HardCodedStringToPlaceholderMoveIntention extends JavaCreatePropertyFix {
  private static final Map<String, List<String>> myExcludedProperties = new HashMap();
  private static final String[] myEscapes = { ":", "_", "/", "\\", "#", "$", "{", PlaceholderUtils.DEFAULT_PLACEHOLDER_SUFFIX };

  public HardCodedStringToPlaceholderMoveIntention() {
    addExcludedProperties(PlaceholderUtils.PLACEHOLDER_CONFIGURER_CLASS, PlaceholderUtils.PLACEHOLDER_PREFIX_PROPERTY_NAME, PlaceholderUtils.PLACEHOLDER_SUFFIX_PROPERTY_NAME);
  }

  public boolean isAvailable(Project project, Editor editor, PsiFile file) {
    GenericDomValue<?> genericDomValue;
    return (file instanceof XmlFile) && !(file instanceof JspFile) && InfraDomUtils.isInfraXml((XmlFile) file) && (genericDomValue = getValueElement(editor, file)) != null && isAvailable(
            genericDomValue, genericDomValue.getParentOfType(InfraBean.class, false));
  }

  @Nullable
  private static GenericDomValue<?> getValueElement(Editor editor, PsiFile file) {
    DomElement domElement = DomUtil.getDomElement(editor, file);
    if (domElement instanceof GenericDomValue) {
      return (GenericDomValue<?>) domElement;
    }
    return null;
  }

  public void invoke(Project project, Editor editor, PsiFile file) {
    Couple invokeAction;
    GenericDomValue<?> domElement = getValueElement(editor, file);
    if (domElement == null) {
      return;
    }
    Set<BeanPointer<?>> placeholderConfigurerBeans = getPlaceholderConfigurerBeans(domElement);
    if (placeholderConfigurerBeans.size() > 0) {
      String suggestedKey = suggestKey(domElement);
      Set<PropertiesFile> propertiesFiles = PlaceholderUtils.getInstance().getResources(placeholderConfigurerBeans);
      XmlElement element = domElement.getXmlElement();
      if (element == null || (invokeAction = invokeAction(project, file, element, suggestedKey, domElement.getStringValue(), new ArrayList(propertiesFiles))) == null) {
        return;
      }
      String createdKey = (String) invokeAction.getFirst();
      Pair<String, String> pair = getPrefixAndSuffix(placeholderConfigurerBeans);
      WriteAction.run(() -> {
        domElement.setStringValue(pair.first + createdKey + pair.second);
      });
    }
  }

  public String getText() {
    String message = InfraBundle.message("bean.property.extract.name");
    return message;
  }

  public String getFamilyName() {
    String text = getText();
    return text;
  }

  private static boolean isAvailable(GenericDomValue<?> valueElement, @Nullable InfraBean springBean) {
    if (valueElement == null || springBean == null) {
      return false;
    }
    String s = valueElement.getStringValue();
    if (StringUtil.isEmptyOrSpaces(s) || isMultiline(s)) {
      return false;
    }
    Set<BeanPointer<?>> placeholderConfigurerBeans = getPlaceholderConfigurerBeans(valueElement);
    return placeholderConfigurerBeans.size() > 0 && (!(valueElement instanceof InfraValue) || !DomUtil.hasXml(((InfraValue) valueElement).getType())) && !PlaceholderUtils.getInstance()
            .isPlaceholder(s, new ArrayList(placeholderConfigurerBeans)) && !isExcludedProperties(springBean, valueElement);
  }

  private static boolean isExcludedProperties(@Nullable InfraBean springBean, GenericDomValue valueHolder) {
    PsiClass beanClass;
    InfraProperty springProperty;
    if (springBean != null && (beanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType())) != null && myExcludedProperties.get(
            beanClass.getQualifiedName()) != null && (springProperty = valueHolder.getParentOfType(InfraProperty.class, false)) != null) {
      return myExcludedProperties.get(beanClass.getQualifiedName()).contains(springProperty.getName().getStringValue());
    }
    return false;
  }

  @Nullable
  private static String suggestKey(GenericDomValue<?> domElement) {
    String key;
    if ((domElement instanceof Prop) && (key = ((Prop) domElement).getKey().getStringValue()) != null) {
      return key;
    }
    LinkedList<String> keyFragments = new LinkedList<>();
    DomElement parentOfType = domElement.getParentOfType(InfraInjection.class, false);
    while (true) {
      InfraInjection current = (InfraInjection) parentOfType;
      if (current == null) {
        break;
      }
      keyFragments.addFirst(createKeyFragment(current));
      parentOfType = current.getParentOfType(InfraInjection.class, true);
    }
    DomInfraBean topLevelBean = findTopLevelBean(domElement);
    keyFragments.addFirst(createPrefixFromBean(topLevelBean));
    String key2 = StringUtil.join(keyFragments, ".");
    return StringUtil.isEmptyOrSpaces(key2) ? suggestValue(domElement.getStringValue()) : key2;
  }

  @Nullable
  private static String createPrefixFromBean(@Nullable DomInfraBean bean) {
    if (bean != null) {
      String beanName = bean.getBeanName();
      if (beanName != null) {
        return beanName;
      }
      PsiClass beanClass = PsiTypesUtil.getPsiClass(bean.getBeanType());
      if (beanClass != null) {
        Project project = bean.getManager().getProject();
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
        PsiClassType classType = PsiTypesUtil.getClassType(beanClass);
        SuggestedNameInfo nameInfo = styleManager.suggestVariableName(VariableKind.LOCAL_VARIABLE, null, null, classType);
        if (nameInfo.names.length > 0) {
          return nameInfo.names[0];
        }
        return null;
      }
      return null;
    }
    return null;
  }

  @Nullable
  private static DomInfraBean findTopLevelBean(DomElement domElement) {
    DomElement parentOfType = domElement.getParentOfType(DomInfraBean.class, false);
    while (true) {
      DomInfraBean current = (DomInfraBean) parentOfType;
      if (current != null) {
        if (current.getParent() instanceof Beans) {
          return current;
        }
        parentOfType = current.getParentOfType(DomInfraBean.class, true);
      }
      else {
        return null;
      }
    }
  }

  @Nullable
  private static String createKeyFragment(InfraValueHolder holder) {
    InfraBean bean;
    if (holder instanceof InfraProperty) {
      return ((InfraProperty) holder).getName().getStringValue();
    }
    if ((holder instanceof ConstructorArg constructorArg) && (bean = (InfraBean) holder.getParent()) != null) {
      GenericAttributeValue<Integer> index = constructorArg.getIndex();
      PsiParameter parameter = null;
      if (index.getValue() != null) {
        parameter = ConstructorArgIndexConverterImpl.resolve(index, bean);
      }
      else {
        ResolvedConstructorArgs resolvedArgs = bean.getResolvedConstructorArgs();
        PsiMethod resolvedMethod = resolvedArgs.getResolvedMethod();
        if (resolvedMethod != null) {
          parameter = resolvedArgs.getResolvedArgs(resolvedMethod).get(constructorArg);
        }
      }
      if (parameter != null) {
        PsiMethod method = PsiTreeUtil.getParentOfType(parameter, PsiMethod.class);
        if (method instanceof PsiCompiledElement) {
          JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(holder.getManager().getProject());
          SuggestedNameInfo nameInfo = styleManager.suggestVariableName(VariableKind.LOCAL_VARIABLE, null, null, parameter.getType());
          return ArrayUtil.getFirstElement(nameInfo.names);
        }
        return parameter.getName();
      }
    }
    if (holder instanceof InfraEntry entry) {
      String keyValue = entry.getKeyAttr().getStringValue();
      if (keyValue != null) {
        return suggestValue(keyValue);
      }
      String value = entry.getKey().getValue().getStringValue();
      if (value != null) {
        return suggestValue(value);
      }
      return null;
    }
    return null;
  }

  private static String suggestValue(String value) {
    String[] strArr;
    if (value == null) {
      return "";
    }
    for (String myEscape : myEscapes) {
      value = value.replace(myEscape, ".");
    }
    while (value.contains("..")) {
      value = value.replace("..", ".");
    }
    return StringUtil.trimEnd(StringUtil.trimStart(value.trim(), "."), ".");
  }

  private static Pair<String, String> getPrefixAndSuffix(Collection<BeanPointer<?>> springBeans) {
    for (BeanPointer pointer : springBeans) {
      CommonInfraBean bean = pointer.getBean();
      if (bean instanceof DomInfraBean) {
        return PlaceholderUtils.getInstance().getPlaceholderPrefixAndSuffix((DomInfraBean) bean);
      }
    }
    return Pair.create(PlaceholderUtils.DEFAULT_PLACEHOLDER_PREFIX, PlaceholderUtils.DEFAULT_PLACEHOLDER_SUFFIX);
  }

  private static boolean isMultiline(String s) {
    return s.trim().indexOf(10) >= 0;
  }

  private static void addExcludedProperties(String baseClassName, String... properties) {
    myExcludedProperties.put(baseClassName, Arrays.asList(properties));
  }

  private static Set<BeanPointer<?>> getPlaceholderConfigurerBeans(DomElement domElement) {
    InfraModel model = InfraConverterUtil.getSpringModel(domElement);
    return model == null ? Collections.emptySet() : InfraModelVisitorUtils.getPlaceholderConfigurers(model);
  }
}
