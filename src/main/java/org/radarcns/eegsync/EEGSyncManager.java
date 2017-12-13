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

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.ArrayMap;

import org.radarcns.android.auth.AppSource;
import org.radarcns.android.data.DataCache;
import org.radarcns.android.device.AbstractDeviceManager;
import org.radarcns.android.device.DeviceStatusListener;
import org.radarcns.kafka.ObservationKey;
import org.radarcns.passive.eegsync.EegSyncPulse;
import org.radarcns.topic.AvroTopic;
import org.radarcns.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;


/** Manages EEG synchronization pulses */
public class EEGSyncManager extends AbstractDeviceManager<EEGSyncService, EEGSyncStatus> {
    private static final Logger logger = LoggerFactory.getLogger(EEGSyncManager.class);

    private Pattern[] accepTopicIds;
    private final AvroTopic<ObservationKey, EegSyncPulse> eegSyncPulseTopic;

    private final ExecutorService executor;
    private Future<?> pulseFuture;

    private static final String GPIO_FILE_PATH = "/sys/class/gpio/gpio177/value";
    private static final int PULSE_WIDTH_MS = 10;
    private static final int STATIC_DELAY_MS = 1000;
    private static final int MIN_DELAY_MS = 500;
    private static final int MAX_DELAY_MS = 1500;


    public EEGSyncManager(EEGSyncService service) {
        super(service);
        this.eegSyncPulseTopic = createTopic("android_eeg_sync_pulse", EegSyncPulse.class);

        this.executor = Executors.newSingleThreadExecutor();
    }


    @Override
    public void close() throws IOException {
        if (isClosed()) {
            return;
        }
        super.close();
        logger.info("EEG sync closing device {}", this);

        executor.shutdown();
    }

    /*
     * DeviceManager interface
     */

    @Override
    public void start(@NonNull final Set<String> accepTopicIds) {
        logger.info("EEG sync starting.");

        synchronized (this) {
            this.accepTopicIds = Strings.containsPatterns(accepTopicIds);
        }

        // schedule new eeg sync pulse
        pulseFuture = executor.submit(new Runnable() {
            @Override
            public void run() {
                while (!isClosed()) {
                    int delay_ms = ThreadLocalRandom.current().nextInt(MIN_DELAY_MS, MAX_DELAY_MS + 1);
                    try {
                        Thread.sleep(delay_ms);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    sync_pulse(PULSE_WIDTH_MS, delay_ms);
                }
            }
        });

        setName("EEG Sync");

        updateStatus(DeviceStatusListener.Status.READY);

        updateStatus(DeviceStatusListener.Status.CONNECTED);
    }


    protected synchronized void updateStatus(DeviceStatusListener.Status status) {
        if (status == getState().getStatus()) return;

        super.updateStatus(status);
    }

    public void sync_pulse(int width_ms, int delay_ms) {
        logger.debug("EEG sync sending {}ms pulse after a delay of {}ms.", width_ms, delay_ms);
        double stamp = System.currentTimeMillis() / 1000d;

        try {
            FileOutputStream fos = new FileOutputStream(new File(GPIO_FILE_PATH), false);
            fos.write(49);
            SystemClock.sleep(width_ms);
            fos.write(48);
            fos.close();
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }

        getState().setPulseWidth(width_ms);
        getState().setPulseDelay(delay_ms);
        float latestWidth = getState().getPulseWidth();
        float latestDelay = getState().getPulseDelay();

        EegSyncPulse pulse = new EegSyncPulse(stamp, System.currentTimeMillis() / 1000d, latestWidth, latestDelay);
        send(eegSyncPulseTopic, pulse);
        logger.debug("EEG sync pulse sent.");
    }

    @Override
    protected void registerDeviceAtReady() {
        // custom registration
        super.registerDeviceAtReady();
    }

    @Override
    public void didRegister(AppSource source) {
        super.didRegister(source);
        getState().getId().setSourceId(source.getSourceId());
    }

}
