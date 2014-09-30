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

import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import javax.swing.SwingUtilities;

import org.multibit.controller.Controller;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.viewsystem.DisplayHint;
import org.multibit.viewsystem.View;
import org.multibit.model.bitcoin.WalletData;

import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.PeerGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import org.coinspark.wallet.CSAssetDatabase;
import org.coinspark.wallet.CSBalanceDatabase;


public class UpdateAssetsTimerTask extends TimerTask {
    public static final int INTERVAL = 15000; // milliseconds

    private static Logger log = LoggerFactory.getLogger(UpdateAssetsTimerTask.class);

    private Controller controller;
    private BitcoinController bitcoinController;

    private MultiBitFrame mainFrame;

//    private boolean updateTransactions = false;
//    private boolean isCurrentlyUpdating = false;

    private ExecutorService pool; // single executor thread, tasks processed in sequence

    public UpdateAssetsTimerTask(BitcoinController bitcoinController, MultiBitFrame mainFrame) {
        this.controller = bitcoinController;
        this.bitcoinController = bitcoinController;
        this.mainFrame = mainFrame;
        this.pool = Executors.newSingleThreadExecutor();
    }
    

    @Override
    public boolean cancel() {
        this.pool.shutdown();
        this.pool.shutdownNow();
        return super.cancel();
    }
    
    // TODO: Better for task to iterate over wallets and invoke update methods
    //       or add a separate task for each wallet to the executor queue?
    @Override
    public void run() {
//        log.debug("UpdateAssetsTimerTask run() has fired...");
        // if (this.bitcoinController.getModel().getActiveWallet() != null) {

        pool.execute(new Runnable() {
            @Override
            public void run() {
                log.debug("UpdateAssetsTimerTask fired and executing");
                PeerGroup peerGroup = null;
                if (bitcoinController.getMultiBitService() != null && bitcoinController.getMultiBitService().getPeerGroup() != null) {
                    peerGroup = bitcoinController.getMultiBitService().getPeerGroup();
                }

                List<WalletData> perWalletModelDataList = bitcoinController.getModel().getPerWalletModelDataList();
                if (perWalletModelDataList != null) {
                    Iterator<WalletData> iterator = perWalletModelDataList.iterator();
                    while (iterator.hasNext()) {
			try {
			    WalletData wd = iterator.next();
			    if (wd != null) {
//                            log.debug("LOOP: Attempting to update assets in wallet: " + wd.getWalletDescription() + "...");
				Wallet w = wd.getWallet();
				if (w == null) {
				    log.debug("getWallet() returned null");

				} else {
				    if (peerGroup == null) {
					log.debug("Cannot invoke validateAssets(peerGroup) as peerGroup is null");
				    } else {
					w.CS.validateAllAssets(peerGroup);
//					CSAssetDatabase assetDB = w.CS.getAssetDB();
//					if (assetDB == null) {
//					    log.debug("getAssetDB() returned null");
//					} else {
//					    assetDB.validateAssets(peerGroup);
////                                        log.debug("Invoked validateAssets(peerGroup)");
//					}
				    }

				    w.CS.calculateBalances();
//				    CSBalanceDatabase balanceDB = w.CS.getBalanceDB();
//				    if (balanceDB == null) {
////                                    log.debug("getBalanceDB() returned null");
//				    } else {
//					balanceDB.calculateBalances();
////                                    log.debug("Invoked calculateBalances()");
//				    }
				}
			    }
			} catch (java.util.ConcurrentModificationException cme) {
//			    log.debug("Caught ConcurrentModificationException: " + cme.getMessage());
			}
		    }
                }
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });

//        log.debug("UpdateAssetsTimerTask run() is ending.");
    }

//    public boolean isUpdateTransactions() {
//        // Clone before return.
//        return updateTransactions ? true : false;
//    }
//
//    public void setUpdateTransactions(boolean updateTransactions) {
//        this.updateTransactions = updateTransactions;
//    }
}
