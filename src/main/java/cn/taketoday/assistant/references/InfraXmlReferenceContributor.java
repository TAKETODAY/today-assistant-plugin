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

package cn.taketoday.assistant.references;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.properties.PropertiesReferenceProvider;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.filters.AndFilter;
import com.intellij.psi.filters.ScopeFilter;
import com.intellij.psi.filters.TextFilter;
import com.intellij.psi.filters.XmlTagFilter;
import com.intellij.psi.filters.position.NamespaceFilter;
import com.intellij.psi.filters.position.ParentElementFilter;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.ProcessingContext;
import com.intellij.util.xml.DomUtil;
import com.intellij.xml.util.XmlUtil;

import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.xml.CustomBean;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.lang.NonNull;

final class InfraXmlReferenceContributor extends PsiReferenceContributor {

  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[] { "code" },
            new ScopeFilter(new ParentElementFilter(new AndFilter(new NamespaceFilter("http://www.springframework.org/tags"),
                    new AndFilter(XmlTagFilter.INSTANCE, new TextFilter("message", "theme"))), 2
            )),
            new PropertiesReferenceProvider(false));
    PatternCondition<XmlAttributeValue> customBeanPattern = new PatternCondition<>("customBeanId") {
      @Override
      public boolean accepts(@NonNull XmlAttributeValue attributeValue, ProcessingContext context) {
        CustomBeanWrapper element = DomUtil.findDomElement(attributeValue, CustomBeanWrapper.class);
        if (element == null) {
          return false;
        }
        for (CustomBean customBean : element.getCustomBeans()) {
          if (customBean.getIdAttribute() == attributeValue.getParent()) {
            context.put("bean", customBean);
            return true;
          }
        }
        return false;
      }
    };
    registrar.registerReferenceProvider(
            XmlPatterns.xmlAttributeValue()
                    .inVirtualFile(PsiJavaPatterns.virtualFile().ofType(XmlFileType.INSTANCE))
                    .inFile(PlatformPatterns.psiFile(XmlFile.class).with(new PatternCondition<>("isInfraXmlFile") {
                      @Override
                      public boolean accepts(XmlFile file, ProcessingContext context) {
                        return InfraDomUtils.isInfraXml(file);
                      }
                    })).with(customBeanPattern), new PsiReferenceProvider() {

              public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
                CustomBean bean = (CustomBean) context.get("bean");
                return new PsiReference[] {
                        PsiReferenceBase.createSelfReference(element, bean.getIdentifyingPsiElement())
                };
              }
            }
    );
  }
}
