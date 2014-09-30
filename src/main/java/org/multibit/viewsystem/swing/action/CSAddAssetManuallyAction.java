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
package org.multibit.viewsystem.swing.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.multibit.controller.Controller;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.model.bitcoin.WalletTableData;
import org.multibit.utils.ImageLoader;
import org.multibit.viewsystem.swing.MultiBitFrame;
import org.multibit.viewsystem.swing.view.dialogs.TransactionDetailsDialog;
import org.multibit.viewsystem.swing.view.panels.CSShowAssetsPanel;
import org.multibit.viewsystem.swing.view.panels.ShowTransactionsPanel;
import com.google.bitcoin.core.Wallet;
import org.coinspark.protocol.CoinSparkAssetRef;
import org.coinspark.wallet.CSAsset;
import org.coinspark.wallet.CSAssetDatabase;

/**
 * This {@link Action} shows the transaction details dialog
 */
public class CSAddAssetManuallyAction extends AbstractAction {

    private static final long serialVersionUID = 777792498732457765L;

    private final Controller controller;
    private final BitcoinController bitcoinController;
    
    private MultiBitFrame mainFrame;
    private CSShowAssetsPanel showAssetsPanel;

    /**
     * Creates a new {@link ShowTransactionDetailsAction}.
     */
    public CSAddAssetManuallyAction(BitcoinController bitcoinController, MultiBitFrame mainFrame, CSShowAssetsPanel showAssetsPanel) {
        super(bitcoinController.getLocaliser().getString("CSAddAssetManuallyAction.text"), ImageLoader.fatCow16(ImageLoader.FATCOW.add));
        this.bitcoinController = bitcoinController;
        this.controller = this.bitcoinController;
        this.showAssetsPanel = showAssetsPanel;
        
        this.mainFrame = mainFrame;

        MnemonicUtil mnemonicUtil = new MnemonicUtil(controller.getLocaliser());
        putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("CSAddAssetManuallyAction.tooltip"));
        //putValue(MNEMONIC_KEY, mnemonicUtil.getMnemonic("CSAddAssetManuallyAction.mnemonicKey"));
    }

    /**
     * show dialog to get asset ref.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
	//WalletTableData rowTableData = showTransactionsPanel.getSelectedRowData();

	String s = (String) JOptionPane.showInputDialog(
		mainFrame,
		"You can add an asset manually by entering it's asset reference\n"
		+ "which has the format \"digits-digits-digits\".",
		"Add Asset Manually",
		JOptionPane.PLAIN_MESSAGE,
		null,
		null,
		"");

	//If a string was returned, say so.
	if ((s != null) && (s.length() > 0)) {
	    s  = s.trim();
	    //System.out.println("asset ref detected! " + s);
	    CoinSparkAssetRef assetRef = new CoinSparkAssetRef();
	    if (assetRef.decode(s)) {
		Wallet wallet = this.bitcoinController.getModel().getActiveWallet();
		CSAssetDatabase assetDB = wallet.CS.getAssetDB();
		if (assetDB != null) {
		    CSAsset asset = new CSAsset(assetRef, CSAsset.CSAssetSource.MANUAL);
		    if (assetDB.insertAsset(asset) != null) {
			//System.out.println("Inserted new asset manually: " + asset);
		    }
		}
	    } else {
		JOptionPane.showMessageDialog(mainFrame, "The asset reference \"" + s + "\" does not appear to be valid.");
	    }
	}
		    
//        final TransactionDetailsDialog transactionDetailsDialog = new TransactionDetailsDialog(this.bitcoinController, mainFrame, rowTableData);
//        transactionDetailsDialog.setVisible(true);
        
	
        // Put the focus back on the table so that the up and down arrows work.
        showAssetsPanel.getTable().requestFocusInWindow();
    }
}