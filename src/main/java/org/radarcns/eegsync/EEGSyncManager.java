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
import android.os.SystemClock;
import android.support.annotation.NonNull;

import org.radarcns.android.RadarConfiguration;
import org.radarcns.android.data.DataCache;
import org.radarcns.android.data.TableDataHandler;
import org.radarcns.android.device.DeviceManager;
import org.radarcns.android.device.DeviceStatusListener;
import org.radarcns.key.MeasurementKey;
import org.radarcns.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import static org.radarcns.android.RadarConfiguration.SOURCE_ID_KEY;

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

    private final ExecutorService executor;
    private Future<?> pulseFuture;

    private static final String GPIO_FILE_PATH = "/sys/class/gpio/gpio177/value";
    private static final int PULSE_WIDTH_MS = 10;
    private static final int STATIC_DELAY_MS = 1000;
    private static final int MIN_DELAY_MS = 500;
    private static final int MAX_DELAY_MS = 1500;


    public EEGSyncManager(Context context, DeviceStatusListener eegSyncService, String groupId, TableDataHandler handler, EEGSyncTopics topics) {
        this.dataHandler = handler;
        this.eegSyncPulseTable = dataHandler.getCache(topics.getEegSyncPulseTopic());

        this.eegSyncService = eegSyncService;
        this.context = context;

        this.executor = Executors.newSingleThreadExecutor();

        synchronized (this) {
            this.deviceStatus = new EEGSyncStatus();
            setIDs(groupId, RadarConfiguration.getOrSetUUID(this.context.getApplicationContext(), SOURCE_ID_KEY));
            this.deviceStatus.getId().setUserId(groupId);
            this.deviceName = null;
            this.isClosed = true;
            this.acceptableIds = null;
        }
    }


    public void close() {
        synchronized (this) {
            if (this.isClosed) {
                return;
            }
            logger.info("EEG sync closing device {}", deviceName);
            this.isClosed = true;
        }

        executor.shutdown();

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

        // schedule new eeg sync pulse
        pulseFuture = executor.submit(new Runnable() {
            @Override
            public void run() {
                while (!isClosed) {
                    int delay_ms = ThreadLocalRandom.current().nextInt(MIN_DELAY_MS, MAX_DELAY_MS + 1);
                    try {
                        Thread.sleep(delay_ms);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    sync_pulse(delay_ms);
                }
            }
        });

        updateStatus(DeviceStatusListener.Status.CONNECTED);
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

    public void setIDs(String userId, String sourceId) {
        if (this.deviceStatus.getId().getUserId() == null) {
            this.deviceStatus.getId().setUserId(userId);
        }
        if (this.deviceStatus.getId().getSourceId() == null) {
            this.deviceStatus.getId().setSourceId(sourceId);
        }
    }





    public void sync_pulse(int delay_ms) {
        logger.info("EEG sync sending {}ms pulse after a delay of {}ms.", PULSE_WIDTH_MS, delay_ms);
        double stamp = System.currentTimeMillis() / 1000d;

        try {
            FileOutputStream fos = new FileOutputStream(new File(GPIO_FILE_PATH), false);
            fos.write(49);
            SystemClock.sleep(PULSE_WIDTH_MS);
            fos.write(48);
            fos.close();
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }

        EEGSyncPulse pulse = new EEGSyncPulse(stamp, System.currentTimeMillis() / 1000d);
        dataHandler.addMeasurement(eegSyncPulseTable, deviceStatus.getId(), pulse);
        logger.info("EEG sync pulse sent.");
    }

}
