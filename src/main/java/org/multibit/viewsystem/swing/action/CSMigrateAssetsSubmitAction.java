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

import com.google.bitcoin.core.*;
import com.google.bitcoin.crypto.KeyCrypterException;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.model.bitcoin.WalletBusyListener;
import org.multibit.utils.WhitespaceTrimmer;
import org.multibit.viewsystem.swing.MultiBitFrame;
import org.multibit.viewsystem.swing.view.panels.CSMigrateAssetsPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.CharBuffer;
import java.util.Iterator;
import org.coinspark.protocol.CoinSparkAddress;
import org.multibit.model.bitcoin.WalletData;
import org.multibit.utils.CSMiscUtils;
import com.google.bitcoin.core.Wallet.SendRequest;
import java.math.BigInteger;
import java.util.Map;
import java.util.ArrayList;
import org.coinspark.core.CSExceptions;

/**
 * This {@link Action} signs a message
 */
public class CSMigrateAssetsSubmitAction extends MultiBitSubmitAction implements WalletBusyListener {

    private static final Logger log = LoggerFactory.getLogger(ImportPrivateKeysSubmitAction.class);

    private static final long serialVersionUID = 555557598757765L;

    private MultiBitFrame mainFrame;
    private CSMigrateAssetsPanel migrateAssetsPanel;
    
    /**
     * Creates a new {@link SignMessageSubmitAction}.
     */
    public CSMigrateAssetsSubmitAction(BitcoinController bitcoinController, MultiBitFrame mainFrame,
            CSMigrateAssetsPanel signMessagePanel, ImageIcon icon) {
        super(bitcoinController, "migrateAssetsAction.text", "migrateAssetsAction.tooltip", "migrateAssetsAction.mnemonicKey", icon);
        this.mainFrame = mainFrame;
        this.migrateAssetsPanel = signMessagePanel;

        // This action is a WalletBusyListener.
        super.bitcoinController.registerWalletBusyListener(this);
        walletBusyChange(super.bitcoinController.getModel().getActivePerWalletModelData().isBusy());
    }

    /**
     * Verify the message.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (abort()) {
            return;
        }
        
        if (migrateAssetsPanel == null) {
            return;
        }
	
	/* Validation of fields occurs here */

        CharSequence walletPassword = null;
        if (migrateAssetsPanel.getWalletPasswordField() != null) {
            walletPassword = CharBuffer.wrap(migrateAssetsPanel.getWalletPasswordField().getPassword());

            if (bitcoinController.getModel().getActiveWallet().isEncrypted()) {
                if (walletPassword.length() == 0) {
                    migrateAssetsPanel.setMessageText1(controller.getLocaliser().getString(
                            "showExportPrivateKeysAction.youMustEnterTheWalletPassword"));
                    migrateAssetsPanel.setMessageText2(" ");
                    return;
                }

                if (!bitcoinController.getModel().getActiveWallet().checkPassword(walletPassword)) {
                    // The password supplied is incorrect.
                    migrateAssetsPanel.setMessageText1(controller.getLocaliser().getString(
                            "createNewReceivingAddressSubmitAction.passwordIsIncorrect"));
                    migrateAssetsPanel.setMessageText2(" ");
                    return;
                }
            }
        }      
	
	String addressText = null;
        if (migrateAssetsPanel.getAddressTextArea() != null) {
            addressText = migrateAssetsPanel.getAddressTextArea().getText();
            if (addressText != null) {
              addressText = WhitespaceTrimmer.trim(addressText);
            }
        }

	// Should never be null or empty as we check before allowing this action to be invoked.
	if (addressText == null || "".equals(addressText)) {
            migrateAssetsPanel.setMessageText1(controller.getLocaliser().getString("signMessageAction.noAddress"));
            migrateAssetsPanel.setMessageText2(" ");  
            return;
        }
		
	String btcAddress = "";
	CoinSparkAddress csa = CSMiscUtils.decodeCoinSparkAddress(addressText);
	if (csa != null) {
	    // Can the coinspark address receive assets?
	    int flags = csa.getAddressFlags();
	    if ((flags & CoinSparkAddress.COINSPARK_ADDRESS_FLAG_ASSETS) > 0) {
		btcAddress = csa.getBitcoinAddress();
		boolean validBtcAddress = false;
		try {
		    new Address(this.bitcoinController.getModel().getNetworkParameters(), btcAddress);
		    validBtcAddress = true;
		} catch (AddressFormatException afe) {
		    // Carry on.
		} catch (java.lang.StringIndexOutOfBoundsException e) {
		    // Carry on.
		}
		if (!validBtcAddress) {
		    migrateAssetsPanel.setMessageText1(controller.getLocaliser().getString(
			    "migrateAssetsSubmitAction.addressWrongNetwork"));
		    migrateAssetsPanel.setMessageText2(" ");
		    return;
		}
	    } else {
		migrateAssetsPanel.setMessageText1(controller.getLocaliser().getString(
			"migrateAssetsSubmitAction.addressNoAssets"));
		migrateAssetsPanel.setMessageText2(" ");
		return;
	    }
	} else {
	    migrateAssetsPanel.setMessageText1(controller.getLocaliser().getString(
		    "migrateAssetsSubmitAction.addressInvalid"));
	    migrateAssetsPanel.setMessageText2(" ");
	    return;
	}
	
	
//        String messageText = null;
//        if (migrateAssetsPanel.getMessageTextArea() != null) {
//            messageText = migrateAssetsPanel.getMessageTextArea().getText();
//        }
//        
//        log.debug("addressText = '" + addressText + "'");
//        log.debug("messageText = '" + messageText + "'");
        
