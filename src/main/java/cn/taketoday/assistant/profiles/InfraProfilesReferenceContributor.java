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

package cn.taketoday.assistant.profiles;

import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.uast.UastPatterns;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.UastReferenceRegistrar;
import com.intellij.util.ProcessingContext;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.ReferencePatternConditions;

final class InfraProfilesReferenceContributor extends PsiReferenceContributor {

  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    InfraProfilesReferenceProvider profilesProvider = new InfraProfilesReferenceProvider();
    UastReferenceRegistrar.registerUastReferenceProvider(
            registrar,
            UastPatterns.injectionHostUExpression().withSourcePsiCondition(ReferencePatternConditions.PROJECT_HAS_FACETS_CONDITION)
                    .inCall(
                            UastPatterns.callExpression()
                                    .withMethodName(PsiJavaPatterns.string().oneOf("addActiveProfile", "setActiveProfiles", "setDefaultProfiles"))
                                    .withReceiver(PsiJavaPatterns.psiClass().withQualifiedName(InfraConstant.CONFIGURABLE_ENVIRONMENT))
                    ),
            UastReferenceRegistrar.uastInjectionHostReferenceProvider(
                    (expression, host) -> profilesProvider.getReferencesByElement(host, new ProcessingContext())), 100.0d
    );
  }
}
