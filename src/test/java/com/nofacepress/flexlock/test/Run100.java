/*
 * Copyright 2018,2020 No Face Press, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.nofacepress.flexlock.test;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class Run100 extends Runner {

  private static final int MAX_RUN_COUNT = 100;
  private Runner runner;

  public Run100(final Class<?> testClass) throws InitializationError {
    runner = new BlockJUnit4ClassRunner(testClass);
  }

  @Override
  public Description getDescription() {
    final Description description =
        Description.createSuiteDescription("Run " + MAX_RUN_COUNT + " times");
    description.addChild(runner.getDescription());
    return description;
  }

  @Override
  public void run(final RunNotifier notifier) {
    class L extends RunListener {
      boolean shouldContinue = true;
      int runCount = 0;

      @Override
      public void testFailure(@SuppressWarnings("unused") final Failure failure) throws Exception {
        shouldContinue = false;
      }

      @Override
      public void testFinished(@SuppressWarnings("unused") Description description)
          throws Exception {
        runCount++;

        shouldContinue = (shouldContinue && runCount < MAX_RUN_COUNT);
      }
    }

    final L listener = new L();
    notifier.addListener(listener);

    while (listener.shouldContinue) {
      runner.run(notifier);
    }
  }
}
