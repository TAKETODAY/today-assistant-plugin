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

package cn.taketoday.assistant.code.cache;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
public interface CacheableConstant {
  String CACHEABLE = "cn.taketoday.cache.annotation.Cacheable";
  String CACHE_EVICT = "cn.taketoday.cache.annotation.CacheEvict";
  String CACHE_PUT = "cn.taketoday.cache.annotation.CachePut";
  String CACHE_CONFIG = "cn.taketoday.cache.annotation.CacheConfig";
  String ENABLE_CACHING = "cn.taketoday.cache.annotation.EnableCaching";
  String CACHING = "cn.taketoday.cache.annotation.Caching";
  String KEY_GENERATOR_CLASS = "cn.taketoday.cache.interceptor.KeyGenerator";
  String CACHE_MANAGER_CLASS = "cn.taketoday.cache.CacheManager";
  String CACHE_RESOLVER_CLASS = "cn.taketoday.cache.interceptor.CacheResolver";
  String ROOT_OBJECT_CLASS = "cn.taketoday.cache.interceptor.CacheExpressionRootObject";
  String CACHE_CLASSNAME = "cn.taketoday.cache.Cache";
  String CAFFEINE_CLASSNAME = "com.github.benmanes.caffeine.cache.Caffeine";
  String JCACHE_CACHE_MANAGER = "cn.taketoday.cache.jcache.JCacheCacheManager";
  String REDIS_CACHE_MANAGER = "cn.taketoday.data.redis.cache.RedisCacheManager";
}
