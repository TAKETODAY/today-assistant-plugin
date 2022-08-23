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

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.swing.Icon;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.lang.Nullable;
import icons.JavaUltimateIcons;

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
    return InfraBundle.message("scheduled.tasks");
  }

  @Override
  public Icon getIcon() {
    return JavaUltimateIcons.Cdi.Gutter.ScheduledEvent;
  }

  @Override
  @Nullable
  public LineMarkerInfo<PsiElement> getLineMarkerInfo(PsiElement element) {
    return null;
  }

  @Override
  public void collectSlowLineMarkers(
          List<? extends PsiElement> elements, Collection<? super LineMarkerInfo<?>> result) {
    for (PsiElement element : elements) {
      LineMarkerInfo<PsiElement> markerInfo = computeLineMarkerInfo(element);
      if (markerInfo != null) {
        result.add(markerInfo);
      }
    }
  }

  private LineMarkerInfo<PsiElement> computeLineMarkerInfo(PsiElement element) {
    UElement identifier = UastUtils.getUParentForIdentifier(element);
    if (identifier instanceof UMethod uMethod) {
      if (Objects.equals(UElementKt.getSourcePsiElement(uMethod.getUastAnchor()), element)
              && isScheduled(uMethod)) {
        identifier = uMethod.getUastAnchor();
        if (identifier != null) {
          PsiElement identifyingElement = identifier.getSourcePsi();
          if (identifyingElement != null) {
            return new LineMarkerInfo<>(identifyingElement,
                    identifyingElement.getTextRange(),
                    JavaUltimateIcons.Cdi.Gutter.ScheduledEvent,
                    o -> InfraBundle.message("scheduled.method"), null,
                    GutterIconRenderer.Alignment.LEFT, InfraBundle.messagePointer("scheduled.method"));
          }
        }
      }
    }
    return null;
  }

  private boolean isScheduled(UMethod uMethod) {
    for (UAnnotation anno : uMethod.getUAnnotations()) {
      if (Objects.equals(anno.getQualifiedName(), AnnotationConstant.SCHEDULED)
              || Objects.equals(anno.getQualifiedName(), AnnotationConstant.SCHEDULES)) {
        return true;
      }
    }
    return false;
  }
}
