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
package cn.taketoday.assistant.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.BitUtil;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;

import java.util.Map;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

/**
 * Defines search parameters for querying Spring model for matching bean(s).
 *
 * @see InfraModelSearchers
 * @see CommonInfraModel#processByClass(BeanClass, Processor)
 * @see CommonInfraModel#processByName(BeanName, Processor)
 */
public abstract class ModelSearchParameters {

  private static final Logger LOG = Logger.getInstance(ModelSearchParameters.class);

  protected ModelSearchParameters() { }

  /**
   * Returns whether given search parameters are valid to start a search.
   *
   * @return {@code false} if search cannot be performed.
   */
  public abstract boolean canSearch();

  public static BeanClass byClass(PsiClass psiClass) {
    return new BeanClass(psiClass);
  }

  public static BeanClass byType(PsiType psiType) {
    return new BeanClass(psiType);
  }

  public static final class BeanClass extends ModelSearchParameters {
    private final PsiType myType;

    private byte myOptions;

    private static final byte CAN_SEARCH = 1;
    private static final byte WITH_INHERITORS = 2;
    private static final byte EFFECTIVE_BEAN_TYPES = 4;

    private BeanClass(PsiClass psiClass) {
      myType = JavaPsiFacade.getElementFactory(psiClass.getProject()).createType(psiClass, getSubstitutor(psiClass));
      myOptions = BitUtil.set(myOptions, CAN_SEARCH, InfraUtils.isBeanCandidateClass(psiClass));

      assertSearchClass(psiClass);
    }

    private static PsiSubstitutor getSubstitutor(PsiClass psiClass) {
      return psiClass.isValid() ? JavaPsiFacade.getInstance(psiClass.getProject()).getElementFactory().createRawSubstitutor(psiClass) : PsiSubstitutor.EMPTY;
    }

    private BeanClass(PsiType psiType) {
      myType = psiType;
      PsiClass searchClass = PsiTypesUtil.getPsiClass(psiType);
      myOptions = BitUtil.set(myOptions, CAN_SEARCH, searchClass == null || InfraUtils.isBeanCandidateClass(searchClass));

      assertSearchClass(searchClass);
    }

    private void assertSearchClass(@Nullable PsiClass psiClass) {
      if (psiClass instanceof PsiAnonymousClass) {
        LOG.error("cannot search for anonymous class: " + psiClass + " " + myType.getClass() + ": " + myType.getCanonicalText());
      }
      if (psiClass != null && psiClass.getQualifiedName() == null) {
        LOG.error("cannot search for null FQN class: " + psiClass + " " + myType.getClass() + ": " + myType.getCanonicalText());
      }
    }

    @Override
    public boolean canSearch() {
      return BitUtil.isSet(myOptions, CAN_SEARCH);
    }

    public BeanClass withInheritors() {
      myOptions = BitUtil.set(myOptions, WITH_INHERITORS, true);
      return this;
    }

    public BeanClass effectiveBeanTypes() {
      myOptions = BitUtil.set(myOptions, EFFECTIVE_BEAN_TYPES, true);
      return this;
    }

    public PsiType getSearchType() {
      return myType;
    }

    public boolean isWithInheritors() {
      return BitUtil.isSet(myOptions, WITH_INHERITORS);
    }

    public boolean isEffectiveBeanTypes() {
      return BitUtil.isSet(myOptions, EFFECTIVE_BEAN_TYPES);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof BeanClass beanClass))
        return false;

      if (myOptions != beanClass.myOptions)
        return false;
      if (!myType.isValid() || !beanClass.myType.isValid())
        return false;
      return myType.equals(beanClass.myType);
    }

    @Override
    public int hashCode() {
      int result = myType.hashCode();
      result = 31 * result + myOptions;
      return result;
    }

    @Override
    public String toString() {
      return "ModelSearchParameters.BeanClass{" +
              "myType=" + (myType.isValid() ? myType : "INVALID") +
              ", myOptions=" + myOptions +
              '}';
    }
  }

  private static final Map<String, BeanName> ourCachedBeanNames = ContainerUtil.createConcurrentWeakKeyWeakValueMap();

  public static BeanName byName(String beanName) {
    return ourCachedBeanNames.computeIfAbsent(beanName, BeanName::new);
  }

  public static final class BeanName extends ModelSearchParameters {

    private final String myBeanName;

    private BeanName(String beanName) {
      myBeanName = StringUtil.isNotEmpty(beanName) ? beanName : "";
    }

    @Override
    public boolean canSearch() {
      return !myBeanName.isEmpty();
    }

    public String getBeanName() {
      return myBeanName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof BeanName name))
        return false;

      return myBeanName.equals(name.myBeanName);
    }

    @Override
    public int hashCode() {
      return myBeanName.hashCode();
    }
  }
}
