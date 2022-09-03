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

import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;

class ReadyStateLiveProperty extends AbstractLiveProperty<Boolean> {
  private static final Logger LOG = Logger.getInstance(ReadyStateLiveProperty.class);

  private static final long STATE_CHECKING_RETRY_INTERVAL = 500;
  private static final long STATE_CHECKING_DELAY_INTERVAL = 1000;
  private static final int STATE_CHECKING_RETRY_COUNT = 20;
  private final LiveProperty<InfraModuleDescriptor> myModuleDescriptor;
  private final LiveProperty<String> myServiceUrl;
  private final LifecycleErrorHandler myErrorHandler;
  private final Object myLock;
  private Runnable myChecker;
  private long myStart;
  private long myRetryCount;
  private volatile boolean myDisposed;
  private static final long STATE_CHECKING_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
  private static final long SERVICE_URL_RETRIEVING_TIMEOUT = TimeUnit.MINUTES.toMillis(1);

  ReadyStateLiveProperty(LiveProperty<InfraModuleDescriptor> moduleDescriptor,
          LiveProperty<String> serviceUrl, LifecycleErrorHandler errorHandler, Disposable parent) {
    super(Boolean.FALSE);
    this.myLock = new Object();
    this.myDisposed = false;
    this.myModuleDescriptor = moduleDescriptor;
    this.myErrorHandler = errorHandler;
    this.myServiceUrl = serviceUrl;
    this.myServiceUrl.addPropertyListener(new LivePropertyListener() {

      @Override
      public void propertyChanged() { }

      @Override
      public void computationFinished() {
        if (!StringUtil.isEmpty(myServiceUrl.getValue())) {
          startChecking();
        }
      }

      @Override
      public void computationFailed(Exception e) {
        Throwable cause = e.getCause();
        if ((e instanceof LifecycleException) && cause != null) {
          synchronized(myLock) {
            myRetryCount++;
            if (myRetryCount < STATE_CHECKING_RETRY_COUNT && System.currentTimeMillis() - myStart <= SERVICE_URL_RETRIEVING_TIMEOUT) {
              ScheduledExecutorService scheduler = JobScheduler.getScheduler();
              scheduler.schedule(myServiceUrl::compute, STATE_CHECKING_RETRY_INTERVAL, TimeUnit.MILLISECONDS);
              return;
            }
            myErrorHandler.handleLifecycleError(message("infra.application.endpoints.error.failed.to.retrieve.jmx.service.url"));
          }
        }
        setValue(null);
        Exception toReport = (!(e instanceof LifecycleException) || !(cause instanceof Exception)) ? e : (Exception) cause;
        getListeners().forEach(listener -> {
          listener.computationFailed(toReport);
        });
        getListeners().forEach(LivePropertyListener::computationFinished);
      }
    });
    if (parent != null) {
      Disposer.register(parent, this);
    }
  }

  public void dispose() {
    this.myDisposed = true;
  }

  @Override
  public void compute() {
    synchronized(this.myLock) {
      this.myStart = System.currentTimeMillis();
      this.myRetryCount = 0L;
    }
    ScheduledExecutorService scheduler = JobScheduler.getScheduler();
    scheduler.schedule(myServiceUrl::compute, STATE_CHECKING_DELAY_INTERVAL, TimeUnit.MILLISECONDS);
  }

  private void startChecking() {
    synchronized(this.myLock) {
      if (this.myChecker != null) {
        return;
      }
      setValue(Boolean.FALSE);
      this.myChecker = new Runnable() {
        @Override
        public void run() {
          if (myDisposed || Boolean.TRUE.equals(getValue())) {
            return;
          }
          boolean timeoutExceeded = false;
          synchronized(myLock) {
            if (System.currentTimeMillis() - myStart > STATE_CHECKING_TIMEOUT) {
              setValue(null);
              myChecker = null;
              timeoutExceeded = true;
            }
          }
          if (timeoutExceeded) {
            Exception e = new LifecycleException(message("infra.application.endpoints.application.ready.check.timeout.exceeded"), null);
            getListeners().forEach(listener -> {
              listener.computationFailed(e);
            });
            getListeners().forEach(LivePropertyListener::computationFinished);
            return;
          }
          var moduleDescriptor = Objects.requireNonNull(myModuleDescriptor.getValue());
          try {
            var connector = new InfraApplicationConnector(myServiceUrl.getValue(), moduleDescriptor);
            if (connector.isReady()) {
              setValue(Boolean.TRUE);
              getListeners().forEach(LivePropertyListener::propertyChanged);
              getListeners().forEach(LivePropertyListener::computationFinished);
              connector.close();
              return;
            }
            connector.close();
            JobScheduler.getScheduler().schedule(this, STATE_CHECKING_RETRY_INTERVAL, TimeUnit.MILLISECONDS);
          }
          catch (Exception e2) {
            boolean retryCountExceeded = false;
            synchronized(myLock) {
              myRetryCount++;
              if (myRetryCount >= STATE_CHECKING_RETRY_COUNT) {
                setValue(null);
                myChecker = null;
                retryCountExceeded = true;
              }
              if (retryCountExceeded) {
                LOG.debug(e2);
                getListeners().forEach(listener2 -> {
                  listener2.computationFailed(e2);
                });
                getListeners().forEach(LivePropertyListener::computationFinished);
              }
            }
          }
        }
      };
      ApplicationManager.getApplication().executeOnPooledThread(this.myChecker);
    }
  }
}
