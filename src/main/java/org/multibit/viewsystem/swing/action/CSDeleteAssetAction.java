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
package org.multibit.viewsystem.swing.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import org.multibit.model.bitcoin.WalletAssetTableData;
import org.multibit.viewsystem.swing.WalletAssetSummaryTableModel;
import org.coinspark.wallet.CSAsset;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.utils.ImageLoader;
import org.multibit.viewsystem.swing.MultiBitFrame;
import org.multibit.viewsystem.swing.view.panels.CSShowAssetsPanel;
import javax.swing.JComponent;
import org.multibit.utils.CSMiscUtils;
import com.google.bitcoin.core.Wallet;

/**
 * Action to delete a CSAsset from asset db.
 */
// ActionEvent object is a table, and string is the row
public class CSDeleteAssetAction extends AbstractAction {
    private BitcoinController bitcoinController;
    private JComponent view;
    private Object caller;
    
    public CSDeleteAssetAction(BitcoinController bitcoinController, JComponent view, Object caller) {
	super(bitcoinController.getLocaliser().getString("CSDeleteAssetManuallyAction.text"), ImageLoader.fatCow16(ImageLoader.FATCOW.delete));
	this.bitcoinController = bitcoinController;
	this.view = view;
	this.caller = caller;
        putValue(SHORT_DESCRIPTION, bitcoinController.getLocaliser().getString("CSDeleteAssetManuallyAction.tooltip"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

	WalletAssetTableData data = null;
	JComponent dataView = null;
	
	// Custom code for this type of action
	if (this.caller instanceof MultiBitFrame) {
//	    JTable table = (JTable) e.getSource();
//	    int row = Integer.parseInt(e.getActionCommand());
//	    WalletAssetSummaryTableModel model = (WalletAssetSummaryTableModel) table.getModel();
//	    data = model.getRow(row);
//	    dataView = table;
	} else if (this.caller instanceof CSShowAssetsPanel) {
	    data = ((CSShowAssetsPanel)this.caller).getSelectedRowData();
	    dataView = this.view;
	}
	
	if (data != null) {
	    CSAsset asset = data.getAsset();
	    if (asset != null) {
		String name = asset.getNameShort();
		int n = JOptionPane.showConfirmDialog(
			this.view,
			"Are you sure you want to delete" + 
			((name == null) ? " the asset" : (" '" + name + "'")) +
			"?\n\n" +
			"A valid asset will always exist in the Bitcoin network, so you can add it\n" + 
			"back to your wallet by keeping a copy of the asset reference below:\n\n" + CSMiscUtils.getHumanReadableAssetRef(asset) + "\n\n",
			"Delete Asset",
			JOptionPane.YES_NO_OPTION);
		if (JOptionPane.YES_OPTION == n) {
		    // DELETE IT
		    Wallet wallet = this.bitcoinController.getModel().getActiveWallet();
		    wallet.CS.deleteAsset(asset);
		}
	    }
	}
    }
}
