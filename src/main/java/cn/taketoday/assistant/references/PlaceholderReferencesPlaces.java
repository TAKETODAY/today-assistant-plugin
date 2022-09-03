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

package cn.taketoday.assistant.references;

import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.AnnotationConstant;
import kotlin.TuplesKt;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;

public final class PlaceholderReferencesPlaces {

  private static final List<String> VALUE_ATTRS = List.of("value");
  private static final List<String> VALUE_PATH_ATTRS = List.of("value", "path");
  private static final List<String> VALUE_URL_ATTRS = List.of("value", "url");

  // @formatter:off
  public static final Map<String, List<String>> PLACEHOLDER_ANNOTATIONS = MapsKt.mapOf(
          TuplesKt.to(AnnotationConstant.VALUE, VALUE_ATTRS),
          TuplesKt.to(AnnotationConstant.SCHEDULED, CollectionsKt.listOf("cron", "zone", "fixedDelayString", "fixedRateString", "initialDelayString")),
          TuplesKt.to(AnnotationConstant.PROPERTY_SOURCE, VALUE_ATTRS),
          TuplesKt.to(AnnotationConstant.TEST_PROPERTY_SOURCE, VALUE_ATTRS),
          TuplesKt.to("cn.taketoday.web.annotation.GET", VALUE_PATH_ATTRS),
          TuplesKt.to("cn.taketoday.web.annotation.POST", VALUE_PATH_ATTRS),
          TuplesKt.to("cn.taketoday.web.annotation.PUT", VALUE_PATH_ATTRS),
          TuplesKt.to("cn.taketoday.web.annotation.PATCH", VALUE_PATH_ATTRS),
          TuplesKt.to("cn.taketoday.web.annotation.DELETE", VALUE_PATH_ATTRS),
          TuplesKt.to("cn.taketoday.web.annotation.ActionMapping", VALUE_PATH_ATTRS),
          TuplesKt.to("cn.taketoday.web.annotation.RequestMapping", VALUE_PATH_ATTRS),
          TuplesKt.to("cn.taketoday.web.annotation.GetMapping", VALUE_PATH_ATTRS),
          TuplesKt.to("cn.taketoday.web.annotation.PostMapping", VALUE_PATH_ATTRS),
          TuplesKt.to("cn.taketoday.web.annotation.PutMapping", VALUE_PATH_ATTRS),
          TuplesKt.to("cn.taketoday.web.annotation.PatchMapping", VALUE_PATH_ATTRS),
          TuplesKt.to("cn.taketoday.web.annotation.DeleteMapping", VALUE_PATH_ATTRS),
          TuplesKt.to("cn.taketoday.web.service.annotation.HttpExchange", VALUE_URL_ATTRS),
          TuplesKt.to("cn.taketoday.web.service.annotation.GetExchange", VALUE_URL_ATTRS),
          TuplesKt.to("cn.taketoday.web.service.annotation.HeadExchange", VALUE_URL_ATTRS),
          TuplesKt.to("cn.taketoday.web.service.annotation.DeleteExchange", VALUE_URL_ATTRS),
          TuplesKt.to("cn.taketoday.web.service.annotation.OptionsExchange", VALUE_URL_ATTRS),
          TuplesKt.to("cn.taketoday.web.service.annotation.PutExchange", VALUE_URL_ATTRS),
          TuplesKt.to("cn.taketoday.web.service.annotation.PostExchange", VALUE_URL_ATTRS),
          TuplesKt.to("cn.taketoday.web.service.annotation.PatchExchange", VALUE_URL_ATTRS),
          TuplesKt.to("cn.taketoday.amqp.rabbit.annotation.Queue", CollectionsKt.listOf("value", "name", "durable", "exclusive", "autoDelete", "ignoreDeclarationExceptions")),
          TuplesKt.to("cn.taketoday.amqp.rabbit.annotation.Exchange", CollectionsKt.listOf("value", "name")),
          TuplesKt.to("cn.taketoday.amqp.rabbit.annotation.QueueBinding", CollectionsKt.listOf("key", "durable", "type", "internal", "ignoreDeclarationExceptions", "delayed")),
          TuplesKt.to("cn.taketoday.amqp.rabbit.annotation.RabbitListener", CollectionsKt.listOf("queues", "priority")),
          TuplesKt.to("cn.taketoday.jms.annotation.JmsListener", CollectionsKt.listOf("destination")),
          TuplesKt.to("cn.taketoday.kafka.annotation.KafkaListener", CollectionsKt.listOf("id", "topics", "topicPattern", "containerGroup", "groupId", "clientIdPrefix", "concurrency", "autoStartup", "properties", "info")),
          TuplesKt.to("cn.taketoday.kafka.annotation.RetryableTopic", CollectionsKt.listOf("attempts", "timeout", "autoCreateTopics", "numPartitions", "replicationFactor", "includeNames", "excludeNames", "traversingCauses", "retryTopicSuffix", "dltTopicSuffix", "autoStartDltHandler")),
          TuplesKt.to("cn.taketoday.cloud.openfeign.FeignClient", CollectionsKt.listOf("url", "name", "value", "path"))
  );

}
