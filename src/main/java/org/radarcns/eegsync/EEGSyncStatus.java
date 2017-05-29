/*
 * Copyright 2017 Uniklinik Freiburg and The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarcns.eegsync;

import android.os.Parcel;
import android.os.Parcelable;

import org.radarcns.android.device.BaseDeviceState;
import org.radarcns.android.device.DeviceStateCreator;

/**
 * The status on a single point in time of the EEG synchronization.
 */
public class EEGSyncStatus extends BaseDeviceState {
    private float pulseWidth = Float.NaN;
    private float pulseDelay = Float.NaN;

    public static final Parcelable.Creator<EEGSyncStatus> CREATOR = new DeviceStateCreator<>(EEGSyncStatus.class);

    @Override
    public synchronized void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeFloat(this.pulseWidth);
        dest.writeFloat(this.pulseDelay);
    }

    public void updateFromParcel(Parcel in) {
        super.updateFromParcel(in);
        pulseWidth = in.readFloat();
        pulseDelay = in.readFloat();
    }


    /**
     * getter
     */

    public float getPulseWidth() { return pulseWidth; }

    public float getPulseDelay() { return pulseDelay; }



    /**
     * setter
     */

    public void setPulseWidth(float width) {
        this.pulseWidth = width;
    }

    public void setPulseDelay(float delay) {
        this.pulseDelay = delay;
    }
}
