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

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.DataInputOutputUtil;
import com.intellij.util.io.IOUtil;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.assistant.dom.InfraCustomNamespaces;
import cn.taketoday.assistant.factories.FactoryBeansManager;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.utils.search.BeanSearchParameters;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Alias;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntProcedure;

public class InfraXmlBeansIndex extends FileBasedIndexExtension<Pair<InfraBeanIndexType, String>, TIntArrayList> {

  private static final int INDEX_VERSION = 27;
  public static final ID<Pair<InfraBeanIndexType, String>, TIntArrayList> NAME = ID.create("XmlBeansIndex");
  private final DataIndexer<Pair<InfraBeanIndexType, String>, TIntArrayList, FileContent> myIndexer = InfraIndexUtil::indexFile;
  private final DataExternalizer<TIntArrayList> myValueExternalizer = new DataExternalizer<>() {

    public void save(DataOutput out, TIntArrayList values) throws IOException {
      int size = values.size();
      if (size == 1) {
        DataInputOutputUtil.writeINT(out, -values.getQuick(0));
        return;
      }
      DataInputOutputUtil.writeINT(out, size);
      for (int i = 0; i < size; i++) {
        DataInputOutputUtil.writeINT(out, values.getQuick(i));
      }
    }

    public TIntArrayList read(DataInput in) throws IOException {
      int countOrSingleValue = DataInputOutputUtil.readINT(in);
      if (countOrSingleValue < 0) {
        TIntArrayList result = new TIntArrayList(1);
        result.add(-countOrSingleValue);
        return result;
      }
      TIntArrayList result2 = new TIntArrayList(countOrSingleValue);
      for (int i = 0; i < countOrSingleValue; i++) {
        result2.add(DataInputOutputUtil.readINT(in));
      }
      return result2;
    }
  };

  public ID<Pair<InfraBeanIndexType, String>, TIntArrayList> getName() {
    return NAME;
  }

  public DataIndexer<Pair<InfraBeanIndexType, String>, TIntArrayList, FileContent> getIndexer() {
    return myIndexer;
  }

  public KeyDescriptor<Pair<InfraBeanIndexType, String>> getKeyDescriptor() {
    return new KeyDescriptor<>() {

      public void save(DataOutput out, Pair<InfraBeanIndexType, String> pair) throws IOException {
        DataInputOutputUtil.writeINT(out, pair.first.ordinal());
        if (pair.first.key() == null) {
          IOUtil.writeUTF(out, pair.second);
        }
      }

      public Pair<InfraBeanIndexType, String> read(DataInput in) throws IOException {
        InfraBeanIndexType type = InfraBeanIndexType.values()[DataInputOutputUtil.readINT(in)];
        if (type.key() != null) {
          return type.key();
        }
        String value = IOUtil.readUTF(in);
        return Pair.create(type, value);
      }

      public int getHashCode(Pair<InfraBeanIndexType, String> value) {
        InfraBeanIndexType indexType = value.first;
        String fqn = value.second;
        return (31 * indexType.ordinal()) + (fqn != null ? fqn.hashCode() : 0);
      }

      public boolean isEqual(Pair<InfraBeanIndexType, String> val1, Pair<InfraBeanIndexType, String> val2) {
        if (!val1.first.equals(val2.first)) {
          return false;
        }
        if (val1.first.key() != null) {
          return true;
        }
        return Objects.equals(val1.second, val2.second);
      }
    };
  }

  public DataExternalizer<TIntArrayList> getValueExternalizer() {
    return myValueExternalizer;
  }

  public FileBasedIndex.InputFilter getInputFilter() {
    return new DefaultFileTypeSpecificInputFilter(XmlFileType.INSTANCE);
  }

  public boolean dependsOnFileContent() {
    return true;
  }

  public int getVersion() {
    int customNamespacesVersion = 0;
    for (InfraCustomNamespaces customNamespaces : InfraCustomNamespaces.EP_NAME.getExtensionList()) {
      customNamespacesVersion = customNamespacesVersion + customNamespaces.getModelVersion() + customNamespaces.getClass().getName().length();
    }
    return INDEX_VERSION + customNamespacesVersion + FactoryBeansManager.getIndexingVersion();
  }

  public static boolean processBeansByClass(BeanSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> processor) {
    PsiClass psiClass = PsiTypesUtil.getPsiClass(params.getPsiType());
    if (psiClass == null) {
      return true;
    }
    String psiClassFqn = Objects.requireNonNull(psiClass.getQualifiedName());
    boolean byBeanClass = processSpringBeanPointer(Pair.create(InfraBeanIndexType.BEAN_CLASS, psiClassFqn), params, processor);
    if (!byBeanClass) {
      return false;
    }
    SmartList<BeanPointer<?>> smartList = new SmartList<>();
    processSpringBeanPointer(InfraBeanIndexType.BEAN_TYPE_PROVIDER.key(), params, new CommonProcessors.CollectProcessor<>(smartList));
    if (smartList.isEmpty()) {
      return true;
    }
    return ContainerUtil.process(smartList, (BeanPointer<?> pointer) -> {
      if (!Comparing.equal(psiClass, pointer.getBeanClass())) {
        return true;
      }
      return processor.process(pointer);
    });
  }

