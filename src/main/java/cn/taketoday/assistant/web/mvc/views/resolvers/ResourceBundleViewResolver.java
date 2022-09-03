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

package cn.taketoday.assistant.web.mvc.views.resolvers;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;

import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.utils.resources.InfraResourcesBuilder;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.web.mvc.views.ViewResolver;

public class ResourceBundleViewResolver extends ViewResolver {
  private final Set<String> myBasenames;
  private final CommonInfraBean myBean;

  public ResourceBundleViewResolver(CommonInfraBean commonInfraBean) {
    this.myBasenames = new LinkedHashSet();
    this.myBean = commonInfraBean;
    if (commonInfraBean instanceof InfraBean bean) {
      String basename = InfraPropertyUtils.getPropertyStringValue(bean, "basename");
      if (basename != null) {
        this.myBasenames.add(basename);
        return;
      }
      Set<String> basenames = InfraPropertyUtils.getArrayPropertyStringValues(bean, "basenames");
      if (basenames.size() > 0) {
        this.myBasenames.addAll(basenames);
      }
      else {
        this.myBasenames.add("views");
      }
    }
  }

  @Override
  @TestOnly
  public String getID() {
    String str = "ResourceBundleViewResolver[" + this.myBean.getBeanName() + "]";
    return str;
  }

  @Override
  public Set<PsiElement> resolveView(String viewName) {
    List<PropertiesFile> bundles = getBundles();
    for (PropertiesFile bundle : bundles) {
      IProperty property = bundle.findPropertyByKey(viewName);
      if (property != null) {
        return Collections.singleton(property.getPsiElement());
      }
    }
    return Collections.emptySet();
  }

  @Override

  public List<LookupElement> getAllResolverViews() {
    ArrayList<LookupElement> list = new ArrayList<>();
    List<PropertiesFile> bundles = getBundles();
    for (PropertiesFile bundle : bundles) {
      for (IProperty property : bundle.getProperties()) {
        String key = property.getKey();
        if (key != null) {
          list.add(LookupElementBuilder.create(key).withTailText(" (" + property.getValue() + ")", true));
        }
      }
    }
    return list;
  }

  @Override
  public String bindToElement(PsiElement element) {
    return ((IProperty) element).getKey();
  }

  @Override

  public String handleElementRename(String newElementName) {
    return newElementName;
  }

  private List<PropertiesFile> getBundles() {
    PsiFile containingFile = this.myBean.getContainingFile();
    List<PropertiesFile> files = new ArrayList<>();
    for (String basename : this.myBasenames) {
      String fileName = StringUtil.replace(basename, ".", "/") + ".properties";
      PsiExpression psiExpression = JavaPsiFacade.getElementFactory(containingFile.getProject()).createExpressionFromText("\"" + fileName + "\"", containingFile);
      if (psiExpression instanceof PsiLiteral) {
        InfraResourcesBuilder resourcesBuilder = InfraResourcesBuilder.create(psiExpression, fileName).fromRoot(fileName.startsWith("/")).soft(false);
        PsiReference[] references = ResourcesUtil.of().getReferences(resourcesBuilder);
        if (references[0] instanceof FileReference fileReference) {
          PsiFileSystemItem resolve = fileReference.getFileReferenceSet().resolve();
          if (resolve instanceof PropertiesFile propertiesFile) {
            files.add(propertiesFile);
          }
        }
      }
    }
    return files;
  }
}
