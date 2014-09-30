/* 
 * SparkBit
 *
 * Copyright 2011-2014 multibit.org
 * Copyright 2014 Coin Sciences Ltd
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.multibit.model.bitcoin;


/**
 * Interface to implement if you are interested in hearing about when the state of a wallets busy status changes.
 * 
 * @author jim
 *
 */
public interface WalletBusyListener {
    /**
     * The Wallet wallets busy status has changed to 'newWalletIsBusy'.
     * 
     * @param newWalletIsBusy
     */
    public void walletBusyChange(boolean newWalletIsBusy);
}
