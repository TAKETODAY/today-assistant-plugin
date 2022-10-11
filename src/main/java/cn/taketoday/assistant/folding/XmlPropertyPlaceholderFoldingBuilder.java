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

package cn.taketoday.assistant.folding;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.folding.JavaCodeFoldingSettings;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.lang.properties.IProperty;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomElementVisitor;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.values.InfraPlaceholderReferenceResolver;
import cn.taketoday.assistant.model.values.PlaceholderPropertyReference;
import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/27 22:15
 */
public class XmlPropertyPlaceholderFoldingBuilder extends FoldingBuilderEx {

  public FoldingDescriptor[] buildFoldRegions(PsiElement root, Document document, boolean quick) {
    if (root instanceof XmlFile && !quick && isEnabled()) {
      DomFileElement<Beans> fileElement = InfraDomUtils.getDomFileElement((XmlFile) root);
      if (fileElement == null) {
        return FoldingDescriptor.EMPTY;
      }
      else {
        PlaceholderUtils placeholderUtils = PlaceholderUtils.getInstance();
        List<FoldingDescriptor> regions = new SmartList<>();
        fileElement.accept(new DomElementVisitor() {
          public void visitDomElement(DomElement element) {
            if (DomUtil.hasXml(element)) {
              if (element instanceof GenericDomValue<?> genericDomValue) {
                if (placeholderUtils.isRawTextPlaceholder(genericDomValue)) {
                  XmlElement xmlElement = element.getXmlElement();
                  if (xmlElement instanceof XmlAttribute) {
                    XmlAttributeValue value = ((XmlAttribute) xmlElement).getValueElement();
                    if (value != null) {
                      ContainerUtil.addIfNotNull(regions, XmlPropertyPlaceholderFoldingBuilder.this.createDescriptor(value));
                    }
                  }
                  else if (xmlElement instanceof XmlTag) {
                    ContainerUtil.addIfNotNull(regions, XmlPropertyPlaceholderFoldingBuilder.this.createDescriptor(xmlElement));
                  }
                }
              }
              else {
                element.acceptChildren(this);
              }

            }
          }
        });
        return regions.isEmpty() ? FoldingDescriptor.EMPTY : regions.toArray(FoldingDescriptor.EMPTY);
      }
    }
    else {
      return FoldingDescriptor.EMPTY;
    }
  }

  protected boolean isEnabled() {
    return JavaCodeFoldingSettings.getInstance().isCollapseI18nMessages();
  }

  @Nullable
  protected FoldingDescriptor createDescriptor(PsiElement element) {
    Iterator<PsiReference> var2 = getInnerReferences(element).iterator();
    PsiReference reference;
    do {
      if (!var2.hasNext()) {
        return null;
      }

      reference = var2.next();
    }
    while (!(reference instanceof PlaceholderPropertyReference));

    if (((PlaceholderPropertyReference) reference).multiResolve(false).length == 0) {
      return null;
    }
    else {
      return new FoldingDescriptor(element, this.getRangeForFolding(element, (PlaceholderPropertyReference) reference)) {
        public Set<Object> getDependencies() {
          Pair<PlaceholderPropertyReference, PsiElement> resolveResult = resolveProperty(element);
          PsiElement property = resolveResult != null ? resolveResult.second : null;
          if (property != null) {
            PsiFile containingFile = property.getContainingFile();
            return ContainerUtil.createMaybeSingletonSet(containingFile);
          }
          else {
            return super.getDependencies();
          }
        }
      };
    }
  }

  protected static Iterable<PsiReference> getInnerReferences(PsiElement element) {
    Stream<PsiReference> references = Arrays.stream(element.getReferences());
    if (element instanceof PsiLanguageInjectionHost) {
      Stream<PsiReference> innerReferences = Arrays.stream(element.getChildren()).flatMap((e) -> Arrays.stream(e.getReferences()));
      references = Stream.concat(references, innerReferences);
    }

    Objects.requireNonNull(references);
    return references::iterator;
  }

  protected TextRange getRangeForFolding(PsiElement element, PlaceholderPropertyReference reference) {
    return reference.getFullTextRange().shiftRight(element.getTextRange().getStartOffset());
  }

  public String getPlaceholderText(ASTNode node) {
    PsiElement element = node.getPsi();
    Pair<PlaceholderPropertyReference, PsiElement> resolveResult = resolveProperty(element);
    return this.getPlaceholderText(element, resolveResult);
  }

  @Nullable
  protected String getPlaceholderText(PsiElement element, Pair<PlaceholderPropertyReference, PsiElement> resolveResult) {
    PsiElement property = resolveResult != null ? resolveResult.second : null;
    String text = null;
    if (property instanceof IProperty) {
      text = ((IProperty) property).getUnescapedValue();
    }
    else if (property != null) {
      for (InfraPlaceholderReferenceResolver resolver : InfraPlaceholderReferenceResolver.array()) {
        text = resolver.getPropertyValue(resolveResult.first, property);
        if (text != null) {
          break;
        }
      }
    }

    if (text == null) {
      return null;
    }
    else if (text.trim().isEmpty()) {
      return "<empty>";
    }
    else {
      PsiFile psiFile = element.getContainingFile();
      if (psiFile != null) {
        int maxLength = getPlaceholderTextMaxLength(psiFile);
        text = StringUtil.shortenTextWithEllipsis(text, maxLength, 0);
      }

      return text;
    }
  }

  public boolean isCollapsedByDefault(ASTNode node) {
    return true;
  }

  @Nullable
  private static Pair<PlaceholderPropertyReference, PsiElement> resolveProperty(PsiElement element) {
    for (PsiReference reference : getInnerReferences(element)) {
      if (reference instanceof PlaceholderPropertyReference placeholderPropertyReference) {
        ResolveResult[] results = placeholderPropertyReference.multiResolve(false);
        if (results.length > 0) {
          return Pair.create(placeholderPropertyReference, results[0].getElement());
        }
      }
    }

    return null;
  }

  private static int getPlaceholderTextMaxLength(PsiFile psiFile) {
    return CodeStyle.getSettings(psiFile).getRightMargin(psiFile.getLanguage()) / 2;
  }
}

