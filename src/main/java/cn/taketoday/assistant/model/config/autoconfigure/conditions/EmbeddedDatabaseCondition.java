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

import cn.taketoday.assistant.util.InfraUtils;

class EmbeddedDatabaseCondition extends ConditionalContributor {
  private final PooledDataSourceCondition myPooledDataSourceCondition = new PooledDataSourceCondition();

  @Override
  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    EmbeddedDatabaseConnection[] values;
    if (this.myPooledDataSourceCondition.matches(context).isMatch()) {
      return ConditionOutcome.noMatch(ConditionMessage.found("supported pooled data source"));
    }
    for (EmbeddedDatabaseConnection connection : EmbeddedDatabaseConnection.values()) {
      if (InfraUtils.findLibraryClass(context.getModule(), connection.getDriverClassName()) != null) {
        return ConditionOutcome.match("Found embedded database");
      }
    }
    return ConditionOutcome.noMatch("Did not find embedded database");
  }

  enum EmbeddedDatabaseConnection {
    H2("org.h2.Driver"),
    DERBY("org.apache.derby.jdbc.EmbeddedDriver"),
    HSQL("org.hsqldb.jdbcDriver");

    private final String myDriverClassName;

    EmbeddedDatabaseConnection(String driverClassName) {
      this.myDriverClassName = driverClassName;
    }

    public String getDriverClassName() {
      return this.myDriverClassName;
    }
  }
}
