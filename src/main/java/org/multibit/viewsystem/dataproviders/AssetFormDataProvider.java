/* 
 * SparkBit
 *
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
package org.multibit.viewsystem.dataproviders;

//import java.math.BigInteger;

/**
 * DataProvider for send asset and send asset confirm action
 */
public interface AssetFormDataProvider extends BitcoinFormDataProvider { 
    /**
     * Get the type of the asset (BTC or (color*)
     */
    public int getAssetId();
    public String getAssetAmount();
    public boolean isSenderPays();
    
    public String getAssetAmountText(); // the raw text that was typed
}
