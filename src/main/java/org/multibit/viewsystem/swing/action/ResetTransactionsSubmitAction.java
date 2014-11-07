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

import com.google.bitcoin.core.Transaction;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.file.WalletSaveException;
import org.multibit.message.Message;
import org.multibit.message.MessageManager;
import org.multibit.model.bitcoin.WalletData;
import org.multibit.network.ReplayManager;
import org.multibit.network.ReplayTask;
import org.multibit.utils.DateUtils;
import org.multibit.viewsystem.swing.MultiBitFrame;
import org.multibit.viewsystem.swing.view.walletlist.SingleWalletPanel;
import org.multibit.viewsystem.swing.view.walletlist.WalletListPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.coinspark.wallet.CSAssetDatabase;
import org.coinspark.wallet.CSAsset;
import org.coinspark.wallet.CSBalanceDatabase;

/**
 * This {@link Action} resets the blockchain and transactions.
 */
public class ResetTransactionsSubmitAction extends MultiBitSubmitAction {

    private static final Logger log = LoggerFactory.getLogger(ResetTransactionsSubmitAction.class);

    private static final long serialVersionUID = 1923492460523457765L;

    private static final int NUMBER_OF_MILLISECOND_IN_A_SECOND = 1000;

    private MultiBitFrame mainFrame;

    /**
     * Creates a new {@link ResetTransactionsSubmitAction}.
     */
    public ResetTransactionsSubmitAction(BitcoinController bitcoinController, MultiBitFrame mainFrame, Icon icon) {
        super(bitcoinController, "resetTransactionsSubmitAction.text", "resetTransactionsSubmitAction.tooltip",
                "resetTransactionsSubmitAction.mnemonicKey", icon);
        this.mainFrame = mainFrame;
    }


    /**
     * Reset the transactions and replay the blockchain.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (abort()) {
            return;
        }


	int n = JOptionPane.showConfirmDialog(
			this.mainFrame,
			"Are you sure you want to reset blocks, transactions and assets?\n\n" +
			"If YES, the application will quit, and you must relaunch and wait\n" +
			"for blocks, transactions and assets to synchronize with the network.\n",
			"Confirm Reset",
			JOptionPane.YES_NO_OPTION);
	if (JOptionPane.NO_OPTION == n) {
	    return;
	}
	
	
        setEnabled(false);

        WalletData activePerWalletModelData = super.bitcoinController.getModel().getActivePerWalletModelData();

            // Work out the earliest transaction date and save it to the wallet.
            Date earliestTransactionDate = new Date(DateUtils.nowUtc().getMillis());
            Set<Transaction> allTransactions = activePerWalletModelData.getWallet().getTransactions(true);
            if (allTransactions != null) {
                for (Transaction transaction : allTransactions) {
                    if (transaction != null) {
                        Date updateTime = transaction.getUpdateTime();
                        if (updateTime != null && earliestTransactionDate.after(updateTime)) {
                            earliestTransactionDate = updateTime;
                        }
                    }
                }
            }
            Date actualResetDate = earliestTransactionDate;

            // Look at the earliest key creation time - this is
            // returned in seconds and is converted to milliseconds.
            long earliestKeyCreationTime = activePerWalletModelData.getWallet().getEarliestKeyCreationTime()
                    * NUMBER_OF_MILLISECOND_IN_A_SECOND;
            if (earliestKeyCreationTime != 0 && earliestKeyCreationTime < earliestTransactionDate.getTime()) {
                earliestTransactionDate = new Date(earliestKeyCreationTime);
                actualResetDate = earliestTransactionDate;
            }

        // Take an extra day off the reset date to ensure the wallet is cleared entirely
        actualResetDate = new Date (actualResetDate.getTime() - 3600 * 24 * NUMBER_OF_MILLISECOND_IN_A_SECOND);  // Number of milliseconds in a day

        // Remove the transactions from the wallet.
        activePerWalletModelData.getWallet().clearTransactions(actualResetDate);

	/* Remove all assets */

	// After resync, balances are not valid, so we remove actual files too.
	// FIXME: Asset table needs to recreate model or be emptied out when we switch to the pane.
	// recreateWalletData() is getting invoked, yet data stil remains.
	CSAssetDatabase db = activePerWalletModelData.getWallet().CS.getAssetDB();
	int[] assetIDs = db.getAssetIDs();
	if (assetIDs != null) {
	    for (int x : assetIDs) {
		CSAsset asset = db.getAsset(x);
		if (x != 0) {
		    boolean result = activePerWalletModelData.getWallet().CS.deleteAsset(asset);
		    log.debug(">>>> Deleted asset id: " + x + ", result = " + result);
		}
	    }
	}

	// Still need to quit and relaunch otherwise spendable amount is 0.
	// asset ref is xxx-0-xxx.
	
