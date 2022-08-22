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

package cn.taketoday.assistant.code.event.jam;

import com.intellij.jam.JamElement;
import com.intellij.jam.JamService;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.semantic.SemKey;

import java.util.List;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:11
 */
public interface EventListenerElement extends JamElement {
  String CLASSES_ATTR_NAME = "event";
  String VALUE_ATTR_NAME = "value";
  String CONDITION_ATTR_NAME = "condition";
  SemKey<EventListenerElement> EVENT_LISTENER_ROOT_JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("EventListener");

  List<PsiClass> getEventListenerClasses();

  @Nullable
  PsiMethod getPsiElement();

  @Nullable
  PsiAnnotation getAnnotation();

  boolean isValid();
}
