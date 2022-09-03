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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.reference.SoftReference;
import com.intellij.util.CommonProcessors;
import com.intellij.xml.util.PsiElementPointer;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBean;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveResource;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.lang.Nullable;

final class LiveBeanImpl implements LiveBean {
  private static final String BEAN_RESOURCE_PREFIX = "/WEB-INF/classes/";
  private static final String CONFIGURATION_PROPERTIES_BEAN = "CONFIGURATION_PROPERTIES";
  private static final Pattern CLASS_NAME_START_PATTERN = Pattern.compile("\\.[A-Z]");

  private final String myId;
  @Nullable
  private final String myScope;
  @Nullable
  private final String myType;
  @Nullable
  private final LiveResource myResource;
  private final Set<LiveBean> myDependencies;
  private final Set<LiveBean> myInjectedInto;
  private final boolean myInnerBean;
  private volatile String myName;
  private volatile String myClassName;
  private volatile WeakReference<PsiClass> myClassRef;
  private volatile WeakReference<BeanPointer<?>> myBeanPointerRef;

  static LiveBeanImpl createLiveBean(String id, @Nullable String scope, @Nullable String type, LiveResource resource) {
    return new LiveBeanImpl(id, scope, type, resource, false);
  }

  static LiveBeanImpl createInnerBean(String id) {
    return new LiveBeanImpl(id, null, null, null, true);
  }

  private LiveBeanImpl(String id, @Nullable String scope, @Nullable String type, @Nullable LiveResource resource, boolean innerBean) {
    this.myDependencies = new HashSet();
    this.myInjectedInto = new HashSet();
    this.myId = id;
    this.myScope = scope;
    this.myType = type;
    this.myResource = resource;
    this.myInnerBean = innerBean;
  }

  @Override

  public String getId() {
    String str = this.myId;
    return str;
  }

  @Override
  @Nullable
  public String getScope() {
    return this.myScope;
  }

  @Override
  @Nullable
  public String getType() {
    return this.myType;
  }

  @Override
  @Nullable
  public LiveResource getResource() {
    return this.myResource;
  }

  @Override

  public Set<LiveBean> getDependencies() {
    Set<LiveBean> unmodifiableSet = Collections.unmodifiableSet(this.myDependencies);
    return unmodifiableSet;
  }

  @Override

  public Set<LiveBean> getInjectedInto() {
    Set<LiveBean> unmodifiableSet = Collections.unmodifiableSet(this.myInjectedInto);
    return unmodifiableSet;
  }

  @Override
  public boolean isInnerBean() {
    return this.myInnerBean;
  }

  @Override

  public String getName() {
    String name = this.myName;
    if (name != null) {
      return name;
    }
    else if (this.myId.endsWith(CONFIGURATION_PROPERTIES_BEAN) || this.myId.contains("-")) {
      this.myName = this.myId;
      String str = this.myName;
      return str;
    }
    else {
      String name2 = dropPackagePrefix(this.myId).replaceAll("\\$", ".");
      int index = name2.indexOf(64);
      if (index > 0) {
        name2 = name2.substring(0, index);
      }
      this.myName = StringUtil.decapitalize(name2);
      String str2 = this.myName;
      return str2;
    }
  }

  private String dropPackagePrefix(String name) {
    Matcher matcher = CLASS_NAME_START_PATTERN.matcher(name);
    if (!matcher.find()) {
      return name;
    }
    int nameIndex = matcher.start();
    if (nameIndex < 0 || nameIndex >= name.length() - 1) {
      return name;
    }
    String className = name.substring(nameIndex + 1);
    int subClassNameCandidateIndex = StringUtil.indexOfAny(className, ".$@");
    String subClassNameCandidate = subClassNameCandidateIndex >= 0 ? className.substring(0, subClassNameCandidateIndex) : className;
    String beanClassName = getClassName();
    if (!beanClassName.contains("$" + subClassNameCandidate)) {
      return className;
    }
    int nameIndex2 = StringUtil.lastIndexOf(name, '.', 0, nameIndex);
    if (nameIndex2 >= 0 && nameIndex2 < name.length() - 1) {
      name = name.substring(nameIndex2 + 1);
    }
    return name;
  }

  @Override

  public String getClassName() {
    String className = this.myClassName;
    if (className != null) {
      return className;
    }
    String className2 = this.myType;
    if (className2 != null && className2.trim().length() > 0) {
      if (className2.startsWith("com.sun.proxy") && this.myResource != null && this.myResource.hasDescription()) {
        String resourcePath = this.myResource.getResourcePath();
        if (LiveResourceImpl.isClassResource(resourcePath)) {
          className2 = extractClassName(resourcePath);
        }
      }
    }
    else {
      className2 = this.myId;
    }
    this.myClassName = cleanClassName(className2);
    String str = this.myClassName;
    return str;
  }

  @Override
  @Nullable
  public PsiClass findBeanClass(Project project, GlobalSearchScope searchScope) {
    String className = getClassName();
    PsiClass psiClass = SoftReference.dereference(this.myClassRef);
    if (psiClass != null && psiClass.isValid() && matches(psiClass)) {
      return psiClass;
    }
    PsiClass psiClass2 = LiveResourceImpl.findClass(className, project, searchScope);
    this.myClassRef = new WeakReference<>(psiClass2);
    return psiClass2;
  }

