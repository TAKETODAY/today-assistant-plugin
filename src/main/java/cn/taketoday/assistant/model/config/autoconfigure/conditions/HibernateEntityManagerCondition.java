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

import static cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage.didNotFindClass;
import static cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage.foundClass;

class HibernateEntityManagerCondition extends ConditionalContributor {

  private static final String[] CLASS_NAMES = {
          "org.hibernate.ejb.HibernateEntityManager",
          "org.hibernate.jpa.HibernateEntityManager"
  };

  @Override
  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    for (String className : CLASS_NAMES) {
      if (InfraUtils.findLibraryClass(context.getModule(), className) != null) {
        return ConditionOutcome.match(foundClass(className));
      }
    }
    return ConditionOutcome.noMatch(didNotFindClass(CLASS_NAMES));
  }
}
