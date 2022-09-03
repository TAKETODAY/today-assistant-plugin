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

package cn.taketoday.assistant.web.mvc.model;

import com.intellij.jam.JamPomTarget;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.microservices.utils.UrlMappingBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;
import cn.taketoday.assistant.web.mvc.model.mappings.UrlMappingPsiBasedElement;
import cn.taketoday.lang.Nullable;
import one.util.streamex.StreamEx;

public final class RequestMappingServiceImpl implements RequestMappingService {
  private final Project myProject;

  public RequestMappingServiceImpl(Project project) {
    this.myProject = project;
  }

  @Override
  public Collection<? extends UrlMappingElement> getMappings(BeanPointer<?> beanPointer, @Nullable String baseUrl) {
    PsiClass beanClass = beanPointer.getBeanClass();
    if (beanClass == null) {
      return Collections.emptyList();
    }
    JamService jamService = JamService.getJamService(this.myProject);
    RequestMapping<PsiClass> classLevelMapping = jamService.getJamElement(RequestMapping.CLASS_JAM_KEY, beanClass);
    List<JamStringAttributeElement<String>> classMappingUrls = classLevelMapping != null ? classLevelMapping.getMappingUrls() : Collections.emptyList();
    if (classMappingUrls.isEmpty()) {
      classMappingUrls = Collections.singletonList(null);
    }
    List<JamStringAttributeElement<String>> rootMappingUrls = classMappingUrls;
    return StreamEx.of(jamService.getAnnotatedMembersList(beanClass, RequestMapping.METHOD_JAM_KEY, 10)).flatMap(jam -> {
      return StreamEx.of(jam.getMappingUrls()).flatMap(url -> {
        return rootMappingUrls.stream().map(root -> {
          return Pair.create(root, url);
        });
      }).map(mappingPair -> {
        UrlMappingBuilder builder = new UrlMappingBuilder(baseUrl);
        if (mappingPair.getFirst() != null) {
          builder.appendSegment(mappingPair.getFirst().getStringValue());
        }
        JamStringAttributeElement<String> urlAttribute = mappingPair.getSecond();
        return new UrlMappingPsiBasedElement(builder.appendSegment(urlAttribute.getStringValue()).build(), jam.getPsiElement(), new JamPomTarget(jam, urlAttribute), null, jam.getMethods());
      });
    }).toList();
  }
}
