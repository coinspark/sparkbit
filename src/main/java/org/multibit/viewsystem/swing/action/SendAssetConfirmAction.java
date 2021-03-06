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
import com.google.bitcoin.core.Wallet.SendRequest;
import com.google.bitcoin.crypto.KeyCrypterException;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.awt.BorderLayout;
import java.awt.Dialog;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.message.Message;
import org.multibit.message.MessageManager;
import org.multibit.model.bitcoin.BitcoinModel;
import org.multibit.utils.ImageLoader;
//import org.multibit.viewsystem.dataproviders.BitcoinFormDataProvider;
import org.multibit.viewsystem.swing.MultiBitFrame;
//import org.multibit.viewsystem.swing.view.dialogs.SendBitcoinConfirmDialog;
import org.multibit.viewsystem.swing.view.dialogs.AssetValidationErrorDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.ArrayUtils;
import org.multibit.utils.CSMiscUtils;
import org.coinspark.protocol.CoinSparkAddress;
import org.coinspark.protocol.CoinSparkGenesis;
import org.coinspark.protocol.CoinSparkMessagePart;
import org.coinspark.protocol.CoinSparkPaymentRef;

import org.multibit.viewsystem.dataproviders.AssetFormDataProvider;
import org.multibit.viewsystem.swing.view.dialogs.SendAssetConfirmDialog;
import org.coinspark.wallet.CSAsset;
import org.multibit.viewsystem.swing.view.dialogs.SendBitcoinConfirmDialog;
import org.multibit.viewsystem.swing.view.dialogs.ValidationErrorDialog;

/**
 * This {@link Action} shows the send bitcoin confirm dialog or validation dialog on an attempted spend.
 */
public class SendAssetConfirmAction extends MultiBitSubmitAction {

    private static final long serialVersionUID = 5513592460523457765L;

    private static final Logger log = LoggerFactory.getLogger(SendAssetConfirmAction.class);

    private MultiBitFrame mainFrame;
    private AssetFormDataProvider dataProvider;
    private BitcoinController bitcoinController;

    /**
     * Creates a new {@link SendBitcoinConfirmAction}.
     */
    public SendAssetConfirmAction(BitcoinController bitcoinController, MultiBitFrame mainFrame, AssetFormDataProvider dataProvider) {
        super(bitcoinController, "sendBitcoinConfirmAction.text", "sendAssetConfirmAction.tooltip", "sendBitcoinConfirmAction.mnemonicKey", null);
	//ImageLoader.createImageIcon(ImageLoader.SEND_BITCOIN_ICON_FILE));
        this.mainFrame = mainFrame;
        this.dataProvider = dataProvider;
        this.bitcoinController = bitcoinController;
    }

