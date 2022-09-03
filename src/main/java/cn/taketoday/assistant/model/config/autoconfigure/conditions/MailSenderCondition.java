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

import static cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage.didNotFindConfigKey;
import static cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage.foundConfigKey;
import static cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome.match;
import static cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome.noMatch;

class MailSenderCondition extends ConditionalContributor {

  private static final String SPRING_MAIL_HOST = "mail.host";
  private static final String SPRING_MAIL_JNDI_NAME = "mail.jndi-name";

  @Override
  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    if (hasConfigKey(context, SPRING_MAIL_HOST)) {
      return match(foundConfigKey(SPRING_MAIL_HOST));
    }
    return hasConfigKey(context, SPRING_MAIL_JNDI_NAME)
           ? match(foundConfigKey(SPRING_MAIL_JNDI_NAME))
           : noMatch(didNotFindConfigKey(SPRING_MAIL_HOST, SPRING_MAIL_JNDI_NAME));
  }
}
