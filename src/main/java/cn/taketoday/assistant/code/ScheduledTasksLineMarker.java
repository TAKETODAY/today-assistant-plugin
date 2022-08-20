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

package cn.taketoday.assistant.code;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.spring.SpringBundle;
import com.intellij.spring.constants.SpringAnnotationsConstants;
import com.intellij.util.Function;

import cn.taketoday.lang.Nullable;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.AnnotationConstant;
import icons.JavaUltimateIcons;
import kotlin.jvm.internal.Intrinsics;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 21:11
 */
public class ScheduledTasksLineMarker extends LineMarkerProviderDescriptor {

  @Override
  public String getId() {
    return "ScheduledTasksLineMarker";
  }

  @Override
  public String getName() {
    return SpringBundle.message("spring.scheduled.tasks");
  }

  @Override
  public Icon getIcon() {
    return JavaUltimateIcons.Cdi.Gutter.ScheduledEvent;
  }

  @Override
  @Nullable
  public LineMarkerInfo getLineMarkerInfo(PsiElement element) {
    return null;
  }

  @Override
  public void collectSlowLineMarkers(List elements, Collection result) {
    for (Object o : elements) {
      PsiElement element = (PsiElement) o;
      LineMarkerInfo markerInfo = computeLineMarkerInfo(element);
      if (markerInfo != null) {
        result.add(markerInfo);
      }
    }

  }

  private final LineMarkerInfo computeLineMarkerInfo(PsiElement element) {
    UElement var10000 = UastUtils.getUParentForIdentifier(element);
    if (!(var10000 instanceof UMethod)) {
      var10000 = null;
    }

    UDeclaration var6 = (UDeclaration) var10000;
    if (var6 != null) {
      UDeclaration parent$iv = var6;
      var6 = !Intrinsics.areEqual(UElementKt.getSourcePsiElement(parent$iv.getUastAnchor()), element) ? null : parent$iv;
    }
    else {
      var6 = null;
    }

    if (var6 instanceof UMethod uMethod) {
      if (isScheduled(uMethod)) {
        var10000 = uMethod.getUastAnchor();
        if (var10000 != null) {
          PsiElement identifyingElement = var10000.getSourcePsi();
          if (identifyingElement != null) {
            return new LineMarkerInfo<>(identifyingElement,
                    identifyingElement.getTextRange(),
                    JavaUltimateIcons.Cdi.Gutter.ScheduledEvent,
                    o -> SpringBundle.message("spring.scheduled.method"), null,
                    GutterIconRenderer.Alignment.LEFT, SpringBundle.messagePointer("spring.scheduled.method"));
          }
        }
      }
    }
    return null;
  }

  private boolean isScheduled(UMethod uMethod) {
    List<UAnnotation> list = uMethod.getUAnnotations();
    if (list.isEmpty()) {
      return false;
    }

    Iterator<UAnnotation> iterator = list.iterator();
    while (true) {
      if (!iterator.hasNext()) {
        return false;
      }

      UAnnotation anno = iterator.next();
      if (Intrinsics.areEqual(anno.getQualifiedName(), AnnotationConstant.SCHEDULED)
              || Intrinsics.areEqual(anno.getQualifiedName(), AnnotationConstant.SCHEDULES)) {
        return true;
      }
    }
  }
}
