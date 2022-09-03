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

abstract class AsyncLiveProperty<T> extends AbstractLiveProperty<T> {
  protected static final Logger LOG = Logger.getInstance(AsyncLiveProperty.class);
  private final LifecycleErrorHandler myErrorHandler;
  private volatile boolean myDisposed;

  protected abstract T doCompute() throws LifecycleException;

  protected AsyncLiveProperty(LifecycleErrorHandler errorHandler, Disposable parent) {
    this(errorHandler, parent, null);
  }

  protected AsyncLiveProperty(LifecycleErrorHandler errorHandler, Disposable parent, T defaultValue) {
    super(defaultValue);
    this.myErrorHandler = errorHandler;
    if (parent != null) {
      Disposer.register(parent, this);
    }
  }

  @Override
  public void compute() {
    if (this.myDisposed) {
      return;
    }
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      if (this.myDisposed) {
        return;
      }
      try {
        try {
          T value = doCompute();
          T oldValue = setValue(value);
          if (!Comparing.equal(oldValue, value)) {
            getListeners().forEach(LivePropertyListener::propertyChanged);
          }
          getListeners().forEach(LivePropertyListener::computationFinished);
        }
        catch (LifecycleException e) {
          LOG.debug(e.getCause());
          if (e.getMessage() != null) {
            this.myErrorHandler.handleLifecycleError(e.getMessage());
          }
          setValue(null);
          Throwable cause = e.getCause();
          if (cause == null) {
            getListeners().forEach(LivePropertyListener::computationFinished);
            return;
          }
          Exception toNotify = cause instanceof Exception ? (Exception) cause : new LifecycleException(cause.getMessage(), cause);
          getListeners().forEach(listener3 -> {
            listener3.computationFailed(toNotify);
          });
          getListeners().forEach(LivePropertyListener::computationFinished);
        }
      }
      catch (Throwable th) {
        getListeners().forEach(LivePropertyListener::computationFinished);
        throw th;
      }
    });
  }

  public void dispose() {
    this.myDisposed = true;
  }

  protected boolean isDisposed() {
    return this.myDisposed;
  }
}
