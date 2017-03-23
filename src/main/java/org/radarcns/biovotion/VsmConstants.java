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

/**
 * Define some constants used in several activities and views.
 *
 * @author Christopher Métrailler (mei@hevs.ch)
 * @version 1.0 - 2016/07/20
 */
public final class VsmConstants {

  /**
   * Key used to pass the DiscoveredEntity descriptor as intent argument.
   * The descriptor of the discovered VSM device is serialized and passed between activities.
   */
  public final static String KEY_DESC_EXTRA = "key_extra_desc";

  /**
   * Default Bluetooth connection timeout. If this timeout is reached, the callback
   * VsmDeviceListener#onVsmDeviceConnectionError(VsmDevice, VsmConnectionState) is called.
   *
   * <p>
   * The default timeout on Android is 30 seconds. Using the BLE library, the maximum timeout value
   * is fixed to 25 seconds.
   * When connecting the first time to a VSM device, the connection time can be up to 5 seconds because the pairing
   * process can take time and it is based on some internal delays.
   */
  public final static int BLE_CONN_TIMEOUT_MS = 10000;
  
  private VsmConstants() {
    // Private
  }
}
