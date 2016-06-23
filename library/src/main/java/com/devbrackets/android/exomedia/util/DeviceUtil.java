/*
 * Copyright (C) 2016 Brian Wernick
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

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.LinkedList;
import java.util.List;

/**
 * A Utility class to help determine characteristics about the device
 */
public class DeviceUtil {
    protected static final List<NonCompatibleDevice> NON_COMPATIBLE_DEVICES;

    static {
        NON_COMPATIBLE_DEVICES = new LinkedList<>();
        NON_COMPATIBLE_DEVICES.add(new NonCompatibleDevice("Amazon"));
    }

    @SuppressWarnings("SimplifiableIfStatement")
    public boolean supportsExoPlayer(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && !isNotCompatible(NON_COMPATIBLE_DEVICES)) {
            return true;
        }

        //Because Amazon Kindles are popular devices, we add a specific check for them
        return Build.MANUFACTURER.equalsIgnoreCase("Amazon") && (isDeviceTV(context) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
    }

    /**
     * Determines if the current device is not compatible based on the list of devices
     * that don't correctly support the ExoPlayer
     *
     * @param nonCompatibleDevices The list of devices that aren't compatible
     * @return True if the current device is not compatible
     */
    public boolean isNotCompatible(@NonNull List<NonCompatibleDevice> nonCompatibleDevices) {
        for (NonCompatibleDevice device : nonCompatibleDevices) {
            if (Build.MANUFACTURER.equalsIgnoreCase(device.getManufacturer())) {
                if (device.ignoreModel()) {
                    return true;
                }

                if (Build.DEVICE.equalsIgnoreCase(device.getModel())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determines if the current device is a TV.
     *
     * @param context The context to use for determining the device information
     * @return True if the current device is a TV
     */
    public boolean isDeviceTV(Context context) {
        //Since Android TV is only API 21+ that is the only time we will compare configurations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UiModeManager uiManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
            return uiManager != null && uiManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
        }

        return false;
    }

    public static class NonCompatibleDevice {
        /***
         * True if we should treat all devices from the manufacturer as non compliant
         */
        private boolean ignoreModel;
        private final String model;
        private final String manufacturer;

        public NonCompatibleDevice(@NonNull String manufacturer) {
            this.manufacturer = manufacturer;
            this.model = null;
            this.ignoreModel = true;
        }

        public NonCompatibleDevice(@NonNull String model, @NonNull String manufacturer) {
            this.model = model;
            this.manufacturer = manufacturer;
        }

        public boolean ignoreModel() {
            return ignoreModel;
        }

        public String getModel() {
            return model;
        }

        public String getManufacturer() {
            return manufacturer;
        }
    }
}
