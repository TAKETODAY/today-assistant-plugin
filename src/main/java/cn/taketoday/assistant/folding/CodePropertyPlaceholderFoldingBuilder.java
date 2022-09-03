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

import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.util.SmartList;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UastContextKt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.model.values.PlaceholderPropertyReference;
import cn.taketoday.assistant.references.PlaceholderReferencesPlaces;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/27 22:18
 */
public final class CodePropertyPlaceholderFoldingBuilder extends XmlPropertyPlaceholderFoldingBuilder {

  @Override
  public FoldingDescriptor[] buildFoldRegions(PsiElement root, Document document, boolean quick) {
    if (quick || !isEnabled() || !InfraLibraryUtil.hasLibrary(root.getProject())) {
      return FoldingDescriptor.EMPTY;
    }
    SmartList<FoldingDescriptor> smartList = new SmartList<>();
    root.accept(new PsiRecursiveElementVisitor() {
      public void visitElement(PsiElement element) {
        Collection<FoldingDescriptor> foldingDescriptors = createFoldingDescriptors(element);
        if (!foldingDescriptors.isEmpty()) {
          smartList.addAll(foldingDescriptors);
        }
        else {
          super.visitElement(element);
        }
      }
    });
    return smartList.toArray(new FoldingDescriptor[0]);
  }

  @Override
  protected TextRange getRangeForFolding(PsiElement element, PlaceholderPropertyReference reference) {
    TextRange var10;
    label31:
    {
      String text = element.getText();
      String var10000 = reference.getFullTextRange().substring(text);
      String wrappedRefText = "${" + var10000 + "}";
      var10 = reference.getRangeInElement();
      int startFoldingTextOffset = var10.getStartOffset() - 2;
      int it = ((Number) startFoldingTextOffset).intValue();
      Integer var11 = it > -1 ? startFoldingTextOffset : null;
      if ((it > -1 ? startFoldingTextOffset : null) != null) {
        label28:
        {
          startFoldingTextOffset = var11;
          it = ((Number) startFoldingTextOffset).intValue();
          Intrinsics.checkNotNullExpressionValue(text, "text");
          Character var12 = StringsKt.getOrNull(text, it - 1);
          if (var12 != null) {
            if (var12 == '\\') {
              var10 = TextRange.from(it - 1, wrappedRefText.length() + 1);
              break label28;
            }
          }

          var10 = TextRange.from(it, wrappedRefText.length());
        }

        if (var10 != null) {
          break label31;
        }
      }

      var10 = reference.getFullTextRange();
    }

    TextRange wrappedRefRange = var10;
    TextRange textRange = element.getTextRange();
    var10 = wrappedRefRange.shiftRight(textRange.getStartOffset());
    return var10;
  }

  public Collection<FoldingDescriptor> createFoldingDescriptors(PsiElement psiElement) {
    ArrayList<FoldingDescriptor> arrayList;
    UAnnotation uAnnotation = UastContextKt.toUElement(psiElement, UAnnotation.class);
    if (uAnnotation != null) {
      Iterable<String> iterable = PlaceholderReferencesPlaces.PLACEHOLDER_ANNOTATIONS.get(uAnnotation.getQualifiedName());
      if (iterable != null) {
        ArrayList<PsiElement> psiElements = new ArrayList<>();
        for (String attribute : iterable) {
          UExpression findDeclaredAttributeValue = uAnnotation.findDeclaredAttributeValue(attribute);
          PsiElement sourcePsi = findDeclaredAttributeValue != null ? findDeclaredAttributeValue.getSourcePsi() : null;
          if (sourcePsi != null) {
            psiElements.add(sourcePsi);
          }
        }
        ArrayList<FoldingDescriptor> list = new ArrayList<>();
        for (PsiElement it : psiElements) {
          List<FoldingDescriptor> descriptors = createReferenceFoldingDescriptors(it);
          list.addAll(descriptors);
        }
        arrayList = list;
      }
      else {
        arrayList = null;
      }
      if (arrayList != null) {
        return arrayList;
      }
    }
    return CollectionsKt.emptyList();
  }

  private ArrayList<FoldingDescriptor> createReferenceFoldingDescriptors(PsiElement element) {
    ArrayList<FoldingDescriptor> descriptors = new ArrayList<>();

    for (PsiReference ref : XmlPropertyPlaceholderFoldingBuilder.getInnerReferences(element)) {
      Impl impl;
      if (!(ref instanceof PlaceholderPropertyReference)) {
        impl = null;
      }
      else {
        ResolveResult[] resolveResults = ((PlaceholderPropertyReference) ref).multiResolve(false);
        Intrinsics.checkNotNullExpressionValue(resolveResults, "resolveResults");
        impl = resolveResults.length == 0
               ? null : new Impl(ref, resolveResults, element,
                getRangeForFolding(element, (PlaceholderPropertyReference) ref), this, element);
      }

      if (impl != null) {
        descriptors.add(impl);
      }
    }

    return descriptors;
  }

  public Pair<PlaceholderPropertyReference, PsiElement> resolveProperty(PlaceholderPropertyReference reference) {
    ResolveResult[] results = reference.multiResolve(false);
    Intrinsics.checkNotNullExpressionValue(results, "results");
    if (results.length != 0) {
      ResolveResult resolveResult = results[0];
      Intrinsics.checkNotNullExpressionValue(resolveResult, "results[0]");
      return Pair.create(reference, resolveResult.getElement());
    }
    return null;
  }
}

final class Impl extends FoldingDescriptor {
  // $FF: synthetic field
  final PsiReference reference;
  // $FF: synthetic field
  final ResolveResult[] resolveResults;
  // $FF: synthetic field
  final CodePropertyPlaceholderFoldingBuilder this$0;
  // $FF: synthetic field
  final PsiElement psiElement;

  Impl(PsiReference psiReference, ResolveResult[] resolveResults,
          PsiElement element, TextRange textRange, CodePropertyPlaceholderFoldingBuilder builder, PsiElement var6) {
    super(element, textRange);
    this.reference = psiReference;
    this.resolveResults = resolveResults;
    this.this$0 = builder;
    this.psiElement = var6;
    String text;
    ResolveResult resolveResult = ArraysKt.singleOrNull(resolveResults);
    if (resolveResult != null) {
      text = this.this$0.getPlaceholderText(this.psiElement, new Pair(this.reference, resolveResult.getElement()));
    }
    else {
      text = null;
    }

    setPlaceholderText(text);
  }

  @Override
  public Set getDependencies() {
    Pair<PlaceholderPropertyReference, PsiElement> resolveResult = this$0.resolveProperty((PlaceholderPropertyReference) this.reference);
    PsiElement property = resolveResult != null ? resolveResult.second : null;
    if (property != null) {
      return SetsKt.setOfNotNull(property.getContainingFile());
    }
    else {
      return super.getDependencies();
    }
  }
}
