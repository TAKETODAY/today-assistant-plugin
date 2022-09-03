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

import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.DelimitedListProcessor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.xml.XmlElement;
import com.intellij.uast.UastModificationTracker;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.DomService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.model.jam.SemContributorUtil;
import cn.taketoday.assistant.model.jam.profile.InfraContextProfile;
import cn.taketoday.assistant.model.jam.profile.InfraJamProfile;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraDomProfile;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraProfilesFactory {

  public static InfraProfilesFactory getInstance() {
    return ApplicationManager.getApplication().getService(InfraProfilesFactory.class);
  }

  public List<InfraProfileTarget> findProfileTargets(Module module, boolean includeTests) {
    return ContainerUtil.concat(findJamTargets(module, includeTests), findDomTargets(module, includeTests));
  }

  @Nullable
  public InfraProfileTarget findProfileTarget(Module module, boolean includeTests, String profileName) {
    for (InfraProfileTarget target : findJamTargets(module, includeTests)) {
      if (profileName.equals(target.getName())) {
        return target;
      }
    }
    for (InfraProfileTarget target2 : findDomTargets(module, includeTests)) {
      if (profileName.equals(target2.getName())) {
        return target2;
      }
    }
    return null;
  }

  public PsiReference[] getProfilesReferences(
          Module module, PsiElement element, @cn.taketoday.lang.Nullable String value,
          int valueOffset, String delimiters, boolean isDefinition) {
    List<TextRange> ranges = getProfileRanges(value, delimiters);
    if (ranges.isEmpty()) {
      return PsiReference.EMPTY_ARRAY;
    }
    int referenceOffset = ElementManipulators.getOffsetInElement(element) + valueOffset;
    return ContainerUtil.map2Array(ranges, PsiReference.class,
            range -> new InfraProfilePsiReference(element, range.shiftRight(referenceOffset), module, isDefinition));
  }

  /**
   * @return parsed profiles expressions that can be matched against active profiles
   * @throws MalformedProfileExpressionException if profile expression is malformed
   */
  public Predicate<Set<String>> parseProfileExpressions(Collection<String> expressions) {
    return InfraProfilesParser.parse(expressions);
  }

  private static List<InfraProfileTarget> findDomTargets(Module module, boolean includeTests) {
    return (CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      Map<Boolean, List<InfraProfileTarget>> map = ConcurrentFactoryMap.createMap(
              withTests -> findDomTargets(module, module.getModuleScope(withTests)));
      return CachedValueProvider.Result.create(map, DomManager.getDomManager(module.getProject()));
    })).get(includeTests);
  }

  public static List<InfraProfileTarget> findDomTargets(Module module, GlobalSearchScope scope) {
    SmartList<InfraProfileTarget> smartList = new SmartList<>();
    List<DomFileElement<Beans>> elements = DomService.getInstance().getFileElements(Beans.class, module.getProject(), scope);
    for (DomFileElement<Beans> element : elements) {
      collectProfileTargets(element.getRootElement(), smartList);
    }
    return smartList;
  }

  private static void collectProfileTargets(Beans beans, List<InfraProfileTarget> targets) {
    InfraDomProfile profile = beans.getProfile();
    XmlElement xmlElement = profile.getXmlElement();
    if (xmlElement != null) {
      String value = profile.getStringValue();
      List<TextRange> ranges = getProfileRanges(value, InfraUtils.INFRA_DELIMITERS);
      if (!ranges.isEmpty()) {
        int offset = ElementManipulators.getOffsetInElement(xmlElement);
        targets.addAll(ContainerUtil.map(ranges, range -> {
          return new InfraProfileTarget(xmlElement, range.substring(value), range.getStartOffset() + offset);
        }));
      }
    }
    for (Beans beansProfile : beans.getBeansProfiles()) {
      collectProfileTargets(beansProfile, targets);
    }
  }

  private static List<InfraProfileTarget> findJamTargets(Module module, boolean includeTests) {
    return (CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      Map<Boolean, List<InfraProfileTarget>> map = ConcurrentFactoryMap.createMap(
              withTests -> findJamTargets(module, module.getModuleScope(withTests)));
      Project project = module.getProject();
      return CachedValueProvider.Result.create(map, ProjectRootManager.getInstance(project), UastModificationTracker.getInstance(project));
    })).get(includeTests);
  }

  public static List<InfraProfileTarget> findJamTargets(Module module, GlobalSearchScope scope) {
    List<InfraProfileTarget> targets = new ArrayList<>();
    JamService jamService = JamService.getJamService(module.getProject());
    List<String> fqns = new ArrayList<>(SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.PROFILE).fun(module));
    fqns.add(AnnotationConstant.PROFILE);
    for (String fqn : fqns) {
      List<InfraContextProfile> jamProfiles = jamService.getJamClassElements(InfraContextProfile.CONTEXT_PROFILE_JAM_KEY, fqn, scope);
      for (InfraContextProfile jamProfile : jamProfiles) {
        targets.addAll(getProfileTargets(jamProfile));
      }
      List<InfraContextProfile> jamMethodProfiles = jamService.getJamMethodElements(InfraContextProfile.CONTEXT_PROFILE_JAM_KEY, fqn, scope);
      for (InfraContextProfile jamProfile2 : jamMethodProfiles) {
        targets.addAll(getProfileTargets(jamProfile2));
      }
    }
    return targets;
  }

  private static List<InfraProfileTarget> getProfileTargets(InfraContextProfile jamProfile) {
    SmartList<InfraProfileTarget> smartList = new SmartList<>();
    for (JamStringAttributeElement<String> attributeElement : jamProfile.getValueElements()) {
      PsiAnnotationMemberValue psiElement = attributeElement.getPsiElement();
      if (psiElement != null) {
        String value = attributeElement.getStringValue();
        List<TextRange> ranges = getProfileRanges(value, InfraJamProfile.PROFILE_DELIMITERS);
        if (!ranges.isEmpty()) {
          PsiAnnotationMemberValue memberValue = attributeElement.getPsiElement();
          boolean isPsiLiteral = memberValue instanceof PsiLiteral;
          int offset = isPsiLiteral ? ElementManipulators.getOffsetInElement(memberValue) : -1;
          smartList.addAll(ContainerUtil.map(ranges, range -> {
            int nameOffset = isPsiLiteral ? range.getStartOffset() + offset : -1;
            return new InfraProfileTarget(psiElement, range.substring(value), nameOffset);
          }));
        }
      }
    }
    return smartList;
  }

  private static List<TextRange> getProfileRanges(@cn.taketoday.lang.Nullable String value, String delimiters) {
    if (StringUtil.isEmptyOrSpaces(value)) {
      return Collections.emptyList();
    }
    SmartList<TextRange> smartList = new SmartList<>();
    new DelimitedListProcessor(delimiters) {
      protected void processToken(int start, int end, boolean delimitersOnly) {
        String profileName = value.substring(start, end);
        String profileNameTrimmed = profileName.trim();
        int profileNameIdx = profileName.indexOf(profileNameTrimmed);
        TextRange trimmedRange = TextRange.from(start + profileNameIdx, profileNameTrimmed.length());
        if (StringUtil.startsWithChar(profileNameTrimmed, '!')) {
          trimmedRange = trimmedRange.shiftRight(1).grown(-1);
        }
        if (trimmedRange.getLength() > 0) {
          smartList.add(trimmedRange);
        }
      }
    }.processText(value);
    return smartList;
  }

  public static class MalformedProfileExpressionException extends RuntimeException {
    MalformedProfileExpressionException(String message) {
      super(message);
    }
  }

}
