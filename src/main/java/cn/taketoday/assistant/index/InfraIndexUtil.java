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

package cn.taketoday.assistant.index;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomReflectionUtil;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.factories.FactoryBeansManager;
import cn.taketoday.assistant.model.xml.BeanName;
import cn.taketoday.assistant.model.xml.BeanType;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Alias;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.context.BeansPackagesScanBean;
import cn.taketoday.lang.Nullable;
import gnu.trove.TIntArrayList;

final class InfraIndexUtil {

  public enum InfraBeanType {
    SIMPLE,
    FACTORY_METHOD,
    FACTORY_BEAN,
    FACTORY_BEAN_CLASS,
    CUSTOM,
    CUSTOM_BEAN_WRAPPER,
    COMPONENT_SCAN,
    ALIAS,
    BEAN_TYPE_PROVIDER,
    ABSTRACT_BEAN,
    BEAN_NAME_PROVIDER
  }

  public static Map<Pair<InfraBeanIndexType, String>, TIntArrayList> indexFile(FileContent content) {
    List<InfraBeanIndexInfo> infos = getAllBeanInfos(content);
    if (infos.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Pair<InfraBeanIndexType, String>, TIntArrayList> valueMap = new HashMap<>();
    for (InfraBeanIndexInfo info : infos) {
      InfraBeanType type = info.getType();
      int offset = info.getOffset();
      if (type == InfraBeanType.ALIAS) {
        addKeyedValue(valueMap, InfraBeanIndexType.ALIAS, info.getId(), offset);
      }
      else {
        addKeyedValue(valueMap, InfraBeanIndexType.BEAN_NAME, info.getId(), offset);
        for (String alias : info.getAliases()) {
          addKeyedValue(valueMap, InfraBeanIndexType.BEAN_NAME, alias, offset);
        }
        if (type == InfraBeanType.SIMPLE || type == InfraBeanType.CUSTOM) {
          addKeyedValue(valueMap, InfraBeanIndexType.BEAN_CLASS, info.getFqn(), offset);
        }
        if (type == InfraBeanType.BEAN_TYPE_PROVIDER) {
          storeValue(valueMap, InfraBeanIndexType.BEAN_TYPE_PROVIDER.key(), offset);
        }
        else if (type == InfraBeanType.BEAN_NAME_PROVIDER) {
          storeValue(valueMap, InfraBeanIndexType.BEAN_NAME_PROVIDER.key(), offset);
        }
        else if (type == InfraBeanType.COMPONENT_SCAN) {
          storeValue(valueMap, InfraBeanIndexType.COMPONENT_SCAN.key(), offset);
        }
        else if (type == InfraBeanType.FACTORY_BEAN) {
          storeValue(valueMap, InfraBeanIndexType.FACTORY_BEAN.key(), offset);
        }
        else if (type == InfraBeanType.FACTORY_BEAN_CLASS) {
          addKeyedValue(valueMap, InfraBeanIndexType.BEAN_CLASS, info.getFqn(), offset);
          storeValue(valueMap, InfraBeanIndexType.FACTORY_BEAN_CLASS.key(), offset);
        }
        else if (type == InfraBeanType.FACTORY_METHOD) {
          storeValue(valueMap, InfraBeanIndexType.FACTORY_METHOD.key(), offset);
        }
        else if (type == InfraBeanType.CUSTOM_BEAN_WRAPPER) {
          storeValue(valueMap, InfraBeanIndexType.CUSTOM_BEAN_WRAPPER.key(), offset);
        }
        else if (type == InfraBeanType.ABSTRACT_BEAN) {
          storeValue(valueMap, InfraBeanIndexType.ABSTRACT_BEAN.key(), offset);
        }
      }
    }
    return valueMap;
  }

  private static void addKeyedValue(Map<Pair<InfraBeanIndexType, String>, TIntArrayList> map, InfraBeanIndexType type, @Nullable String key, int offset) {
    if (key == null) {
      return;
    }
    Pair<InfraBeanIndexType, String> indexKey = Pair.create(type, key);
    storeValue(map, indexKey, offset);
  }

  private static void storeValue(Map<Pair<InfraBeanIndexType, String>, TIntArrayList> map, Pair<InfraBeanIndexType, String> indexKey, int offset) {
    TIntArrayList values = map.get(indexKey);
    if (values == null) {
      values = new TIntArrayList(1);
      map.put(indexKey, values);
    }
    values.add(offset);
  }

  private static List<InfraBeanIndexInfo> getAllBeanInfos(FileContent content) {
    CharSequence text = content.getContentAsText();
    if (CharArrayUtil.indexOf(text, "http://www.springframework.org/", 0) == -1 || (CharArrayUtil.indexOf(text, "<beans", 0) == -1 && CharArrayUtil.indexOf(text, ":beans", 0) == -1)) {
      return Collections.emptyList();
    }
    PsiFile psiFile = content.getPsiFile();
    if (!(psiFile instanceof XmlFile)) {
      return Collections.emptyList();
    }
    DomFileElement<Beans> fileElement = InfraDomUtils.getDomFileElement((XmlFile) psiFile);
    if (fileElement == null) {
      return Collections.emptyList();
    }
    List<InfraBeanIndexInfo> allBeans = new ArrayList<>();
    processChildren(fileElement.getRootElement(), allBeans);
    return allBeans;
  }

  private static void processChildren(DomElement parent, List<InfraBeanIndexInfo> allInfos) {
    List<DomElement> children = DomUtil.getDefinedChildren(parent, true, false);
    for (DomElement element : children) {
      if (element instanceof DomInfraBean) {
        if (element instanceof CustomBeanWrapper) {
          indexCustomBeanWrapper((CustomBeanWrapper) element, allInfos);
        }
        else {
          InfraBeanIndexInfo info = new InfraBeanIndexInfo(getOffset(element));
          allInfos.add(info);
          DomInfraBean domInfraBean = (DomInfraBean) element;
          setBeanName(domInfraBean, info, allInfos);
          setBeanClassName(domInfraBean, info);
          info.setAliases(domInfraBean.getAliases());
          processChildren(element, allInfos);
        }
      }
      else if (element instanceof Alias) {
        String aliasName = ((Alias) element).getAlias().getRawText();
        if (StringUtil.isNotEmpty(aliasName)) {
          InfraBeanIndexInfo info2 = new InfraBeanIndexInfo(getOffset(element));
          info2.setType(InfraBeanType.ALIAS);
          info2.setId(aliasName);
          allInfos.add(info2);
        }
      }
      else {
        processChildren(element, allInfos);
      }
    }
  }

  private static void setBeanName(DomInfraBean bean, InfraBeanIndexInfo existing, List<InfraBeanIndexInfo> allInfos) {
    BeanName beanName = DomReflectionUtil.findAnnotationDFS(bean.getClass(), BeanName.class);
    if (beanName == null) {
      existing.setId(bean.getBeanName());
    }
    else if (beanName.displayOnly()) {
    }
    else {
      String name = beanName.value();
      if (!name.isEmpty()) {
        existing.setId(name);
      }
      else {
        InfraBeanIndexInfo beanNameProviderInfo = new InfraBeanIndexInfo(existing.getOffset());
        beanNameProviderInfo.setType(InfraBeanType.BEAN_NAME_PROVIDER);
        allInfos.add(beanNameProviderInfo);
      }
    }
  }

  private static int getOffset(DomElement element) {
    XmlElement xmlElement = element.getXmlElement();
    return xmlElement.getTextOffset();
  }

  private static void indexCustomBeanWrapper(CustomBeanWrapper wrapper, List<InfraBeanIndexInfo> allBeans) {
    XmlTag tag = wrapper.getXmlTag();
    if (tag != null) {
      InfraBeanIndexInfo info = new InfraBeanIndexInfo(tag.getTextOffset());
      info.setType(InfraBeanType.CUSTOM_BEAN_WRAPPER);
      allBeans.add(info);
    }
  }

  private static void setBeanClassName(DomInfraBean bean, InfraBeanIndexInfo info) {
    if (bean instanceof InfraBean infraBean) {
      GenericAttributeValue<PsiClass> clazzAttrValue = infraBean.getClazz();
      if (DomUtil.hasXml(clazzAttrValue)) {
        String fqn = StringUtil.notNullize(clazzAttrValue.getRawText()).replace('$', '.');
        info.setFqn(fqn);
        if (DomUtil.hasXml(infraBean.getFactoryMethod())) {
          info.setType(InfraBeanType.FACTORY_METHOD);
        }
      }
      else if (DomUtil.hasXml(infraBean.getFactoryBean())) {
        info.setType(InfraBeanType.FACTORY_BEAN);
      }
      else if (DomUtil.hasXml(infraBean.getParentBean())) {
        info.setType(InfraBeanType.ABSTRACT_BEAN);
      }
    }
    else if (bean instanceof BeansPackagesScanBean) {
      info.setType(InfraBeanType.COMPONENT_SCAN);
    }
    else {
      BeanType beanType = DomReflectionUtil.findAnnotationDFS(bean.getClass(), BeanType.class);
      if (beanType != null) {
        String className = beanType.value();
        if (!className.isEmpty()) {
          info.setFqn(className);
          info.setType(InfraBeanType.CUSTOM);
        }
        else {
          info.setType(InfraBeanType.BEAN_TYPE_PROVIDER);
        }
      }
    }
    if (isFactoryBeanCandidateClass(info)) {
      info.setType(InfraBeanType.FACTORY_BEAN_CLASS);
    }
  }

  private static boolean isFactoryBeanCandidateClass(InfraBeanIndexInfo info) {
    String fqn;
    InfraBeanType type = info.getType();
    return (type == InfraBeanType.SIMPLE || type == InfraBeanType.CUSTOM) && (fqn = info.getFqn()) != null && (FactoryBeansManager.of().isKnownBeanFactory(fqn) || StringUtil.endsWith(
            fqn, FactoryBeansManager.FACTORY_BEAN_SUFFIX));
  }

  public static final class InfraBeanIndexInfo {
    private final int offset;
    private String id;
    private String fqn;
    private InfraBeanType myType = InfraBeanType.SIMPLE;
    private String[] aliases = ArrayUtilRt.EMPTY_STRING_ARRAY;

    private InfraBeanIndexInfo(int offset) {
      this.offset = offset;
    }

    private String[] getAliases() {
      return this.aliases;
    }

    private void setAliases(String[] aliases) {
      this.aliases = aliases;
    }

    private int getOffset() {
      return this.offset;
    }

    private String getId() {
      return this.id;
    }

    private void setId(String id) {
      this.id = id;
    }

    private String getFqn() {
      return this.fqn;
    }

    private void setFqn(String fqn) {
      this.fqn = fqn;
    }

    private InfraBeanType getType() {
      return this.myType;
    }

    private void setType(InfraBeanType type) {
      this.myType = type;
    }

    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      InfraBeanIndexInfo info = (InfraBeanIndexInfo) o;
      if (this.myType != info.myType || this.offset != info.offset) {
        return false;
      }
      if (this.id != null) {
        if (!this.id.equals(info.id)) {
          return false;
        }
      }
      else if (info.id != null) {
        return false;
      }
      if (this.fqn != null) {
        if (!this.fqn.equals(info.fqn)) {
          return false;
        }
      }
      else if (info.fqn != null) {
        return false;
      }
      return Arrays.equals(this.aliases, info.aliases);
    }

    public int hashCode() {
      int result = this.offset;
      return (31 * ((31 * ((31 * ((31 * result) + (this.id != null ? this.id.hashCode() : 0))) + (this.aliases != null ? Arrays.hashCode(
              this.aliases) : 0))) + (this.fqn != null ? this.fqn.hashCode() : 0))) + this.myType.hashCode();
    }
  }
}