	WalletData perWalletModelData = this.bitcoinController.getModel().getActivePerWalletModelData();
	log.debug("walletInfo = " + perWalletModelData.getWalletInfo());
	log.debug("address = " + addressText);
	boolean isAddressInSameWallet = perWalletModelData.getWalletInfo().containsReceivingAddress(btcAddress);
	log.debug("result = " + isAddressInSameWallet);
	if (isAddressInSameWallet) {
	    migrateAssetsPanel.setMessageText1(controller.getLocaliser().getString(
		    "migrateAssetsSubmitAction.addressSameWallet"));
	    migrateAssetsPanel.setMessageText2(" ");
	    return;	    
	}

	
	// PLACEHOLDER FOR MIGRATION CODE
	migrateAssetsPanel.setMessageText1("Migration feature has not yet been implemented...");
	migrateAssetsPanel.setMessageText2(" ");
	
//	migrateTo(btcAddress);
    }
    
    /*
    TODO:
    FIXME:
    If there are assets which are NOT valid, should migration be allowed?
    Will implicit transfer include 0 balance assets, which may not be valid?
    */
    private void migrateTo(String address) {
	Address sendAddressObject;
        try {
	    Wallet wallet = bitcoinController.getModel().getActiveWallet();
            sendAddressObject = new Address(this.bitcoinController.getModel().getNetworkParameters(), address);
            SendRequest sendRequest = SendRequest.emptyWallet(sendAddressObject);
            sendRequest.ensureMinRequiredFee = true;
            sendRequest.fee = BigInteger.ZERO;
            wallet.completeTx(sendRequest, false);

	    String s="";
            s+="Wallet can be successfully migrated with fee: " + sendRequest.fee + "\n";
            s+="The following assets were transferred:\n\n";
            
            Map<Integer,BigInteger> mapTransferred = wallet.CS.getAssetTransferValues(sendRequest,true);
            
            for (Map.Entry<Integer,BigInteger> entry : mapTransferred.entrySet()) 
            {
                s+="Asset " + entry.getKey() + ": " + entry.getValue() + "\n";
            }
	    
	    ArrayList<Integer> manualTransfers = new ArrayList<Integer>();
	    int[] array_ids = wallet.CS.getAssetIDs();
 	    if (array_ids != null) {
		for (int i : array_ids) {
		    if (!mapTransferred.containsKey(i)) {
			manualTransfers.add(i);
			
			
			String assetRef = wallet.CS.getAsset(i).getAssetReference().toString();
			s+="Manul asset: " + assetRef + "\n";
			
			
		    }
		}
	    }
	    
	    s += "The following assets require a manual transfer: " + manualTransfers.toString() + "\n";
	    
	    System.out.println(">>>>" + s);
//            CSTransactionAssets txAssets=new CSTransactionAssets(sendRequest.tx);
//            
//            s+="\n";
//            s+="Transfer list: \n\n";
//            
//            if(txAssets.getTransfers() != null)
//            {
//                s+=txAssets.getTransfers().toString() + "\n";
//            }
//            else
//            {
//                s+="Empty!!!\n";
//            }
            
        } catch (AddressFormatException ex) {
            ex.printStackTrace();
        } catch (InsufficientMoneyException ex) {
            ex.printStackTrace();
        } catch (CSExceptions.CannotEncode ex) {
            ex.printStackTrace();	    
	}

    }
    
    private void logError(Exception e) {
        e.printStackTrace();
        migrateAssetsPanel.setMessageText1(controller.getLocaliser().getString("signMessageAction.error"));
        migrateAssetsPanel.setMessageText2(controller.getLocaliser().getString("deleteWalletConfirmDialog.walletDeleteError2", 
                new String[] {e.getClass().getCanonicalName() + " " + e.getMessage()}));
    }

    @Override
    public void walletBusyChange(boolean newWalletIsBusy) {
        // Update the enable status of the action to match the wallet busy
        // status.
        if (super.bitcoinController.getModel().getActivePerWalletModelData().isBusy()) {
            // Wallet is busy with another operation that may change the private
            // keys - Action is disabled.
            putValue(
                    SHORT_DESCRIPTION,
                    controller.getLocaliser().getString(
                            "multiBitSubmitAction.walletIsBusy",
                            new Object[] { controller.getLocaliser().getString(
                                    this.bitcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey()) }));
            setEnabled(false);
        } else {
            // Enable unless wallet has been modified by another process.
            if (!super.bitcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
                putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("signMessageAction.tooltip"));
                setEnabled(true);
            }
        }
    }
}