  @Override
  public boolean matches(PsiClass psiClass) {
    String className = getClassName();
    return StringUtil.replace(className, "$", ".").equals(psiClass.getQualifiedName());
  }

  @Override
  public PsiElementPointer findBeanPointer(@Nullable PsiClass beanClass, @Nullable PsiElement resourceElement, @Nullable CommonInfraModel springModel) {
    InfraModel localModel;
    BeanPointer<?> pointer = SoftReference.dereference(this.myBeanPointerRef);
    if (pointer != null && pointer.isValid() && getName().equals(pointer.getBean().getBeanName())) {
      return pointer;
    }
    PsiElementPointer psiElementPointer = null;
    PsiFile beanFile = resourceElement == null ? null : resourceElement.getContainingFile();
    PsiClass beanContainingClass = getBeanContainingClass(resourceElement);
    CommonProcessors.FindProcessor<BeanPointer<?>> findProcessor = new CommonProcessors.FindProcessor<BeanPointer<?>>() {
      public boolean accept(BeanPointer pointer2) {
        if (beanFile == null) {
          return true;
        }
        boolean accept = beanFile.getManager().areElementsEquivalent(beanFile, pointer2.getContainingFile());
        if (!accept) {
          accept = InheritanceUtil.isInheritorOrSelf(beanContainingClass, getBeanContainingClass(pointer2.getPsiElement()), true);
        }
        return accept;
      }
    };
    PsiFile localModelConfigFile = beanFile;
    if (localModelConfigFile == null && ((this.myResource == null || !this.myResource.hasDescription()) && beanClass != null)) {
      localModelConfigFile = beanClass.getContainingFile();
    }
    if (localModelConfigFile != null && (localModel = InfraManager.from(localModelConfigFile.getProject())
            .getInfraModelByFile(localModelConfigFile)) != null) {
      localModel.processByName(ModelSearchParameters.byName(getName()), findProcessor);
      psiElementPointer = findProcessor.getFoundValue();
    }
    if (psiElementPointer == null && springModel != null) {
      springModel.processByName(ModelSearchParameters.byName(getName()), findProcessor);
      psiElementPointer = findProcessor.getFoundValue();
    }
    this.myBeanPointerRef = new WeakReference<>(pointer);
    if (psiElementPointer != null) {
      return psiElementPointer;
    }
    else if (this.myResource != null && this.myResource.hasDescription()) {
      return getLiveResourcePointer(beanContainingClass != null ? beanContainingClass : beanFile);
    }
    else {
      return () -> beanClass;
    }
  }

  @Override
  public Icon getIcon() {
    if (this.myResource == null || !this.myResource.hasDescription() || LiveResourceImpl.isClassNameResource(this.myResource.getDescription()) || !LiveResourceImpl.isXmlResource(
            this.myResource.getResourcePath())) {
      return Icons.SpringJavaBean;
    }
    Icon icon2 = Icons.SpringBean;
    return icon2;
  }

  public void addDependency(LiveBeanImpl dependency) {
    this.myDependencies.add(dependency);
    dependency.myInjectedInto.add(this);
  }

  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof LiveBeanImpl liveBean) {
      return this.myId.equals(liveBean.myId) && Comparing.equal(this.myResource, liveBean.myResource);
    }
    return false;
  }

  public int hashCode() {
    int result = (31 * 17) + this.myId.hashCode();
    return (31 * result) + (this.myResource != null ? this.myResource.hashCode() : 0);
  }

  public String toString() {
    return this.myId;
  }

  private static String cleanClassName(String className) {
    int index = className.indexOf("$$");
    if (index > 0) {
      className = className.substring(0, index);
    }
    int index2 = className.indexOf(35);
    if (index2 > 0) {
      className = className.substring(0, index2);
    }
    int index3 = className.indexOf(64);
    if (index3 > 0) {
      className = className.substring(0, index3);
    }
    return className;
  }

  private static String extractClassName(String resourcePath) {
    int index = resourcePath.lastIndexOf(BEAN_RESOURCE_PREFIX);
    if (index >= 0) {
      resourcePath = resourcePath.substring(index + BEAN_RESOURCE_PREFIX.length());
    }
    return StringUtil.trimEnd(resourcePath, ".class", true).replaceAll("[\\\\/]", ".");
  }

  @Nullable
  private static PsiClass getBeanContainingClass(@Nullable PsiElement element) {
    if (element == null) {
      return null;
    }
    if (element instanceof PsiClass) {
      return (PsiClass) element;
    }
    if (element instanceof PsiClassOwner) {
      PsiClass[] classes = ((PsiClassOwner) element).getClasses();
      if (classes.length == 0) {
        return null;
      }
      return classes[0];
    }
    return PsiTreeUtil.getParentOfType(element, PsiClass.class);
  }

  private static LiveBean.LiveResourcePointer getLiveResourcePointer(@Nullable PsiElement element) {
    return new LiveResourcePointer() {
      @Nullable
      public PsiElement getPsiElement() {
        return element;
      }
    };
  }
}