    /**
     * Complete the transaction to work out the fee) and then show the send bitcoin confirm dialog.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (abort()) {
            return;
        }

//        SendAssetConfirmDialog sendAssetConfirmDialog = null;
        AssetValidationErrorDialog validationErrorDialog = null;

        try {	    
            String sendAddress = dataProvider.getAddress();
            String sendAmount = Utils.bitcoinValueToPlainString(BitcoinModel.COINSPARK_SEND_MINIMUM_AMOUNT);
	    String sendMessage = null;
	    boolean canSendMessage = false;
	    
	    final int assetId = dataProvider.getAssetId();
	    String assetAmount = dataProvider.getAssetAmount();
	    boolean isSenderPays = dataProvider.isSenderPays();

	    /* Is there a payment charge?  If yes, the asset amount will change */
	    CSAsset asset = bitcoinController.getModel().getActiveWallet().CS.getAsset(assetId);
	    CoinSparkGenesis genesis = asset.getGenesis();
	    BigInteger assetAmountRawUnits = CSMiscUtils.getRawUnitsFromDisplayString(asset, assetAmount);
	    // Was there any rounding, which the user did not see because they clicked on send first,
	    // without losing focus to any other widget which corrects field?
	    String typedAmount = dataProvider.getAssetAmountText();
	    BigDecimal bd1 = new BigDecimal(typedAmount);
	    BigDecimal bd2 = new BigDecimal(assetAmount);
	    bd1 = bd1.stripTrailingZeros();
	    bd2 = bd2.stripTrailingZeros();
	    if (bd1.equals(bd2) == false) {
		String displayUnit = CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, BigInteger.ONE);
		String s = "The smallest transactable unit is "+displayUnit+", so we have rounded the sum down.\nPlease confirm the final amount and click 'Send' again.";
		JOptionPane.showMessageDialog(mainFrame, s);
		return;
	    }
	    // End rounding check/warning
  
	    long desiredRawUnits = assetAmountRawUnits.longValue();
	    short chargeBasisPoints = genesis.getChargeBasisPoints();
	    long rawFlatChargeAmount = genesis.getChargeFlat();
	    boolean chargeExists = ( rawFlatChargeAmount>0 || chargeBasisPoints>0 );
	    if (chargeExists) {
		if (isSenderPays) {
		    long x = genesis.calcGross(desiredRawUnits);
		    assetAmountRawUnits = new BigInteger(String.valueOf(x));
		} else {
		    // We don't have to do anything if recipient pays, just send gross amount.
		    // calcNet() returns what the recipient will receive, but it's not what we send.
//		    long x = genesis.calcNet(desiredRawUnits);
//		    assetAmountRawUnits = new BigInteger(String.valueOf(x));		    
		}
	    }
	    
	    // Todo: Allow invalid assets to be sent even if spendable balance is 0
	    //if (CSMiscUtils.canSendInvalidAsset(bitcoinController) 
            final AssetValidator validator = new AssetValidator(super.bitcoinController);
            if (validator.validate(sendAddress, sendAmount, assetId, assetAmountRawUnits.toString() )) {
		/* CoinSpark START */
		CoinSparkPaymentRef paymentRef = null;

		// We have already validated that the coinspark address and underlying bitcoin are good
		// and that the transfer flag is set on the coinspark address.  We just want the actual
		// underlying bitcoin address to send assets to, which is used to create SendRequest object.
		CoinSparkAddress csa = CSMiscUtils.decodeCoinSparkAddress(sendAddress);
		String btcAddress = CSMiscUtils.getBitcoinAddressStringFromCoinSparkAddress(csa);
		sendAddress = btcAddress;
		
				
		// Does a payment ref exist?
		int flags = csa.getAddressFlags();
		if ((flags & CoinSparkAddress.COINSPARK_ADDRESS_FLAG_PAYMENT_REFS) > 0) {
		    paymentRef = csa.getPaymentRef();
		    log.debug(">>>> CoinSpark address has payment refs flag set: " + paymentRef.toString());
		}
		
		// Messages - can send message and BTC to CoinSpark address, without any assets.
		sendMessage = dataProvider.getMessage();
		canSendMessage = (flags & CoinSparkAddress.COINSPARK_ADDRESS_FLAG_TEXT_MESSAGES)>0;
		/* CoinSpark END */
		
                // Create a SendRequest.
                Address sendAddressObject;

                sendAddressObject = new Address(bitcoinController.getModel().getNetworkParameters(), sendAddress);
                //SendRequest sendRequest = SendRequest.to(sendAddressObject, Utils.toNanoCoins(sendAmount));

		//public static SendRequest to(Address destination,BigInteger value,int assetID, BigInteger assetValue,int split) {
		//BigInteger assetAmountRawUnits = new BigInteger(assetAmount);
		BigInteger bitcoinAmountSatoshis = Utils.toNanoCoins(sendAmount);
		
		final SendRequest sendRequest = SendRequest.to(sendAddressObject, bitcoinAmountSatoshis, assetId, assetAmountRawUnits, 1);
                sendRequest.ensureMinRequiredFee = true;
                sendRequest.fee = BigInteger.ZERO;
                sendRequest.feePerKb = BitcoinModel.SEND_FEE_PER_KB_DEFAULT;

                // Note - Request is populated with the AES key in the SendBitcoinNowAction after the user has entered it on the SendBitcoinConfirm form.
		
		// Send with payment ref - if it exists and is not 0 which SparkBit treats semantically as null
		if (paymentRef != null && paymentRef.getRef()!=0) {
		    sendRequest.setPaymentRef(paymentRef);
		}
		
		// Send a message if the address will take it and message is not empty
		boolean willSendMessage = false;
		if (canSendMessage) {
		    boolean isEmptyMessage = false;
		    if (sendMessage == null || sendMessage.isEmpty() || sendMessage.trim().length() == 0) {
			isEmptyMessage = true;
		    }	    
		    if (!isEmptyMessage) {
			willSendMessage = true;
			//int numParts = 1;
			CoinSparkMessagePart[] parts = { CSMiscUtils.createPlainTextCoinSparkMessagePart(sendMessage) };
			String[] serverURLs = CSMiscUtils.getMessageDeliveryServersArray(bitcoinController);
			sendRequest.setMessage(parts, serverURLs);

			log.debug(">>>> Messaging servers = " + ArrayUtils.toString(serverURLs));
			log.debug(">>>> parts[0] = " + parts[0]);
			log.debug(">>>> parts[0].fileName = " + parts[0].fileName);
			log.debug(">>>> parts[0].mimeType = " + parts[0].mimeType);
			log.debug(">>>> parts[0].content = " + new String(parts[0].content, "UTF-8"));
			//String message = "Hello, the time is now..." + DateTime.now().toString();
//		parts[2].fileName = imagePath;
//		parts[2].mimeType = "image/png";
//		byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
//		parts[2].content = imageBytes;

		    }
		}

		//
		// When sending a message, show a modal dialog.
		// CompleteTX now occurs in background thread so UI does not block
		// when "Send" is clicked with widget updates frozen.
		//

		// Show dialog with indeterminate progress bar
		final JDialog dialog = CSMiscUtils.createModalMessageDialogWithIndeterminateProgress(mainFrame, "SparkBit", "Contacting message delivery servers...");
		// Dialog is made visible after futures have been set up
		
		ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()); //newFixedThreadPool(10));
		ListenableFuture<Boolean> future = service.submit(new Callable<Boolean>() {
		    public Boolean call() throws Exception {
			try {
			    // Complete it (which works out the fee) but do not sign it yet.
			    log.debug("Just about to complete the tx (and calculate the fee)...");
			    bitcoinController.getModel().getActiveWallet().completeTx(sendRequest, false);
			    log.debug("The fee after completing the transaction was " + sendRequest.fee);

			} catch (Exception e) {
			    throw e;
			}
			return true;
		    }
		});

		final BigInteger myAssetAmountRawUnits = assetAmountRawUnits;
		
		Futures.addCallback(future, new FutureCallback<Boolean>() {
		    public void onSuccess(Boolean b) {

			// There is enough money.
			SwingUtilities.invokeLater(new Runnable() {
			    @Override
			    public void run() {
				dialog.dispose();

				SendAssetConfirmDialog mySendAssetConfirmDialog = new SendAssetConfirmDialog(bitcoinController, mainFrame, sendRequest, assetId, myAssetAmountRawUnits, validator);
				mySendAssetConfirmDialog.setVisible(true);
			    }
			});

		    }

		    public void onFailure(Throwable thrown) {
			final String failureReason = thrown.getMessage();
			final boolean isCSException = thrown instanceof org.coinspark.core.CSExceptions.CannotEncode;			     final boolean isInsufficientMoney = thrown instanceof com.google.bitcoin.core.InsufficientMoneyException;			
			// There is not enough money.
			// TODO setup validation parameters accordingly so that it displays ok.
			SwingUtilities.invokeLater(new Runnable() {
			    @Override
			    public void run() {
				dialog.dispose();

				if (isCSException) {
				    JOptionPane.showMessageDialog(mainFrame, "SparkBit is unable to proceed with this transaction:\n\n"+failureReason, "SparkBit Error", JOptionPane.ERROR_MESSAGE);
				} else if (isInsufficientMoney) {
				    // Try to show a human friendly message, need to pass the missing satoshis from failure reason.
				    try {
					String numberOnly = failureReason.replaceAll("[^0-9]", "");
					BigInteger needed = new BigInteger(numberOnly);
					JOptionPane.showMessageDialog(mainFrame, "SparkBit is unable to proceed with this transaction.\n\nInsufficient money in wallet, require " + Utils.bitcoinValueToFriendlyString(needed) + " BTC more.", "SparkBit Error", JOptionPane.ERROR_MESSAGE);
				    } catch (NumberFormatException e) {
					AssetValidationErrorDialog myValidationErrorDialog = new AssetValidationErrorDialog(bitcoinController, mainFrame, sendRequest, true, validator);
					myValidationErrorDialog.setVisible(true);
				    }
				} else {

				    AssetValidationErrorDialog myValidationErrorDialog = new AssetValidationErrorDialog(bitcoinController, mainFrame, sendRequest, true, validator);
				    myValidationErrorDialog.setVisible(true);
				}
			    }
			});
		    }
		});

		
		// Show message server dialog only if we are going to send
		if (willSendMessage) {
		    dialog.setVisible(true);
		}
		
		
