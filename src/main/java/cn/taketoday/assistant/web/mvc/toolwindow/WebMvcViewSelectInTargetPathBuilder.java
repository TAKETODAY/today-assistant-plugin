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

package cn.taketoday.assistant.web.mvc.toolwindow;

import com.intellij.ide.SelectInContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.CommonProcessors;
import com.intellij.util.SmartList;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastUtils;

import java.util.Iterator;
import java.util.Objects;

import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.web.mvc.InfraControllerUtils;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.assistant.web.mvc.model.xml.ViewControllerBase;
import cn.taketoday.lang.Nullable;

public class WebMvcViewSelectInTargetPathBuilder {
  private Object[] myPath = ArrayUtilRt.EMPTY_OBJECT_ARRAY;
  private Module myModule;
  private WebMvcViewSettings myViewSettings;

  public WebMvcViewSelectInTargetPathBuilder(SelectInContext context) {
    PsiElement element = getElement(context);
    if (element != null) {
      calculatePath(element);
    }
  }

  public boolean canSelect() {
    return this.myPath.length != 0;
  }

  public Object[] getPath() {
    return this.myPath;
  }

  private PsiElement getElement(SelectInContext context) {
    Object selector = context.getSelectorInFile();
    if (!(selector instanceof PsiElement element)) {
      return null;
    }
    this.myModule = ModuleUtilCore.findModuleForPsiElement(element);
    if (this.myModule == null) {
      return null;
    }
    this.myViewSettings = WebMvcViewSettings.getInstance(this.myModule.getProject());
    return element;
  }

  private void calculatePath(PsiElement element) {
    UClass uClass = UastUtils.findContaining(element, UClass.class);
    PsiClass psiClass = uClass != null ? uClass.getJavaPsi() : null;
    if (psiClass != null) {
      findControllerCode(element, psiClass);
    }
    else {
      findControllerXml(element);
    }
  }

  private void findControllerCode(PsiElement element, PsiClass psiClass) {
    if (psiClass.hasModifierProperty("private") || PsiUtil.isLocalOrAnonymousClass(psiClass) || !InfraControllerUtils.isController(psiClass)) {
      return;
    }
    CommonProcessors.FindProcessor<BeanPointer<?>> processor = new CommonProcessors.FindProcessor<BeanPointer<?>>() {
      public boolean accept(BeanPointer pointer) {
        return psiClass.equals(pointer.getBeanClass());
      }
    };
    if (WebMvcViewUtils.processControllers(this.myModule, processor)) {
      Iterator it = ModuleUtilCore.getAllDependentModules(this.myModule).iterator();
      while (true) {
        if (!it.hasNext()) {
          break;
        }
        Module dependentModule = (Module) it.next();
        if (!WebMvcViewUtils.processControllers(dependentModule, processor)) {
          this.myModule = dependentModule;
          break;
        }
      }
    }
    if (!processor.isFound()) {
      return;
    }
    BeanPointer<?> controllerBeanPointer = processor.getFoundValue();
    UMethod uMethod = UastUtils.findContaining(element, UMethod.class);
    PsiMethod requestMethod = uMethod != null ? uMethod.getJavaPsi() : null;
    UrlMappingElement requestMappingItem = null;
    boolean isValidRequestMethod = requestMethod != null && InfraControllerUtils.isRequestHandler(requestMethod);
    if (isValidRequestMethod) {
      CommonProcessors.FindProcessor<UrlMappingElement> methodProcessor = new CommonProcessors.FindProcessor<UrlMappingElement>() {
        public boolean accept(UrlMappingElement variant) {
          return requestMethod.equals(variant.getNavigationTarget());
        }
      };
      requestMappingItem = findRequestMappingItem(controllerBeanPointer, methodProcessor);
    }
    createResult(controllerBeanPointer, requestMappingItem);
  }

  private void findControllerXml(PsiElement element) {
    XmlTag xmlTag;
    DomElement domElement;
    ViewControllerBase viewController;
    PsiFile containingFile = element.getContainingFile();
    if (!(containingFile instanceof XmlFile xmlFile)
            || !InfraDomUtils.isInfraXml(xmlFile)
            || (xmlTag = PsiTreeUtil.getParentOfType(element,
            XmlTag.class)) == null || (domElement = DomUtil.getDomElement(xmlTag)) == null || (viewController = domElement.getParentOfType(ViewControllerBase.class, false)) == null) {
      return;
    }
    BeanPointer<?> controllerBeanPointer = InfraBeanService.of().createBeanPointer(viewController);
    String viewControllerPath = StringUtil.trimLeading(Objects.requireNonNull(viewController.getPath().getStringValue()), '/');
    CommonProcessors.FindProcessor<UrlMappingElement> methodProcessor = new CommonProcessors.FindProcessor<UrlMappingElement>() {
      public boolean accept(UrlMappingElement variant) {
        return variant.getURL().equals(viewControllerPath);
      }
    };
    UrlMappingElement requestMappingItem = findRequestMappingItem(controllerBeanPointer, methodProcessor);
    createResult(controllerBeanPointer, requestMappingItem);
  }

  @Nullable
  private UrlMappingElement findRequestMappingItem(BeanPointer controllerBeanPointer, CommonProcessors.FindProcessor<UrlMappingElement> processor) {
    WebMvcViewUtils.processUrls(this.myModule, controllerBeanPointer, this.myViewSettings.getRequestMethods(), processor);
    return processor.getFoundValue();
  }

  private void createResult(BeanPointer controllerBeanPointer, @Nullable UrlMappingElement requestMappingItem) {
    SmartList smartList = new SmartList();
    if (this.myViewSettings.isShowModules()) {
      smartList.add(this.myModule);
    }
    if (this.myViewSettings.isShowControllers()) {
      smartList.add(controllerBeanPointer);
    }
    if (requestMappingItem != null) {
      smartList.add(requestMappingItem);
    }
    this.myPath = ArrayUtil.toObjectArray(smartList);
  }
}
