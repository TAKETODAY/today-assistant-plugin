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

package cn.taketoday.assistant.app.run.lifecycle;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;

import java.util.function.Consumer;

abstract class AsyncProperty<T> extends AbstractProperty<T> {
  protected static final Logger LOG = Logger.getInstance(AsyncProperty.class);

  private final LifecycleErrorHandler errorHandler;
  private volatile boolean disposed;

  protected abstract T doCompute() throws LifecycleException;

  protected AsyncProperty(LifecycleErrorHandler errorHandler, Disposable parent) {
    this(errorHandler, parent, null);
  }

  protected AsyncProperty(LifecycleErrorHandler errorHandler, Disposable parent, T defaultValue) {
    super(defaultValue);
    this.errorHandler = errorHandler;
    if (parent != null) {
      Disposer.register(parent, this);
    }
  }

  @Override
  public void compute() {
    if (this.disposed) {
      return;
    }
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      if (this.disposed) {
        return;
      }
      try {
        try {
          T value = doCompute();
          T oldValue = setValue(value);
          if (!Comparing.equal(oldValue, value)) {
            dispatchPropertyChanged();
          }
          dispatchComputationFinished();
        }
        catch (LifecycleException e) {
          LOG.debug(e.getCause());
          if (e.getMessage() != null) {
            errorHandler.handleLifecycleError(e.getMessage());
          }
          setValue(null);
          Throwable cause = e.getCause();
          if (cause == null) {
            dispatchComputationFinished();
            return;
          }

          Exception toNotify = cause instanceof Exception
                               ? (Exception) cause
                               : new LifecycleException(cause.getMessage(), cause);
          dispatchComputationFailed(toNotify);
          dispatchComputationFinished();
        }
      }
      catch (Throwable th) {
        dispatchComputationFinished();
        throw th;
      }
    });
  }

  @Override
  public void dispose() {
    this.disposed = true;
  }

  protected boolean isDisposed() {
    return this.disposed;
  }
}
