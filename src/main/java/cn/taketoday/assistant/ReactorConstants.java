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

package cn.taketoday.assistant;

import java.util.List;

public interface ReactorConstants {

  String FLUX = "reactor.core.publisher.Flux";

  String HOOKS = "reactor.core.publisher.Hooks";
  String REACTOR_DEBUG_AGENT = "reactor.tools.agent.ReactorDebugAgent";

  // reactive event wrappers, should have single <T> type parameter
  List<String> REACTIVE_EVENT_WRAPPER_CLASSES = List.of(
          FLUX,
          "reactor.core.publisher.Mono",
          "reactor.core.CorePublisher",
          "org.reactivestreams.Publisher",
          "io.reactivex.Flowable",
          "io.reactivex.Observable",
          "io.reactivex.rxjava3.core.Flowable",
          "io.reactivex.rxjava3.core.Observable",
          "io.smallrye.mutiny.Uni",
          "io.smallrye.mutiny.Multi"
  );
}
