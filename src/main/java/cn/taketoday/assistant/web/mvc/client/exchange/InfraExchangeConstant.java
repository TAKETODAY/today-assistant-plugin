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

package cn.taketoday.assistant.web.mvc.client.exchange;

import java.util.List;

public interface InfraExchangeConstant {

  String SPRING_HTTP_EXCHANGE = "cn.taketoday.web.service.annotation.HttpExchange";
  String SPRING_GET_EXCHANGE = "cn.taketoday.web.service.annotation.GetExchange";
  String SPRING_HEAD_EXCHANGE = "cn.taketoday.web.service.annotation.HeadExchange";
  String SPRING_DELETE_EXCHANGE = "cn.taketoday.web.service.annotation.DeleteExchange";
  String SPRING_OPTIONS_EXCHANGE = "cn.taketoday.web.service.annotation.OptionsExchange";
  String SPRING_PUT_EXCHANGE = "cn.taketoday.web.service.annotation.PutExchange";
  String SPRING_POST_EXCHANGE = "cn.taketoday.web.service.annotation.PostExchange";
  String SPRING_PATCH_EXCHANGE = "cn.taketoday.web.service.annotation.PatchExchange";

  List<String> SPRING_EXCHANGE_METHOD_ANNOTATIONS = List.of(
          SPRING_HTTP_EXCHANGE, SPRING_GET_EXCHANGE, SPRING_HEAD_EXCHANGE,
          SPRING_DELETE_EXCHANGE, SPRING_OPTIONS_EXCHANGE, SPRING_PUT_EXCHANGE,
          SPRING_POST_EXCHANGE, SPRING_PATCH_EXCHANGE
  );
}
