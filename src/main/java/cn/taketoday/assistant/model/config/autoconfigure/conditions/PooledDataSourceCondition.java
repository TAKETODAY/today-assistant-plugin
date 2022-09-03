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

package cn.taketoday.assistant.model.config.autoconfigure.conditions;

import com.intellij.openapi.util.Key;

import cn.taketoday.assistant.util.InfraUtils;

class PooledDataSourceCondition extends ConditionalContributor {
  private static final String SPRING_DATASOURCE_TYPE = "datasource.type";
  private static final String[] DATA_SOURCE_TYPE_NAMES = {
          "com.zaxxer.hikari.HikariDataSource",
          "org.apache.tomcat.jdbc.pool.DataSource",
          "org.apache.commons.dbcp2.BasicDataSource"
  };
  private static final Key<ConditionOutcome> OUR_KEY = Key.create("PooledDataSourceCondition");

  @Override
  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    ConditionOutcome conditionOutcome = context.getUserData(
            OUR_KEY);
    if (conditionOutcome == null) {
      conditionOutcome = context.putUserDataIfAbsent(OUR_KEY, calcConditionOutcome(context));
    }
    return conditionOutcome;
  }

  private ConditionOutcome calcConditionOutcome(ConditionalOnEvaluationContext context) {
    if (hasConfigKey(context, SPRING_DATASOURCE_TYPE)) {
      return ConditionOutcome.match(
              ConditionMessage.foundConfigKey(SPRING_DATASOURCE_TYPE));
    }
    for (String dataSourceTypeName : DATA_SOURCE_TYPE_NAMES) {
      if (InfraUtils.findLibraryClass(context.getModule(), dataSourceTypeName) != null) {
        return ConditionOutcome.match(
                ConditionMessage.found("supported DataSource", dataSourceTypeName));
      }
    }
    return ConditionOutcome.noMatch(ConditionMessage.didNotFind("supported DataSource"));
  }
}
