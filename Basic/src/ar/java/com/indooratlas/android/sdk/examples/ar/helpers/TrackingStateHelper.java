/*
 * Copyright 2019 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.indooratlas.android.sdk.examples.ar.helpers;

import android.app.Activity;
import android.view.WindowManager;

/** Gets human readibly tracking failure reasons and suggested actions. */
public final class TrackingStateHelper {
  public static final String INSUFFICIENT_FEATURES_MESSAGE =
      "Can't find anything. Aim device at a surface with more texture or color.";
  public static final String EXCESSIVE_MOTION_MESSAGE = "Moving too fast. Slow down.";
  public static final String INSUFFICIENT_LIGHT_MESSAGE =
      "Too dark. Try moving to a well-lit area.";
  public static final String BAD_STATE_MESSAGE =
      "Tracking lost due to bad internal state. Please try restarting the AR experience.";
  public static final String CAMERA_UNAVAILABLE_MESSAGE =
      "Another app is using the camera. Tap on this app or try closing the other one.";

  private final Activity activity;

  private boolean previousShouldBeOn;

  public TrackingStateHelper(Activity activity) {
    this.activity = activity;
  }

  /** Keep the screen unlocked while tracking, but allow it to lock when tracking stops. */
  public void updateKeepScreenOnFlag(final boolean shouldBeOn) {
    if (shouldBeOn == previousShouldBeOn) {
      return;
    }

    previousShouldBeOn = shouldBeOn;
    activity.runOnUiThread(
      new Runnable() {
        @Override
        public void run() {
          if (shouldBeOn) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          }
        }
      });
  }
}
