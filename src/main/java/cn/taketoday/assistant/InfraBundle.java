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

import com.intellij.DynamicBundle;

import java.util.function.Supplier;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 01:19
 */
public final class InfraBundle extends DynamicBundle {
  private static final String PATH_TO_BUNDLE = "messages.InfraBundle";
  private static final InfraBundle ourInstance = new InfraBundle();

  public static String message(String key, Object... params) {
    return ourInstance.getMessage(key, params);
  }

  public static Supplier<String> messagePointer(String key, Object... params) {
    return ourInstance.getLazyMessage(key, params);
  }

  private InfraBundle() {
    super(PATH_TO_BUNDLE);
  }

}
