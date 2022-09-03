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

package cn.taketoday.assistant.web.mvc.client.rest;

import java.util.List;
import java.util.Set;

public final class RestOperationsConstants {

  public static final String REST_OPERATIONS_PACKAGE = "cn.taketoday.web.client";
  public static final String REST_testOPERATIONS_PACKAGE = "cn.taketoday.test.web.client";

  public static final RestOperationsConstants INSTANCE = new RestOperationsConstants();

  public static final String EXCHANGE_METHOD = "exchange";
  public static final String EXECUTE_METHOD = "execute";

  public static final Set<RestOperation> REST_OPERATIONS_METHODS = Set.of(
          new RestOperation("delete", "DELETE"),
          new RestOperation(EXCHANGE_METHOD, "GET"),
          new RestOperation(EXECUTE_METHOD, "GET"),
          new RestOperation("getForEntity", "GET"),
          new RestOperation("getForObject", "GET"),
          new RestOperation("headForHeaders", "HEAD"),
          new RestOperation("optionsForAllow", "OPTIONS"),
          new RestOperation("patchForObject", "PATCH"),
          new RestOperation("postForEntity", "POST"),
          new RestOperation("postForLocation", "POST"),
          new RestOperation("postForObject", "POST"),
          new RestOperation("put", "PUT")
  );

  private static final List<String> PACKAGE_SEARCH_BLACK_LIST = List.of(REST_OPERATIONS_PACKAGE, REST_testOPERATIONS_PACKAGE);

  public Set<RestOperation> getREST_OPERATIONS_METHODS() {
    return REST_OPERATIONS_METHODS;
  }

  public List<String> getPACKAGE_SEARCH_BLACK_LIST() {
    return PACKAGE_SEARCH_BLACK_LIST;
  }
}
