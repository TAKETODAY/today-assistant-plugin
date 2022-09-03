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

package cn.taketoday.assistant.model.highlighting.autowire.xml;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import java.util.Collection;
import java.util.Map;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.beans.AutowireUtil;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.highlighting.xml.InfraConstructorArgResolveUtil;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.beans.Autowire;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.DefaultableBoolean;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.lang.Nullable;

public final class XmlAutowireExplicitlyInspection extends InfraBeanInspectionBase {

  @Override
  public void checkFileElement(DomFileElement<Beans> domFileElement, DomElementAnnotationHolder holder) {
    super.checkFileElement(domFileElement, holder);
    Beans beans = domFileElement.getRootElement();
    GenericAttributeValue<Autowire> defaultAutowireAttribute = beans.getDefaultAutowire();
    Autowire defaultAutowire = defaultAutowireAttribute.getValue();
    if (defaultAutowire != null && Autowire.NO != defaultAutowire) {
      holder.createProblem(defaultAutowireAttribute, HighlightSeverity.WARNING, InfraBundle.message("bean.autowire.escape"),
              createDefaultAutowireEscapeQuickFixes(beans.createStableCopy(), defaultAutowire));
    }
  }

  private static LocalQuickFix createDefaultAutowireEscapeQuickFixes(Beans beans, Autowire defaultAutowire) {
    return new LocalQuickFix() {

      public String getFamilyName() {
        return InfraBundle.message("bean.autowire.escape");
      }

      public void applyFix(Project project, ProblemDescriptor descriptor) {
        if (!beans.isValid()) {
          return;
        }
        WriteCommandAction.Builder writeCommandAction = WriteCommandAction.writeCommandAction(project, DomUtil.getFile(beans));
        writeCommandAction.run(() -> {
          Autowire autowire2;
          for (InfraBean bean : beans.getBeans()) {
            if (isAutowireCandidate(bean)
                    && ((autowire2 = bean.getAutowire().getValue()) == null || autowire2.getValue()
                    .equals(defaultAutowire.getValue()) || autowire2 == Autowire.DEFAULT)) {
              escapeAutowire(defaultAutowire.getValue(), bean);
            }
          }
          beans.getDefaultAutowire().undefine();
        });
      }
    };
  }

  @Override
  protected void checkBean(InfraBean infraBean, Beans beans, DomElementAnnotationHolder holder, @Nullable CommonInfraModel model) {
    if (isAutowireCandidate(infraBean)) {
      addAutowireEscapeWarning(infraBean, holder);
    }
  }

  private static boolean isAutowireCandidate(InfraBean infraBean) {
    DefaultableBoolean autoWireCandidate = infraBean.getAutowireCandidate().getValue();
    return autoWireCandidate == null || autoWireCandidate.getBooleanValue();
  }

  private static void addAutowireEscapeWarning(InfraBean infraBean, DomElementAnnotationHolder holder) {
    Autowire autowire = infraBean.getAutowire().getValue();
    if (autowire != null && Autowire.NO != autowire) {
      holder.createProblem(infraBean.getAutowire(), HighlightSeverity.WARNING, InfraBundle.message("bean.use.autowire"),
              createEscapeAutowireQuickFixes(infraBean.createStableCopy(), autowire));
    }
  }

  private static LocalQuickFix createEscapeAutowireQuickFixes(InfraBean infraBean, Autowire autowire) {
    return new LocalQuickFix() {

      public String getFamilyName() {
        return InfraBundle.message("bean.autowire.escape");
      }

      public void applyFix(Project project, ProblemDescriptor descriptor) {
        if (!infraBean.isValid()) {
          return;
        }
        WriteCommandAction.Builder writeCommandAction
                = WriteCommandAction.writeCommandAction(infraBean.getManager().getProject(), DomUtil.getFile(infraBean));
        writeCommandAction.run(() -> {
          escapeAutowire(autowire.getValue(), infraBean);
        });
      }
    };
  }

  public static void escapeAutowire(String autowire, InfraBean infraBean) {
    CommonInfraModel model = InfraModelService.of().getModel(infraBean);
    if (autowire.equals(Autowire.BY_TYPE.getValue())) {
      escapeByTypeAutowire(infraBean, model);
    }
    else if (autowire.equals(Autowire.BY_NAME.getValue())) {
      escapeByNameAutowire(infraBean);
    }
    else if (autowire.equals(Autowire.CONSTRUCTOR.getValue())) {
      escapeConstructorAutowire(infraBean, model);
    }
    else if (autowire.equals(Autowire.AUTODETECT.getValue())) {
      if (InfraConstructorArgResolveUtil.hasEmptyConstructor(infraBean) && !InfraConstructorArgResolveUtil.isInstantiatedByFactory(infraBean)) {
        escapeByTypeAutowire(infraBean, model);
      }
      else {
        escapeConstructorAutowire(infraBean, model);
      }
    }
  }

  private static void escapeConstructorAutowire(InfraBean infraBean, CommonInfraModel model) {
    Map<PsiType, Collection<BeanPointer<?>>> map = AutowireUtil.getConstructorAutowiredProperties(infraBean, model);
    for (PsiType psiType : map.keySet()) {
      ConstructorArg arg = infraBean.addConstructorArg();
      arg.getType().setStringValue(psiType.getCanonicalText());
      arg.getRefAttr().setStringValue(chooseReferencedBeanName(map.get(psiType)));
    }
    infraBean.getAutowire().undefine();
  }

  private static void escapeByNameAutowire(InfraBean infraBean) {
    Map<PsiMethod, BeanPointer<?>> autowiredProperties = AutowireUtil.getByNameAutowiredProperties(infraBean);
    for (PsiMethod psiMethod : autowiredProperties.keySet()) {
      InfraProperty springProperty = infraBean.addProperty();
      BeanPointer<?> autowiredBean = autowiredProperties.get(psiMethod);
      String refBeanName = (autowiredBean == null || autowiredBean.getName() == null) ? "" : autowiredBean.getName();
      springProperty.getName().setStringValue(PropertyUtilBase.getPropertyNameBySetter(psiMethod));
      springProperty.getRefAttr().setStringValue(refBeanName);
    }
    infraBean.getAutowire().undefine();
  }

  private static void escapeByTypeAutowire(InfraBean infraBean, CommonInfraModel model) {
    Map<PsiMethod, Collection<BeanPointer<?>>> autowiredProperties = AutowireUtil.getByTypeAutowiredProperties(infraBean, model);
    for (PsiMethod psiMethod : autowiredProperties.keySet()) {
      InfraProperty springProperty = infraBean.addProperty();
      springProperty.getName().setStringValue(PropertyUtilBase.getPropertyNameBySetter(psiMethod));
      springProperty.getRefAttr().setStringValue(chooseReferencedBeanName(autowiredProperties.get(psiMethod)));
    }
    infraBean.getAutowire().undefine();
  }

  private static String chooseReferencedBeanName(Collection<BeanPointer<?>> autowiredBeans) {
    if (autowiredBeans != null) {
      for (BeanPointer<?> autowiredBean : autowiredBeans) {
        CommonInfraModel model = InfraModelService.of().getModelByBean(autowiredBean.getBean());
        String beanName = BeanCoreUtils.getReferencedName(autowiredBean, model.getAllCommonBeans());
        if (beanName != null && beanName.trim().length() > 0) {
          return beanName;
        }
      }
      return "";
    }
    return "";
  }
}