/*		
                // Complete it (which works out the fee) but do not sign it yet.
                log.debug("Just about to complete the tx (and calculate the fee)...");
                boolean completedOk;
                try {
                    bitcoinController.getModel().getActiveWallet().completeTx(sendRequest, false);
                  completedOk = true;
                  log.debug("The fee after completing the transaction was " + sendRequest.fee);
                } catch (InsufficientMoneyException ime) {
                  completedOk = false;
                }
                if (completedOk) {
                    // There is enough money.

                    sendAssetConfirmDialog = new SendAssetConfirmDialog(super.bitcoinController, mainFrame, sendRequest, assetId, assetAmountRawUnits, validator);
                    sendAssetConfirmDialog.setVisible(true);
                } else {
                    // There is not enough money.
                    // TODO setup validation parameters accordingly so that it displays ok.
                    validationErrorDialog = new AssetValidationErrorDialog(super.bitcoinController, mainFrame, sendRequest, true, validator);
                    validationErrorDialog.setVisible(true);
                }
*/
		
            } else {
                validationErrorDialog = new AssetValidationErrorDialog(super.bitcoinController, mainFrame, null, false, validator);
                validationErrorDialog.setVisible(true);
            }
        } catch (WrongNetworkException e1) {
            logMessage(e1);
        } catch (AddressFormatException e1) {
            logMessage(e1);
        } catch (KeyCrypterException e1) {
            logMessage(e1);
	} catch (NumberFormatException nfe) {
	    JOptionPane.showMessageDialog(mainFrame, "Please enter a valid amount.");
        } catch (Exception e1) {
            logMessage(e1);
        }
    }

