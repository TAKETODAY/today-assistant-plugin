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
package cn.taketoday.assistant.model.jam.converters;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PackageReferenceSet;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.CommonProcessors;
import com.intellij.util.PatternUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

import cn.taketoday.lang.Nullable;

public class InfraAntPatternPackageReferenceSet extends PackageReferenceSet {

  public InfraAntPatternPackageReferenceSet(String packageName,
          PsiElement element,
          int startInElement,
          GlobalSearchScope scope) {
    super(packageName, element, startInElement, scope);
  }

  @Override
  protected PsiPackageReference createReference(TextRange range, int index) {
    return new PsiPackageReference(this, range, index) {
      @Override
      protected ResolveResult[] doMultiResolve() {
        Collection<PsiPackage> packages = new LinkedHashSet<>();
        for (PsiPackage parentPackage : getContext()) {
          packages.addAll(resolvePackages(parentPackage));
        }
        return PsiElementResolveResult.createResults(packages);
      }

      private Collection<PsiPackage> resolvePackages(@Nullable final PsiPackage context) {
        if (context == null)
          return Collections.emptySet();

        final String packageName = getValue();

        CommonProcessors.CollectProcessor<PsiPackage> processor = getProcessor(packageName);

        PsiPackageReference parentContextReference = myIndex > 0 ? getReferenceSet().getReference(myIndex - 1) : null;

        if (packageName.equals("*")) {
          for (PsiPackage aPackage : context.getSubPackages(getResolveScope())) {
            if (!processor.process(aPackage))
              break;
          }
          return processor.getResults();
        }

        if (parentContextReference != null && parentContextReference.getValue().equals(("**"))) {
          return getSubPackages(context, processor, true);
        }

        if (packageName.equals("**")) {
          if (isLastReference()) {
            return getSubPackages(context, processor, false);
          }
          return Collections.singleton(context);
        }
        else if (packageName.contains("*") || packageName.contains("?")) {
          for (final PsiPackage subPackage : context.getSubPackages(getResolveScope())) {
            processor.process(subPackage);
          }
          return processor.getResults();
        }

        return getReferenceSet().resolvePackageName(context, packageName);
      }

      private boolean isLastReference() {
        return this.equals(getReferenceSet().getLastReference());
      }

      private CommonProcessors.CollectProcessor<PsiPackage> getProcessor(String packageName) {
        final Pattern pattern = PatternUtil.fromMask(packageName);
        return new CommonProcessors.CollectProcessor<>(new LinkedHashSet<>()) {
          @Override
          protected boolean accept(PsiPackage psiPackage) {
            String name = psiPackage.getName();
            return name != null && pattern.matcher(name).matches();
          }
        };
      }

      private Collection<PsiPackage> getSubPackages(PsiPackage context, CommonProcessors.CollectProcessor<PsiPackage> processor, boolean deep) {
        processSubPackages(context, processor, deep);
        return processor.getResults();
      }

      private void processSubPackages(final PsiPackage psiPackage,
              final CommonProcessors.CollectProcessor<PsiPackage> processor, boolean deep) {
        for (final PsiPackage subPackage : psiPackage.getSubPackages(getResolveScope())) {
          processor.process(subPackage);
          if (deep)
            processSubPackages(subPackage, processor, true);
        }
      }
    };
  }
}