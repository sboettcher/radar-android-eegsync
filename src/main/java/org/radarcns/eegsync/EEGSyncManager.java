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

import android.content.Context;
import android.support.annotation.NonNull;

import org.radarcns.android.data.DataCache;
import org.radarcns.android.data.TableDataHandler;
import org.radarcns.android.device.DeviceManager;
import org.radarcns.android.device.DeviceStatusListener;
import org.radarcns.key.MeasurementKey;
import org.radarcns.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

//import android.widget.Toast;
//import org.radarcns.android.util.Boast;

/** Manages EEG synchronization pulses */
public class EEGSyncManager implements DeviceManager {
    private static final Logger logger = LoggerFactory.getLogger(EEGSyncManager.class);

    private final TableDataHandler dataHandler;
    private final Context context;
    private final DeviceStatusListener eegSyncService;

    private final DataCache<MeasurementKey, EEGSyncPulse> eegSyncPulseTable;

    private final EEGSyncStatus deviceStatus;

    private boolean isClosed;
    private String deviceName;
    private Pattern[] acceptableIds;

    private final ScheduledExecutorService executor;


    public EEGSyncManager(Context context, DeviceStatusListener eegSyncService, String groupId, TableDataHandler handler, EEGSyncTopics topics) {
        this.dataHandler = handler;
        this.eegSyncPulseTable = dataHandler.getCache(topics.getEegSyncPulseTopic());

        this.eegSyncService = eegSyncService;
        this.context = context;

        this.executor = Executors.newSingleThreadScheduledExecutor();

        synchronized (this) {
            this.deviceStatus = new EEGSyncStatus();
            this.deviceStatus.getId().setUserId(groupId);
            this.deviceName = null;
            this.isClosed = true;
            this.acceptableIds = null;
        }
    }


    public void close() {
        executor.shutdown();

        synchronized (this) {
            if (this.isClosed) {
                return;
            }
            logger.info("EEG sync closing device {}", deviceName);
            this.isClosed = true;
        }

        updateStatus(DeviceStatusListener.Status.DISCONNECTED);
    }


    /*
     * DeviceManager interface
     */

    @Override
    public void start(@NonNull final Set<String> acceptableIds) {
        logger.info("EEG sync starting.");

        synchronized (this) {
            this.acceptableIds = Strings.containsPatterns(acceptableIds);
            this.isClosed = false;
        }
    }

    @Override
    public synchronized boolean isClosed() {
        return isClosed;
    }

    @Override
    public EEGSyncStatus getState() {
        return deviceStatus;
    }

    @Override
    public synchronized String getName() {
        return deviceName;
    }

    @Override
    public synchronized boolean equals(Object other) {
        return other == this
                || other != null && getClass().equals(other.getClass())
                && deviceStatus.getId().getSourceId() != null
                && deviceStatus.getId().equals(((EEGSyncManager) other).deviceStatus.getId());
    }

    @Override
    public int hashCode() {
        return deviceStatus.getId().hashCode();
    }

    private synchronized void updateStatus(DeviceStatusListener.Status status) {
        if (status == deviceStatus.getStatus()) return;
        this.deviceStatus.setStatus(status);

        this.eegSyncService.deviceStatusUpdated(this, status);
    }

}
