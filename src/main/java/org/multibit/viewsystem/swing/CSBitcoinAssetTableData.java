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
package org.multibit.viewsystem.swing;

import org.multibit.model.bitcoin.WalletAssetTableData;

/*
 Dummy class so we can have a Bitcoin entry in the asset summary table.
 */
public class CSBitcoinAssetTableData extends WalletAssetTableData {

//    public boolean hasDataChanged;
    public String dataChangedText;  // if not null, means data has changed so text should be shown
    public String dataChangedToolTipText;
    public String balanceText;		// this is really the estimated
    public String balanceToolTipText;
    public String spendableText;	// this is really the available
    public String spendableToolTipText;
    public String syncText; // if sync text exists, show next to balance.
    public String syncToolTipText;
    public String syncPercentText;
    public String syncPercentToolTipText;
    
    public CSBitcoinAssetTableData() {
	super(null); // no CSAsset
    }
    
    public void clear() {
//	hasDataChanged = false;
	dataChangedText = null;
	dataChangedToolTipText = null;
	balanceText = null;
	balanceToolTipText = null;
	spendableText = null;
	spendableToolTipText = null;
	syncText = null;
	syncToolTipText = null;
	syncPercentText = null;
	syncPercentToolTipText = null;
    }
}
