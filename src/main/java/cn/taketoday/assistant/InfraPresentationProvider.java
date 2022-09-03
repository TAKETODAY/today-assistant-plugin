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

package cn.taketoday.assistant;

import com.intellij.ide.TypePresentationService;
import com.intellij.ide.presentation.PresentationProvider;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;

import java.util.Objects;

import javax.swing.Icon;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.BeanPsiTarget;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraImplicitBeanMarker;
import cn.taketoday.assistant.model.InfrastructureBean;
import cn.taketoday.assistant.model.jam.JamBeanPointer;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.scope.BeanScope;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraImport;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/25 13:25
 */
public class InfraPresentationProvider extends PresentationProvider<Object> {

  @Override
  public String getName(Object o) {
    if (o instanceof CommonInfraBean) {
      return getBeanName((CommonInfraBean) o);
    }

    if (o instanceof BeanPointer<?> beanPointer) {
      return getBeanName(beanPointer);
    }

    if (o instanceof InfraImport) {
      return ((InfraImport) o).getResource().getStringValue();
    }
    return null;
  }

  @Nullable
  @Override
  public Icon getIcon(Object o) {
    return getInfraIcon(o);
  }

  public static String getBeanName(BeanPointer<?> beanPointer) {
    String pointerName = beanPointer.getName();
    return pointerName != null ? pointerName : getBeanName(beanPointer.getBean());
  }

  public static String getBeanName(CommonInfraBean infraBean) {
    if (!infraBean.isValid())
      return InfraBundle.message("bean.invalid");

    String beanName = infraBean.getBeanName();
    if (beanName != null)
      return beanName;

    if (infraBean instanceof InfraBean) {
      PsiType beanClass = infraBean.getBeanType();
      if (beanClass != null)
        return beanClass.getPresentableText();
    }

    String typeName = TypePresentationService.getService().getTypeName(infraBean);
    if (typeName != null)
      return typeName;

    PsiElement identifyingPsiElement = infraBean.getIdentifyingPsiElement();
    if (identifyingPsiElement instanceof PomTargetPsiElement) {
      PsiElement target = identifyingPsiElement.getNavigationElement();
      if (target instanceof XmlTag) {
        String name = ((XmlTag) target).getName();
        if (!StringUtil.isEmptyOrSpaces(name))
          return "<" + name + " ... />";
      }
    }

    return InfraBundle.message("bean.with.unknown.name");
  }

  public static String getInfraBeanLocation(BeanPointer<?> beanPointer) {
    if (beanPointer instanceof JamBeanPointer) {
      CommonInfraBean commonInfraBean = beanPointer.getBean();
      if (commonInfraBean instanceof InfraImplicitBeanMarker beanMarker) {
        return beanMarker.getProviderName();
      }
    }

    return beanPointer.getContainingFile().getName();
  }

  @Nullable
  public static Icon getInfraIcon(@Nullable Object o) {
    if (o instanceof BeanPointer) {
      o = ((BeanPointer<?>) o).getBean();
    }
    else if (o instanceof BeanPsiTarget) {
      o = ((BeanPsiTarget) o).getInfraBean();
    }

    if (!(o instanceof CommonInfraBean)) {
      return null;
    }

    if (o instanceof InfraImplicitBeanMarker) {
      return Icons.ImplicitBean;
    }

    if (o instanceof InfrastructureBean) {
      return Icons.InfrastructureBean;
    }

    if (o instanceof JamPsiMemberInfraBean) {
      return Icons.SpringJavaBean;
    }

    if (o instanceof InfraBean infraBean && infraBean.isValid()) {
      if (infraBean.isAbstract()) {
        return Icons.AbtractBean;
      }

      GenericAttributeValue<BeanScope> scope = infraBean.getScope();
      if (DomUtil.hasXml(scope) &&
              Objects.equals(scope.getStringValue(), BeanScope.PROTOTYPE_SCOPE.getValue())) {
        return Icons.PrototypeBean;
      }
    }

    return Icons.SpringBean;
  }
}
