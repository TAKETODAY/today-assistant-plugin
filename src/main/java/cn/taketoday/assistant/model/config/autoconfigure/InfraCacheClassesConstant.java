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

@Deprecated
public interface InfraCacheClassesConstant {
  String CACHE_CONFIGURATION_IMPORT_SELECTOR =
          "cn.taketoday.boot.autoconfigure.cache.CacheAutoConfiguration.CacheConfigurationImportSelector";

  String GENERIC_CACHE_CONFIGURATION = "cn.taketoday.boot.autoconfigure.cache.GenericCacheConfiguration";
  String EL_CACHE_CACHE_CONFIGURATION = "cn.taketoday.boot.autoconfigure.cache.EhCacheCacheConfiguration";
  String JCACHE_CACHE_CONFIGURATION = "cn.taketoday.boot.autoconfigure.cache.JCacheCacheConfiguration";
  String CAFFEINE_CACHE_CONFIGURATION = "cn.taketoday.boot.autoconfigure.cache.CaffeineCacheConfiguration";
  String SIMPLE_CACHE_CONFIGURATION = "cn.taketoday.boot.autoconfigure.cache.SimpleCacheConfiguration";
}