	String walletFilename = activePerWalletModelData.getWalletFilename();
	String csassets = walletFilename + ".csassets";
	String csbalances = walletFilename + ".csbalances";
	log.debug(">>>> csassets file = " + csassets);
	log.debug(">>>> csbalances file = " + csbalances);
	// TODO: This could be in assetDB or balancesDB class.
	// TODO: We need to grab a lock to prevent any attempted writes while we delete even though chance of a problem is low.
	File f = new File(csassets);             
        if(f.exists()) {
            if(!f.delete()) {
                log.error(">>>> Asset DB: Cannot delete");
            }            
        }
	f = new File(csbalances);
        if(f.exists()) {
            if(!f.delete()) {
                log.error(">>>> Balances DB: Cannot delete");
            }            
        }
	String cslog = walletFilename + ".cslog";
	log.debug(">>>> cslog file = " + cslog);
	f = new File(cslog);
        if(f.exists()) {
            if(!f.delete()) {
                log.error(">>>> CS Log File: Cannot delete");
            }            
        }
	

	
	
        // Save the wallet without the transactions.
        try {
            super.bitcoinController.getFileHandler().savePerWalletModelData(activePerWalletModelData, true);

            super.bitcoinController.getModel().createWalletTableData(super.bitcoinController, super.bitcoinController.getModel().getActiveWalletFilename());
            controller.fireRecreateAllViews(false);
        } catch (WalletSaveException wse) {
            log.error(wse.getClass().getCanonicalName() + " " + wse.getMessage());
            MessageManager.INSTANCE.addMessage(new Message(wse.getClass().getCanonicalName() + " " + wse.getMessage()));
        }
	
    
	// Reinitialise the asset and balances db.
	boolean b = activePerWalletModelData.getWallet().CS.initCSDatabases(walletFilename);
	log.debug(">>>> initCSDatabases returned: " + b);
	// TODO: delete the cached contracts folder, as the assetIDs will be different.
	// but restored assets will have different id, so wont overwrite.
	
        
        // Double check wallet is not busy then declare that the active wallet
        // is busy with the task
        WalletData perWalletModelData = this.bitcoinController.getModel().getActivePerWalletModelData();

        if (!perWalletModelData.isBusy()) {
            perWalletModelData.setBusy(true);
            perWalletModelData.setBusyTaskKey("resetTransactionsSubmitAction.text");
            perWalletModelData.setBusyTaskVerbKey("resetTransactionsSubmitAction.verb");

            super.bitcoinController.fireWalletBusyChange(true);

            resetTransactionsInBackground(actualResetDate, activePerWalletModelData.getWalletFilename());
        }

    }

    /**
     * Reset the transaction in a background Swing worker thread.
     */
    private void resetTransactionsInBackground(final Date resetDate, final String walletFilename) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

            private String message = "";

            @Override
            protected Boolean doInBackground() throws Exception {
                Boolean successMeasure;

                log.debug("Starting replay from date = " + resetDate);
                List<WalletData> perWalletModelDataList = new ArrayList<WalletData>();
                perWalletModelDataList.add(bitcoinController.getModel().getActivePerWalletModelData());

                // Initialise the message in the SingleWalletPanel.
                if (mainFrame != null) {
                    WalletListPanel walletListPanel = mainFrame.getWalletsView();
                    if (walletListPanel != null) {
                        SingleWalletPanel singleWalletPanel = walletListPanel.findWalletPanelByFilename(walletFilename);
                        if (singleWalletPanel != null) {
                            singleWalletPanel.setSyncMessage(controller.getLocaliser().getString("resetTransactionsSubmitAction.verb"), Message.NOT_RELEVANT_PERCENTAGE_COMPLETE);
                        }
                    }
                }

                ReplayTask replayTask = new ReplayTask(perWalletModelDataList, resetDate, ReplayTask.UNKNOWN_START_HEIGHT);
                ReplayManager.INSTANCE.offerReplayTask(replayTask);

                successMeasure = Boolean.TRUE;

                return successMeasure;
            }

            @Override
            protected void done() {
                try {
                    Boolean wasSuccessful = get();
                    if (wasSuccessful != null && wasSuccessful) {
                        log.debug(message);
			
			// After quitting, on launch, sync continues.
			// If we quit before the replaytask is setup, nothing happens on relauch.
			mainFrame.quitApplication();

                    } else {
                        log.error(message);
                    }
                    if (!message.equals("")) {
                        MessageManager.INSTANCE.addMessage(new Message(message));
                    }
                } catch (Exception e) {
                    // Not really used but caught so that SwingWorker shuts down cleanly.
                    log.error(e.getClass() + " " + e.getMessage());
                }
            }
        };
        log.debug("Resetting transactions in background SwingWorker thread");
        worker.execute();
    }
}