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

import org.radarcns.android.device.DeviceTopics;
import org.radarcns.passive.eegsync.EegSyncPulse;
import org.radarcns.topic.AvroTopic;
import org.radarcns.kafka.ObservationKey;

/** Topic manager for topics concerning the EEG Synchronization. */
public class EEGSyncTopics extends DeviceTopics {
    private final AvroTopic<ObservationKey, EegSyncPulse> eegSyncPulseTopic;

    private static final Object syncObject = new Object();
    private static EEGSyncTopics instance = null;

    public static EEGSyncTopics getInstance() {
        synchronized (syncObject) {
            if (instance == null) {
                instance = new EEGSyncTopics();
            }
            return instance;
        }
    }

    private EEGSyncTopics() {
        eegSyncPulseTopic = createTopic("android_eeg_sync_pulse",
                EegSyncPulse.getClassSchema(),
                EegSyncPulse.class);
    }

    public AvroTopic<ObservationKey, EegSyncPulse> getEegSyncPulseTopic() {
        return eegSyncPulseTopic;
    }
}
