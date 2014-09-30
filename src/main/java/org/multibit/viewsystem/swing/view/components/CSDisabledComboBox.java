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
package org.multibit.viewsystem.swing.view.components;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import org.multibit.model.bitcoin.WalletAssetComboBoxItem;

/**
 * If the model has isDisabled() getter on an item, don't allow selection
 */
public class CSDisabledComboBox extends JComboBox {

    public CSDisabledComboBox(ComboBoxModel<WalletAssetComboBoxItem> aModel) {
	super(aModel);
    }
    
    // If we invoke setSelectedIndex on super, then actionPerformed() is triggered.
    @Override
    public void setSelectedIndex(int index) {	
	WalletAssetComboBoxItem item = (WalletAssetComboBoxItem) getItemAt(index);
	if (!item.isDisabled()) {
	    super.setSelectedIndex(index);
	}
    }
    
}