/*
    private void confirmAction(String sendAddress, String sendAmount) {
        SendBitcoinConfirmDialog sendBitcoinConfirmDialog = null;
        AssetValidationErrorDialog validationErrorDialog = null;

        try {
            Validator validator = new Validator(super.bitcoinController);

            boolean result = validator.validate(sendAddress, sendAmount) ;
            if (result)
            {
                // The address and amount are valid.
                SendRequest sendRequest = createSendRequest(sendAddress, sendAmount);

                // Complete it (which works out the fee) but do not sign it yet.
                log.debug("Just about to complete the tx (and calculate the fee)...");
                boolean completedOk;
                try {
                    bitcoinController.getModel().getActiveWallet().completeTx(sendRequest, false);
                  completedOk = true;
                  log.debug("The fee after completing the transaction was " + sendRequest.fee);
                } catch (InsufficientMoneyException ime) {
                  completedOk = false;
                }
                if (completedOk) {
                    // There is enough money.

                    sendBitcoinConfirmDialog = new SendBitcoinConfirmDialog(super.bitcoinController, mainFrame, sendRequest);
                    sendBitcoinConfirmDialog.setVisible(true);
                } else {
                    // There is not enough money.
                    // TODO setup validation parameters accordingly so that it displays ok.
                    validationErrorDialog = new ValidationErrorDialog(super.bitcoinController, mainFrame, sendRequest, true);
                    validationErrorDialog.setVisible(true);
                }

            } else {
                validationErrorDialog = new ValidationErrorDialog(super.bitcoinController, mainFrame, null, false);
                validationErrorDialog.setVisible(true);
            }
        } catch (WrongNetworkException e1) {
            logMessage(e1);
        } catch (AddressFormatException e1) {
            logMessage(e1);
        } catch (KeyCrypterException e1) {
            logMessage(e1);
        } catch (Exception e1) {
            logMessage(e1);
        }
    }
*/
    private SendRequest createSendRequest(String sendAddress, String sendAmount) throws AddressFormatException {
        // Create a SendRequest.
        Address sendAddressObject;

        sendAddressObject = new Address(bitcoinController.getModel().getNetworkParameters(), sendAddress);
        SendRequest sendRequest = SendRequest.to(sendAddressObject, Utils.toNanoCoins(sendAmount));
        sendRequest.ensureMinRequiredFee = true;
        sendRequest.fee = BigInteger.ZERO;
        sendRequest.feePerKb = BitcoinModel.SEND_FEE_PER_KB_DEFAULT;

        // Note - Request is populated with the AES key in the SendBitcoinNowAction after the user has entered it on the SendBitcoinConfirm form.
        return sendRequest;
    }

    /*CoinSpark*/
    private void confirmAssetAction(String sendAddress, String sendAmount, String sendType) {
//        ValidationErrorDialog validationErrorDialog = null;
//
//        try {
//            Validator validator = new Validator(super.bitcoinController);
//
//            boolean result = validator.validate(sendAddress, sendAmount, sendType);
//            if (result)
//            {
//                // The address and amount are valid.
//
//                // Create a SendRequest.
//                String adjustedAmount = Transaction.MIN_NONDUST_OUTPUT.toString();
//                SendRequest sendRequest = createSendRequest(sendAddress, adjustedAmount);
//
//                // Complete it (which works out the fee) but do not sign it yet.
//                log.debug("CoinSpark - Just about to complete the asset tx (and calculate the fee)...");
//                boolean completedOk = bitcoinController.getModel().getActiveWallet().completeAssetTx(sendRequest, sendType, sendAmount);
//                log.debug("The fee after completing the transaction was " + sendRequest.fee);
//                if (completedOk) {
//                	/* CoinSpark START: CSSendAssetConfirmDialog does not exist */
//                	log.debug("CoinSpark - There is enough money and asset. completedOk is true.");
//                    // There is enough money and asset.
//                    //sendAssetConfirmDialog = new CSSendAssetConfirmDialog(super.bitcoinController, mainFrame, sendRequest, sendType);
//                    //sendAssetConfirmDialog.setVisible(true);
//                    /* CoinSpark END */
//                } else {
//                    // There is not enough money.
//                    // TODO setup validation parameters accordingly so that it displays ok.
//                    validationErrorDialog = new ValidationErrorDialog(super.bitcoinController, mainFrame, sendRequest, true);
//                    validationErrorDialog.setVisible(true);
//                }
//
//            } else {
//                validationErrorDialog = new ValidationErrorDialog(super.bitcoinController, mainFrame, null, false);
//                validationErrorDialog.setVisible(true);
//            }
//        } catch (WrongNetworkException e1) {
//            logMessage(e1);
//        } catch (AddressFormatException e1) {
//            logMessage(e1);
//        } catch (KeyCrypterException e1) {
//            logMessage(e1);
//        } catch (Exception e1) {
//            logMessage(e1);
//        }
    }
	/* CoinSpark END */

    private void logMessage(Exception e) {
        e.printStackTrace();
        String errorMessage = controller.getLocaliser().getString("sendBitcoinNowAction.bitcoinSendFailed");
        String detailMessage = controller.getLocaliser().getString("deleteWalletConfirmDialog.walletDeleteError2", new String[]{e.getClass().getCanonicalName() + " " + e.getMessage()});
        MessageManager.INSTANCE.addMessage(new Message(errorMessage + " " + detailMessage));
    }
}
