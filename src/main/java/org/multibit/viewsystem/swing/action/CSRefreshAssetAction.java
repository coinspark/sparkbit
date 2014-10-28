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
import org.coinspark.wallet.CSEventBus;
import org.coinspark.wallet.CSEventType;

/**
 * 
 */
// ActionEvent object is a table, and string is the row
public class CSRefreshAssetAction extends AbstractAction {
    private BitcoinController bitcoinController;
    private JComponent view;
    private Object caller;
    
    public CSRefreshAssetAction(BitcoinController bitcoinController, JComponent view, Object caller) {
	super(bitcoinController.getLocaliser().getString("CSRefreshAssetManuallyAction.text"), ImageLoader.fatCow16(ImageLoader.FATCOW.arrow_refresh));
	this.bitcoinController = bitcoinController;
	this.view = view;
	this.caller = caller;
        putValue(SHORT_DESCRIPTION, bitcoinController.getLocaliser().getString("CSRefreshAssetManuallyAction.tooltip"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

	WalletAssetTableData data = null;
	JComponent dataView = null;
	
	// Custom code for this type of action
	if (this.caller instanceof MultiBitFrame) {
	    JTable table = (JTable) e.getSource();
	    int row = Integer.parseInt(e.getActionCommand());
	    WalletAssetSummaryTableModel model = (WalletAssetSummaryTableModel) table.getModel();
	    data = model.getRow(row);
	    dataView = table;
	} else if (this.caller instanceof CSShowAssetsPanel) {
	    data = ((CSShowAssetsPanel)this.caller).getSelectedRowData();
	    dataView = this.view;
	}
	
	if (data != null) {
	    CSAsset asset = data.getAsset();
	    asset.setRefreshState();
	    
	    // Let's also ask the balances to be recalculated in case of updates/fixes to the tracking server.
	    this.bitcoinController.getModel().getActiveWallet().CS.setNeedsCalculateBalances(asset.getAssetID());
	    
	    //JOptionPane.showMessageDialog(dataView, "Asset details will be verified, will take between 15 to 30 seconds.");
	    
	    // We want main asset panel to refresh, since there isn't an event fired on manual reset.
	    CSEventBus.INSTANCE.postAsyncEvent(CSEventType.ASSET_UPDATED, asset.getAssetID());	}
    }
}
