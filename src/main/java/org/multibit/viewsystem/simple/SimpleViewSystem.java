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
package org.multibit.viewsystem.simple;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.script.Script;
import org.multibit.model.bitcoin.WalletData;
import org.multibit.model.core.StatusEnum;
import org.multibit.viewsystem.DisplayHint;
import org.multibit.viewsystem.View;
import org.multibit.viewsystem.ViewSystem;

import java.math.BigInteger;
import java.util.List;

/**
 * A very simple implementation of a view system.
 */
public class SimpleViewSystem implements ViewSystem {
    int numberOfBlocksDownloaded = 0;

    public int getNumberOfBlocksDownloaded() {
        return numberOfBlocksDownloaded;
    }

    boolean online = false;

    public boolean isOnline() {
        return online;
    }

    @Override
    public void onCoinsReceived(Wallet wallet, Transaction transaction, BigInteger arg2, BigInteger arg3) {
        System.out.println("SIMPLE. Wallet " + wallet.hashCode() + " received transaction:\n" + transaction.toString());
    }

    @Override
    public void onCoinsSent(Wallet wallet, Transaction transaction, BigInteger arg2, BigInteger arg3) {
        System.out.println("SIMPLE. Wallet " + wallet.hashCode() + " send transaction:\n" + transaction.toString());
    }

    @Override
    public void onReorganize(Wallet wallet) {
        System.out.println("SIMPLE. Wallet " + wallet.hashCode() + " was reorganised");
    }

    @Override
    public void onTransactionConfidenceChanged(Wallet wallet, Transaction transaction) {
        //System.out.println("SIMPLE. Confidence changed for wallet: " + wallet.hashCode() + ", transaction:\n" + transaction.toString());
    }

    @Override
    public void displayView(View viewToDisplay) {
        System.out.println("SIMPLE. Displaying view : " + viewToDisplay);
    }

    @Override
    public void navigateAwayFromView(View viewToNavigateAwayFrom) {
    }

    @Override
    public void fireDataChangedUpdateNow(DisplayHint displayHint) {
        System.out.println("SIMPLE. Data has changed - update now.");
    }
    

    @Override
    public void fireDataChangedUpdateLater(DisplayHint displayHint) {
        //System.out.println("SIMPLE. Data has changed - update later.");
    }

    @Override
    public void fireFilesHaveBeenChangedByAnotherProcess(WalletData perWalletModelData) {
        System.out.println("SIMPLE. Files have been changed by another process");
    }

    @Override
    public void setOnlineStatus(StatusEnum statusEnum) {
        online = true;
        System.out.println("SIMPLE. online status = " + statusEnum.getLocalisationKey());
    }

    @Override
    public void blockDownloaded() {
        numberOfBlocksDownloaded++;
    }

    @Override
    public void setHelpContext(String helpContextToDisplay) {
        System.out.println("SIMPLE. Help : " + helpContextToDisplay);
    }

    @Override
    public void onWalletChanged(Wallet wallet) {
        System.out.println("SIMPLE. onWalletChanged");        
    }

    @Override
    public void recreateAllViews(boolean initUI, View initialView) {
        System.out.println("SIMPLE. All views were recreated");
    }

    @Override
    public void onKeysAdded(Wallet wallet, List<ECKey> keys) { 
    }

  @Override
  public void onScriptsAdded(Wallet wallet, List<Script> scripts) {
    System.out.println("SIMPLE. onScriptsAdded");
  }
}