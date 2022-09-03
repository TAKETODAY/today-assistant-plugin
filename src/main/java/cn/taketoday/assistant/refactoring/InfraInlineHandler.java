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

package cn.taketoday.assistant.refactoring;

import com.intellij.lang.refactoring.InlineHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;

import java.util.Collection;
import java.util.Set;

import cn.taketoday.assistant.model.BeanPsiTarget;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.CollectionElements;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraElementsHolder;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.model.xml.beans.PNamespaceRefValue;
import cn.taketoday.assistant.model.xml.beans.impl.InfraBeanImpl;

public class InfraInlineHandler implements InlineHandler {
  private static final Logger LOG = Logger.getInstance(InfraInlineHandler.class);

  private static final String PARENT_ATTR = "parent";

  public Settings prepareInlineElement(PsiElement element, Editor editor, boolean invokedOnReference) {
    return () -> false;
  }

  public void removeDefinition(PsiElement element, InlineHandler.Settings settings) {
    CommonInfraBean bean = InfraBeanUtils.of().findBean(element);
    if (bean instanceof DomInfraBean) {
      ((DomInfraBean) bean).undefine();
    }
  }

  public Inliner createInliner(PsiElement element, InlineHandler.Settings settings) {
    if (!(element instanceof PomTargetPsiElement)) {
      return null;
    }
    PomTarget target = ((PomTargetPsiElement) element).getTarget();
    if (!(target instanceof BeanPsiTarget)) {
      return null;
    }
    if (!(((BeanPsiTarget) target).getInfraBean() instanceof DomInfraBean)) {
      return null;
    }
    return new Inliner() {

      public MultiMap<PsiElement, String> getConflicts(PsiReference reference, PsiElement referenced) {
        return null;
      }

      public void inlineUsage(UsageInfo usage, PsiElement referenced) {
        Project project = referenced.getProject();
        DomManager domManager = DomManager.getDomManager(project);
        CommonInfraBean commonBean = InfraBeanUtils.of().findBean(referenced);
        if (!(commonBean instanceof DomInfraBean bean)) {
          return;
        }
        PsiElement psiElement = usage.getElement();
        if (!(psiElement instanceof XmlAttributeValue xmlAttributeValue)) {
          return;
        }
        PsiElement attribute = psiElement.getParent();
        GenericAttributeValue value = domManager.getDomElement((XmlAttribute) attribute);
        assert value != null;
        DomElement parent = value.getParent();
        if (parent instanceof DomInfraBean) {
          String attrName = ((XmlAttribute) attribute).getName();
          if (attrName.equals(PARENT_ATTR)) {
            inlineParent(value, parent);
          }
          if ((value instanceof PNamespaceRefValue) && (parent instanceof InfraBean infraBean)) {
            InfraProperty property = infraBean.addProperty();
            property.getName().setStringValue(((PNamespaceRefValue) value).getPropertyName());
            copyBean(bean, property);
            value.undefine();
            reformat(parent);
          }
        }
        else if (parent instanceof InfraElementsHolder) {
          copyBean(bean, parent);
          value.undefine();
          reformat(parent);
        }
        else {
          DomElement grandParent = parent.getParent();
          if ((grandParent instanceof InfraElementsHolder) || (grandParent instanceof CollectionElements)) {
            copyBean(bean, grandParent);
            parent.undefine();
            reformat(grandParent);
            return;
          }
          LOG.error("Cannot inline " + attribute);
        }
      }
    };
  }

  private static void inlineParent(GenericAttributeValue value, DomElement parent) {
    InfraBean thisBean = (InfraBean) parent;
    mergeValue(thisBean, thisBean.getScope());
    mergeValue(thisBean, thisBean.getLazyInit());
    mergeValue(thisBean, thisBean.getAutowireCandidate());
    mergeValue(thisBean, thisBean.getAutowire());
    mergeValue(thisBean, thisBean.getDependencyCheck());
    mergeValue(thisBean, thisBean.getDependsOn());
    mergeValue(thisBean, thisBean.getFactoryBean());
    mergeValue(thisBean, thisBean.getFactoryMethod());
    mergeValue(thisBean, thisBean.getInitMethod());
    mergeValue(thisBean, thisBean.getDestroyMethod());
    mergeValue(thisBean, thisBean.getDescription());
    mergeList(thisBean, InfraBeanImpl.CTOR_ARGS_GETTER, InfraBean::addConstructorArg);
    mergeList(thisBean, InfraBeanImpl.PROPERTIES_GETTER, InfraBean::addProperty);
    mergeList(thisBean, InfraBean::getReplacedMethods, InfraBean::addReplacedMethod);
    value.undefine();
    reformat(parent);
  }

  private static <T extends GenericDomValue<?>> void mergeValue(InfraBean infraBean, T value) {
    GenericDomValue mergedValue = BeanCoreUtils.getMergedValue(infraBean, value);
    if (mergedValue != value) {
      value.setStringValue(mergedValue.getStringValue());
    }
  }

  public static <T extends DomElement> void mergeList(InfraBean infraBean, Function<? super InfraBean, ? extends Collection<T>> getter, Function<? super InfraBean, ? extends T> adder) {
    Set<T> merged = BeanCoreUtils.getMergedSet(infraBean, getter);
    Collection<T> existing = getter.fun(infraBean);
    for (T t : existing) {
      if (!merged.contains(t)) {
        t.undefine();
      }
      else {
        merged.remove(t);
      }
    }
    for (T t2 : merged) {
      adder.fun(infraBean).copyFrom(t2);
    }
  }

  private static void reformat(DomElement domElement) {
    try {
      CodeStyleManager.getInstance(domElement.getManager().getProject()).reformat(domElement.getXmlTag());
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
  }

  private static void copyBean(DomInfraBean from, DomElement parent) {
    XmlTag newTag = (XmlTag) parent.getXmlTag().add(from.getXmlTag());
    XmlAttribute id = newTag.getAttribute("id");
    if (id != null) {
      id.delete();
    }
    XmlAttribute name = newTag.getAttribute("name");
    if (name != null) {
      name.delete();
    }
  }
}
