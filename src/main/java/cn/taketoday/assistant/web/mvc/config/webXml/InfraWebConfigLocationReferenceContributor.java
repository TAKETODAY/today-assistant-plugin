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

package cn.taketoday.assistant.web.mvc.config.webXml;

import com.intellij.javaee.model.xml.ParamValue;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.DomPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.impl.source.resolve.reference.impl.manipulators.XmlTagManipulator;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.text.StringTokenizer;

import cn.taketoday.assistant.model.utils.resources.InfraResourcesBuilder;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;

public class InfraWebConfigLocationReferenceContributor extends PsiReferenceContributor {

  private static final String CONTEXT_PARAM = "context-param";
  private static final String INIT_PARAM = "init-param";

  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(XmlPatterns.xmlTag().withLocalName("param-value")
                    .withParent(StandardPatterns.or(DomPatterns.tagWithDom(CONTEXT_PARAM, ParamValue.class), DomPatterns.tagWithDom(INIT_PARAM, ParamValue.class))),
            new WebConfigLocationReferenceProvider());
  }

  private static class WebConfigLocationReferenceProvider extends PsiReferenceProvider {

    public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
      XmlTag tag = (XmlTag) element;
      XmlTag parent = tag.getParentTag();
      if (parent == null || (!parent.getName().equals(CONTEXT_PARAM) && !parent.getName().equals(INIT_PARAM))) {
        return PsiReference.EMPTY_ARRAY;
      }
      XmlTag nameTag = parent.findFirstSubTag("param-name");
      if (nameTag == null) {
        return PsiReference.EMPTY_ARRAY;
      }
      String name = ElementManipulators.getValueText(nameTag);
      if (!name.equals(InfraMvcConstant.CONTEXT_CONFIG_LOCATION)) {
        return PsiReference.EMPTY_ARRAY;
      }
      else if (isAnnotationConfig(parent)) {
        return PsiReference.EMPTY_ARRAY;
      }
      else {
        PsiReference[] result = PsiReference.EMPTY_ARRAY;
        TextRange[] ranges = XmlTagManipulator.getValueRanges(tag);
        for (TextRange range : ranges) {
          String text = range.substring(element.getText());
          StringTokenizer tokenizer = new StringTokenizer(text, ",; \n\t");
          while (tokenizer.hasMoreTokens()) {
            String s = tokenizer.nextToken();
            int end = tokenizer.getCurrentPosition();
            int offset = (end - s.length()) + range.getStartOffset();
            InfraResourcesBuilder builder = InfraResourcesBuilder.create(element, s).fromRoot(true).offset(offset).soft(false);
            PsiReference[] references = ResourcesUtil.of().getReferences(builder);
            result = ArrayUtil.mergeArrays(result, references, PsiReference.ARRAY_FACTORY);
          }
        }
        return result;
      }
    }

    private static boolean isAnnotationConfig(XmlTag contextParamTag) {
      XmlTag parentTag = contextParamTag.getParentTag();
      if (parentTag != null) {
        if ("web-app".equals(parentTag.getName())) {
          return hasAnnotationConfigDefined(parentTag.findSubTags(CONTEXT_PARAM));
        }
        if ("servlet".equals(parentTag.getName())) {
          return hasAnnotationConfigDefined(parentTag.findSubTags(INIT_PARAM));
        }
        return false;
      }
      return false;
    }

    private static boolean hasAnnotationConfigDefined(XmlTag[] contextParams) {
      XmlTag valueTag;
      for (XmlTag contextParam : contextParams) {
        XmlTag nameTag = contextParam.findFirstSubTag("param-name");
        if (nameTag != null) {
          String name = ElementManipulators.getValueText(nameTag);
          if (name.equals(InfraMvcConstant.CONTEXT_CLASS_PARAM_NAME) && (valueTag = contextParam.findFirstSubTag("param-value")) != null) {
            return InfraMvcConstant.ANNOTATION_CONFIG_CLASS.equals(ElementManipulators.getValueText(valueTag));
          }
        }
      }
      return false;
    }
  }
}
