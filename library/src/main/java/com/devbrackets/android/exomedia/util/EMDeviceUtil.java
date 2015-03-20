/*
 * Copyright (C) 2015 Brian Wernick
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.devbrackets.android.exomedia.util;

import android.os.Build;

/**
 * A Utility class to help determine characteristics about the device
 */
public class EMDeviceUtil {
    private static final String[] NON_CTS_COMPLIANT_MANUFACTURERS;
    private static final String[] NON_CTS_COMPLIANT_DEVICES;

    static {
        NON_CTS_COMPLIANT_MANUFACTURERS = new String[] {
            "Amazon"
        };

        NON_CTS_COMPLIANT_DEVICES = new String[] {

        };
    }

    /**
     * Determines if the current device has passed the Android Compatibility Test
     * Suite (CTS).  This will indicate if we can trust the exoPlayer to run correctly
     * on this device.
     *
     * @return True if the device is CTS compliant.
     */
    public static boolean isDeviceCTSCompliant() {
        //Until we can find a better way to do this, we will just list the devices and manufacturers known to not follow compliance
        for (String manufacturer: NON_CTS_COMPLIANT_MANUFACTURERS) {
            if (Build.MANUFACTURER.equalsIgnoreCase(manufacturer)) {
                return false;
            }
        }

        for (String device: NON_CTS_COMPLIANT_DEVICES) {
            if (Build.MANUFACTURER.equalsIgnoreCase(device)) {
                return false;
            }
        }

        return true;
    }
}
