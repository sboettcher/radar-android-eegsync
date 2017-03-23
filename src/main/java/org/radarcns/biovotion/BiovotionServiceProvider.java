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

package org.radarcns.biovotion;

import android.os.Parcelable;

import org.radarcns.android.device.DeviceServiceProvider;

import java.util.Arrays;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;

public class BiovotionServiceProvider extends DeviceServiceProvider<BiovotionDeviceStatus> {
    @Override
    public Class<?> getServiceClass() {
        return BiovotionService.class;
    }

    @Override
    public Parcelable.Creator<BiovotionDeviceStatus> getStateCreator() {
        return BiovotionDeviceStatus.CREATOR;
    }

    @Override
    public boolean hasDetailView() {
        return true;
    }

    public void showDetailView() {
        new BiovotionHeartbeatToast(getActivity()).execute(getConnection());
    }

    @Override
    public List<String> needsPermissions() {
        return Arrays.asList(ACCESS_COARSE_LOCATION, BLUETOOTH, BLUETOOTH_ADMIN);
    }

    @Override
    public String getDisplayName() {
        return getActivity().getString(R.string.biovotionLabel);
    }
}
