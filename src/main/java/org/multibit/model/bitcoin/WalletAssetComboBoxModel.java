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

import javax.swing.DefaultComboBoxModel;
import org.multibit.controller.Controller;
import org.multibit.controller.bitcoin.BitcoinController;
//import org.multibit.exchange.CurrencyConverter;
//import org.multibit.exchange.CurrencyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Wallet;
import org.coinspark.wallet.CSAsset;
import java.math.BigInteger;
import org.coinspark.protocol.CoinSparkAssetRef;
import org.multibit.utils.CSMiscUtils;

public class WalletAssetComboBoxModel extends DefaultComboBoxModel<WalletAssetComboBoxItem>{
    
    public static final int NUMBER_OF_CONFIRMATIONS_TO_SEND_ASSET_THRESHOLD = 3;
    
    private final Controller controller;
    private final BitcoinController bitcoinController;

    public WalletAssetComboBoxModel(BitcoinController bitcoinController) {
	super();

        this.bitcoinController = bitcoinController;
        this.controller = this.bitcoinController;
	
	updateItems();
    }
    
    /*
    Update the box items based upon active wallet.
    Only want valid assets with unspent balance.
    
    From CoinSpark.org: The selector should then list all assets in the Asset Type List for which isVisible is true and assetRef and genesis are not null. Due to the possibility of a blockchain rearrangement, it is strongly recommended not to allow assets to be sent out if there have been less than 6 blockchain confirmations since their assetRef.blockNum.
    */
    public void updateItems() {
	int selectedId = 0; // default is BTC
	WalletAssetComboBoxItem obj = (WalletAssetComboBoxItem)getSelectedItem();
	if (obj!=null) {
	    selectedId = obj.getId();
	}
	
	removeAllElements();
	
	WalletAssetComboBoxItem item = new WalletAssetComboBoxItem(null);
	addElement(item);
	
	Wallet wallet = this.bitcoinController.getModel().getActiveWallet();
	if (wallet != null) {
	    int[] array_ids = wallet.CS.getAssetIDs();
	    if (array_ids != null) {
		for (int i : array_ids) {
		    Wallet.CoinSpark.AssetBalance assetBalance = wallet.CS.getAssetBalance(i);
		    BigInteger x = assetBalance.total; //wallet.CS.getUnspentAssetQuantity(i); 
		    if (x.compareTo(BigInteger.ZERO)==1) {
			CSAsset asset = wallet.CS.getAsset(i);
			if (asset != null && asset.isVisible()) {
			    // Must have at least 6 confirmations.
			    int lastHeight = wallet.getLastBlockSeenHeight();
			    CoinSparkAssetRef assetRef = asset.getAssetReference();
			    if (assetRef != null) {
				final int blockIndex = (int) assetRef.getBlockNum();
				final int numConfirmations = lastHeight - blockIndex + 1; // 0 means no confirmation, 1 is yes for sa
				int threshold = NUMBER_OF_CONFIRMATIONS_TO_SEND_ASSET_THRESHOLD;
				// FIXME: REMOVE/COMMENT OUT BEFORE RELEASE?
				String sendAssetWithJustOneConfirmation = controller.getModel().getUserPreference("sendAssetWithJustOneConfirmation");
				if (Boolean.TRUE.toString().equals(sendAssetWithJustOneConfirmation)) {
				    threshold = 1;
				}
				//System.out.println(">>>> " + CSMiscUtils.getHumanReadableAssetRef(asset) + " num confirmations " + numConfirmations + ", threshold = " + threshold);
				if (numConfirmations >= threshold) {
				    item = new WalletAssetComboBoxItem(asset);
				    addElement(item);
				    if (selectedId == asset.getAssetID()) {
					setSelectedItem(item);
				    }
				}
			    }
 			}
		    }
		}
	    }
	}

    }
    
    // remove disabled flag on non-BTC assets
    public void enableAssets() {
	int n = getSize();
	for (int i=1; i<n; i++) {
	    WalletAssetComboBoxItem item = getElementAt(i);
	    item.setDisabled(false);
	}	
    }
    
    // set disabled flag on non-BTC assets
    public void disableAssets() {
	int n = getSize();
	for (int i=1; i<n; i++) {
	    WalletAssetComboBoxItem item = getElementAt(i);
	    item.setDisabled(true);
	}
    }

}
