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

package cn.taketoday.assistant.web.mvc;

public interface WebMvcFunctionalRoutingConstant {

  String REACTIVE_DISPATCHER_HANDLER = "cn.taketoday.web.reactive.DispatcherHandler";
  String ROUTER_FUNCTION = "cn.taketoday.web.servlet.function.RouterFunction";
  String REACTIVE_ROUTER_FUNCTION = "cn.taketoday.web.reactive.function.server.RouterFunction";
  String REACTIVE_ROUTER_FUNCTIONS = "cn.taketoday.web.reactive.function.server.RouterFunctions";
  String ROUTER_FUNCTIONS = "cn.taketoday.web.servlet.function.RouterFunctions";
  String REACTIVE_ROUTER_FUNCTIONS_BUILDER = "cn.taketoday.web.reactive.function.server.RouterFunctions.Builder";
  String ROUTER_FUNCTIONS_BUILDER = "cn.taketoday.web.servlet.function.RouterFunctions.Builder";
  String REACTIVE_REQUEST_PREDICATES = "cn.taketoday.web.reactive.function.server.RequestPredicates";
  String REQUEST_PREDICATES = "cn.taketoday.web.servlet.function.RequestPredicates";
  String SERVER_REQUEST = "cn.taketoday.web.reactive.function.server.ServerRequest";
  String RENDERING = "cn.taketoday.web.reactive.result.view.Rendering";
  String RENDERING_BUILDER = "cn.taketoday.web.reactive.result.view.Rendering.Builder";
  String BODY_BUILDER = "cn.taketoday.web.reactive.function.server.ServerResponse.BodyBuilder";
  String REACTIVE_ROUTER_FUNCTION_DSL = "cn.taketoday.web.reactive.function.server.RouterFunctionDsl";
  String ROUTER_FUNCTION_DSL = "cn.taketoday.web.servlet.function.RouterFunctionDsl";
  String REACTIVE_HANDLER_MAPPING = "cn.taketoday.web.reactive.HandlerMapping";
  String REACTIVE_SIMPLE_HANDLER_MAPPING = "cn.taketoday.web.reactive.handler.SimpleUrlHandlerMapping";
  String REACTIVE_WEB_SOCKET_HANDLER = "cn.taketoday.web.reactive.socket.WebSocketHandler";
  String WEB_CLIENT = "cn.taketoday.web.reactive.function.client.WebClient";
  String WEB_CLIENT_BUILDER = "cn.taketoday.web.reactive.function.client.WebClient.Builder";
  String WEB_CLIENT_URI_SPEC = "cn.taketoday.web.reactive.function.client.WebClient.UriSpec";
  String WEB_TEST_CLIENT = "cn.taketoday.test.web.reactive.server.WebTestClient";
  String WEB_TEST_CLIENT_URI_SPEC = "cn.taketoday.test.web.reactive.server.WebTestClient.UriSpec";
  String WEB_TEST_CLIENT_BUILDER = "cn.taketoday.test.web.reactive.server.WebTestClient.Builder";
  String WEB_TEST_CLIENT_REQUEST_BODY_SPEC = "cn.taketoday.test.web.reactive.server.WebTestClient.RequestBodySpec";
}
