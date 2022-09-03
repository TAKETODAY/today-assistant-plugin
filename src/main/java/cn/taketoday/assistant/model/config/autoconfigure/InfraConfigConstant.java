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
package cn.taketoday.assistant.model.config.autoconfigure;

import cn.taketoday.assistant.AnnotationConstant;

public interface InfraConfigConstant {

  String ENABLE_AUTO_CONFIGURATION =
          AnnotationConstant.EnableAutoConfiguration;

  String INFRA_APPLICATION = "cn.taketoday.framework.InfraApplication";

  String AUTO_CONFIGURE_ORDER =
          "cn.taketoday.context.annotation.config.AutoConfigureOrder";

  String AUTO_CONFIGURE_AFTER =
          "cn.taketoday.context.annotation.config.AutoConfigureAfter";

  String AUTO_CONFIGURE_BEFORE =
          "cn.taketoday.context.annotation.config.AutoConfigureBefore";

  String CONDITIONAL_ON_CLASS =
          "cn.taketoday.context.condition.ConditionalOnClass";

  String CONDITIONAL_ON_MISSING_CLASS =
          "cn.taketoday.context.condition.ConditionalOnMissingClass";

  String CONDITIONAL_ON_BEAN =
          "cn.taketoday.context.condition.ConditionalOnBean";
  String CONDITIONAL_ON_MISSING_BEAN =
          "cn.taketoday.context.condition.ConditionalOnMissingBean";

  String CONDITIONAL_ON_EXPRESSION =
          "cn.taketoday.context.condition.ConditionalOnExpression";

  String CONDITIONAL_ON_SINGLE_CANDIDATE =
          "cn.taketoday.context.condition.ConditionalOnSingleCandidate";

  String CONDITIONAL_ON_RESOURCE =
          "cn.taketoday.context.condition.ConditionalOnResource";

  String CONDITIONAL_ON_WEB_APPLICATION =
          "cn.taketoday.context.condition.ConditionalOnWebApplication";
  String CONDITIONAL_ON_NOT_WEB_APPLICATION =
          "cn.taketoday.context.condition.ConditionalOnNotWebApplication";

  String CONDITIONAL_ON_PROPERTY =
          "cn.taketoday.context.condition.ConditionalOnProperty";

  String CONDITIONAL_ON_ENABLED_RESOURCE_CHAIN =
          "cn.taketoday.context.annotation.config.web.ConditionalOnEnabledResourceChain";

  String CONDITIONAL_ON_REPOSITORY_TYPE =
          "cn.taketoday.context.annotation.config.data.ConditionalOnRepositoryType";

  String CONDITIONAL_ON_ENABLED_HEALTH_INDICATOR =
          "cn.taketoday.boot.actuate.autoconfigure.ConditionalOnEnabledHealthIndicator";

  String CONDITIONAL_ON_ENABLED_HEALTH_INDICATOR_SB2 =
          "cn.taketoday.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator";

  String CONDITIONAL_ON_ENABLED_INFO_CONTRIBUTOR =
          "cn.taketoday.boot.actuate.autoconfigure.ConditionalOnEnabledInfoContributor";
  String CONDITIONAL_ON_ENABLED_INFO_CONTRIBUTOR_SB2 =
          "cn.taketoday.boot.actuate.autoconfigure.info.ConditionalOnEnabledInfoContributor";

  String CONDITIONAL_ON_MANAGEMENT_PORT =
          "cn.taketoday.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort";

  String AUTO_CONFIGURATION =
          "cn.taketoday.context.annotation.config.AutoConfiguration";
  String IMPORT_AUTO_CONFIGURATION =
          "cn.taketoday.context.annotation.config.ImportAutoConfiguration";
}
