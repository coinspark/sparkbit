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
package org.multibit.model.bitcoin;

import org.coinspark.protocol.CoinSparkAssetRef;
import org.coinspark.wallet.CSAsset;
import org.multibit.utils.CSMiscUtils;

/**
 * Object in combo box
 */
public class WalletAssetComboBoxItem {
    private int id;
    private String label;
    private String assetRef;
    private boolean disabled;

    public boolean isDisabled() {
	return disabled;
    }

    public void setDisabled(boolean disabled) {
	this.disabled = disabled;
    }

    public String getAssetRef() {
	return assetRef;
    }

    public void setAssetRef(String assetRef) {
	this.assetRef = assetRef;
    }

    public int getId() {
	return id;
    }

    public void setID(int id) {
	this.id = id;
    }

    public String getLabel() {
	return label;
    }

    public void setLabel(String label) {
	this.label = label;
    }
    
    /*
    @param CSAsset or null for Bitcoin
    */
    public WalletAssetComboBoxItem(CSAsset asset) {
	if (asset == null) {
	    id = 0;
	    label = "Bitcoin (BTC)";
	    return;
	}
	this.id = asset.getAssetID();
	
	// Get Asset reference and store it.
	//CoinSparkAssetRef assetRef = asset.getAssetReference();	
	String ref = CSMiscUtils.getHumanReadableAssetRef(asset);
	if (ref!=null) setAssetRef(ref);
	
	// Get domain and use it as part of display
	String domain = CSMiscUtils.getDomainHost(asset.getDomainURL());

	//assetRefLabel.setText((genref == null) ? "" : genref.toString());
	this.label = String.format("%s (%s)", asset.getNameShort(), (domain == null) ? "CAUTION: Unknown Domain" : domain);
	
	this.disabled = false;
    }
    
    public String toString() {
	return getLabel();
    }
}
