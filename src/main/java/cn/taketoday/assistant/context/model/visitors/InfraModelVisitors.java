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

package cn.taketoday.assistant.context.model.visitors;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.RecursionManager;

import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.context.model.InfraModel;

public final class InfraModelVisitors {

  /**
   * Infra models are:
   * - connected (imports/component-scans/implicit fileset configurations/auto-configurations/etc.)
   * - contains other models {@link InfraModel}
   * It visits all related infra models(xml,java,imported,scanned, children models, etc.).
   *
   * @return false to stop processing
   */
  public static <T> boolean visitRelatedModels(
          CommonInfraModel parentModel, InfraModelVisitorContext<T> context) {
    return visitRelated(parentModel, context, true);
  }

  public static <T> boolean visitRelatedModels(CommonInfraModel parentModel,
          InfraModelVisitorContext<T> visitorContext, boolean visitParentModel) {
    return visitRelated(parentModel, visitorContext, visitParentModel);
  }

  public static <T> boolean visitRecursionAwareRelatedModels(
          CommonInfraModel parentModel, InfraModelVisitorContext<T> visitorContext) {
    return visitRecursionAwareRelatedModels(parentModel, visitorContext, true);
  }

  public static <T> boolean visitRecursionAwareRelatedModels(
          CommonInfraModel parentModel, InfraModelVisitorContext<T> visitorContext, boolean visitParentModel) {
    Boolean aBoolean = RecursionManager.doPreventingRecursion(parentModel, false, () -> visitRelated(parentModel,
            visitorContext, visitParentModel)
    );
    return aBoolean == null || aBoolean;
  }

  private static <T> boolean visitRelated(CommonInfraModel parentModel,
          InfraModelVisitorContext<T> visitorContext, boolean visitParent) {
    if (visitorContext.hasBeenVisited(parentModel))
      return true;
    if (visitParent && !visitorContext.visit(parentModel))
      return false;
    return visitRelated(visitorContext, parentModel.getRelatedModels());
  }

  private static <T> boolean visitRelated(
          InfraModelVisitorContext<T> visitorContext, Set<CommonInfraModel> relatedModels) {
    for (CommonInfraModel model : relatedModels) {
      ProgressManager.checkCanceled();
      if (!visitRelated(model, visitorContext, true))
        return false;
    }
    return true;
  }
}