  public static boolean processBeansByName(BeanSearchParameters.BeanName params, Processor<? super BeanPointer<?>> processor) {
    String beanName = params.getBeanName();
    boolean processByName = processSpringBeanPointer(Pair.create(InfraBeanIndexType.BEAN_NAME, beanName), params, processor);
    if (!processByName) {
      return false;
    }
    SmartList<BeanPointer<?>> smartList = new SmartList<>();
    processSpringBeanPointer(InfraBeanIndexType.BEAN_NAME_PROVIDER.key(), params, new CommonProcessors.CollectProcessor<>(smartList));
    if (smartList.isEmpty()) {
      return true;
    }
    return ContainerUtil.process(smartList, (BeanPointer<?> pointer) -> {
      if (!Objects.equals(beanName, pointer.getName())) {
        return true;
      }
      return processor.process(pointer);
    });
  }

  public static boolean processComponentScans(BeanSearchParameters.BeanName params, Processor<? super BeanPointer<?>> processor) {
    return processSpringBeanPointer(InfraBeanIndexType.COMPONENT_SCAN.key(), params, processor);
  }

  public static boolean processFactoryBeans(BeanSearchParameters params, Processor<? super BeanPointer<?>> processor) {
    return processSpringBeanPointer(InfraBeanIndexType.FACTORY_BEAN.key(), params, processor);
  }

  public static boolean processFactoryBeanClasses(BeanSearchParameters params, Processor<? super BeanPointer<?>> processor) {
    return processSpringBeanPointer(InfraBeanIndexType.FACTORY_BEAN_CLASS.key(), params, processor);
  }

  public static boolean processFactoryMethods(BeanSearchParameters params, Processor<? super BeanPointer<?>> processor) {
    return processSpringBeanPointer(InfraBeanIndexType.FACTORY_METHOD.key(), params, processor);
  }

  public static boolean processAbstractBeans(BeanSearchParameters params, Processor<? super BeanPointer<?>> processor) {
    SmartList<BeanPointer<?>> smartList = new SmartList<>();
    processSpringBeanPointer(InfraBeanIndexType.ABSTRACT_BEAN.key(), params, new CommonProcessors.CollectProcessor<>(smartList));
    return ContainerUtil.process(smartList, processor);
  }

  public static boolean processAliases(BeanSearchParameters.BeanName params, final Processor<? super Alias> processor) {
    Pair<InfraBeanIndexType, String> key = Pair.create(InfraBeanIndexType.ALIAS, params.getBeanName());
    MultiMap<VirtualFile, TIntArrayList> results = getResults(key, params);
    if (results.isEmpty()) {
      return true;
    }
    DomElementProcessor domElementProcessor = new DomElementProcessor() {

      @Override
      protected boolean processDomElement(DomElement domElement) {
        return !(domElement instanceof Alias) || processor.process((Alias) domElement);
      }
    };
    return processDomElements(params.getProject(), results, domElementProcessor);
  }

  public static boolean processCustomBeans(BeanSearchParameters params, final Processor<? super CustomBeanWrapper> processor) {
    Pair<InfraBeanIndexType, String> key = InfraBeanIndexType.CUSTOM_BEAN_WRAPPER.key();
    MultiMap<VirtualFile, TIntArrayList> results = getResults(key, params);
    if (results.isEmpty()) {
      return true;
    }
    DomElementProcessor domElementProcessor = new DomElementProcessor() {
      @Override
      protected boolean processDomElement(DomElement domElement) {
        return !(domElement instanceof CustomBeanWrapper) || processor.process((CustomBeanWrapper) domElement);
      }
    };
    return processDomElements(params.getProject(), results, domElementProcessor);
  }

  private static boolean processSpringBeanPointer(Pair<InfraBeanIndexType, String> key, BeanSearchParameters params,
          final Processor<? super BeanPointer<?>> processor) {
    MultiMap<VirtualFile, TIntArrayList> results = getResults(key, params);
    if (results.isEmpty()) {
      return true;
    }
    DomElementProcessor domElementProcessor = new DomElementProcessor() {

      @Override
      protected boolean processDomElement(DomElement domElement) {
        if (domElement instanceof DomInfraBean) {
          BeanPointer<?> pointer = InfraBeanService.of().createBeanPointer((DomInfraBean) domElement);
          return processor.process(pointer);
        }
        return true;
      }
    };
    return processDomElements(params.getProject(), results, domElementProcessor);
  }

  private static MultiMap<VirtualFile, TIntArrayList> getResults(Pair<InfraBeanIndexType, String> key, BeanSearchParameters params) {
    MultiMap<VirtualFile, TIntArrayList> results = new MultiMap<>();
    FileBasedIndex.getInstance().processValues(NAME, key, params.getVirtualFile(), (file, value) -> {
      results.putValue(file, value);
      return true;
    }, params.getSearchScope());
    return results;
  }

  private static boolean processDomElements(Project project, MultiMap<VirtualFile, TIntArrayList> indexMap, DomElementProcessor processor) {
    PsiManager psiManager = PsiManager.getInstance(project);
    DomManager domManager = DomManager.getDomManager(project);
    for (Map.Entry<VirtualFile, Collection<TIntArrayList>> entry : indexMap.entrySet()) {
      VirtualFile file = entry.getKey();
      PsiFile psiFile = psiManager.findFile(file);
      if (psiFile instanceof XmlFile) {
        TIntProcedure procedure = value -> {
          PsiElement psiElement = psiFile.findElementAt(value);
          XmlTag xmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class, false);
          DomElement domElement = domManager.getDomElement(xmlTag);
          if (domElement == null) {
            return true;
          }
          return processor.processDomElement(domElement);
        };
        for (TIntArrayList values : entry.getValue()) {
          if (!values.forEach(procedure)) {
            return false;
          }
        }
        continue;
      }
    }
    return true;
  }

  public static abstract class DomElementProcessor {
    protected abstract boolean processDomElement(DomElement domElement);

    private DomElementProcessor() {
    }
  }
}
