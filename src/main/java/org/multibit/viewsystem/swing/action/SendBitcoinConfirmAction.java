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
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.message.Message;
import org.multibit.message.MessageManager;
import org.multibit.model.bitcoin.BitcoinModel;
import org.multibit.viewsystem.dataproviders.BitcoinFormDataProvider;
import org.multibit.viewsystem.swing.MultiBitFrame;
import org.multibit.viewsystem.swing.view.dialogs.SendBitcoinConfirmDialog;
import org.multibit.viewsystem.swing.view.dialogs.ValidationErrorDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.util.ArrayList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.coinspark.protocol.CoinSparkAddress;
/* CoinSpark START */
import org.multibit.utils.CSMiscUtils;
import org.coinspark.protocol.*;
import org.multibit.model.core.CoreModel;
import org.multibit.viewsystem.dataproviders.AssetFormDataProvider;
/* CoinSpark END */

/**
 * This {@link Action} shows the send bitcoin confirm dialog or validation dialog on an attempted spend.
 */
public class SendBitcoinConfirmAction extends MultiBitSubmitAction {

    private static final long serialVersionUID = 1913592460523457765L;

    private static final Logger log = LoggerFactory.getLogger(SendBitcoinConfirmAction.class);

    private MultiBitFrame mainFrame;
    private BitcoinFormDataProvider dataProvider;
    private BitcoinController bitcoinController;

    /**
     * Creates a new {@link SendBitcoinConfirmAction}.
     */
    public SendBitcoinConfirmAction(BitcoinController bitcoinController, MultiBitFrame mainFrame, BitcoinFormDataProvider dataProvider) {
        super(bitcoinController, "sendBitcoinConfirmAction.text", "sendBitcoinConfirmAction.tooltip", "sendBitcoinConfirmAction.mnemonicKey", null);
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

        SendBitcoinConfirmDialog sendBitcoinConfirmDialog = null;
        ValidationErrorDialog validationErrorDialog = null;

        try {
            String sendAddress = dataProvider.getAddress();
            String sendAmount = dataProvider.getAmount();
	    String sendMessage = null;
	    boolean canSendMessage = false;
	    
	    /*CoinSpark START */
	    CoinSparkPaymentRef paymentRef = null;

	    /*
	     If the address is a coinspark address, retrieve the bitcoin address and let the validator check it.
	     The SendRequest object will use the bitcoin address, while the confirmation dialog will use
	     whatever is displayed in the address text field i.e. coinspark address, if there is one.
	     For reference, you can see what is in the textfield, which has been saved in prefs:
	     String address = this.bitcoinController.getModel().getActiveWalletPreference(BitcoinModel.SEND_ADDRESS
	     */
	    if (sendAddress.startsWith("s")) {
		CoinSparkAddress csa = CSMiscUtils.decodeCoinSparkAddress(sendAddress);
		String btcAddress = CSMiscUtils.getBitcoinAddressStringFromCoinSparkAddress(csa);
		if (btcAddress != null) {
		    sendAddress = btcAddress; // the validator will check the btcAddress like normal.
		}
		
		// Does a payment ref exist?
		int flags = csa.getAddressFlags();
		if ((flags & CoinSparkAddress.COINSPARK_ADDRESS_FLAG_PAYMENT_REFS) > 0) {
		    paymentRef = csa.getPaymentRef();
		    log.debug(">>>> CoinSpark address has payment refs flag set: " + paymentRef.toString());
		}
		
		// Messages - can send message and BTC to CoinSpark address, without any assets.
		sendMessage = ((AssetFormDataProvider)dataProvider).getMessage();
		canSendMessage = (flags & CoinSparkAddress.COINSPARK_ADDRESS_FLAG_TEXT_MESSAGES)>0;
	    }
	    /*CoinSpark END */
	    
            Validator validator = new Validator(super.bitcoinController);
            if (validator.validate(sendAddress, sendAmount)) {
                // The address and amount are valid.

                // Create a SendRequest.
                Address sendAddressObject;

                sendAddressObject = new Address(bitcoinController.getModel().getNetworkParameters(), sendAddress);
                SendRequest sendRequest = SendRequest.to(sendAddressObject, Utils.toNanoCoins(sendAmount));
//                SendRequest sendRequest = SendRequest.to(sendAddressObject, Utils.toNanoCoins(sendAmount), 6, new BigInteger("10000"),1);
                sendRequest.ensureMinRequiredFee = true;
                sendRequest.fee = BigInteger.ZERO;
                sendRequest.feePerKb = BitcoinModel.SEND_FEE_PER_KB_DEFAULT;

                // Note - Request is populated with the AES key in the SendBitcoinNowAction after the user has entered it on the SendBitcoinConfirm form.
		
		// Send with payment ref - if it exists
		if (paymentRef != null) {
		    sendRequest.setPaymentRef(paymentRef);
		}
		
		// Send a message if the address will take it and message is not empty
		if (canSendMessage) {
		    boolean isEmptyMessage = false;
		    if (sendMessage == null || sendMessage.isEmpty() || sendMessage.trim().length() == 0) {
			isEmptyMessage = true;
		    }	    
		    if (!isEmptyMessage) {
			int numParts = 1;
			CoinSparkMessagePart[] parts = new CoinSparkMessagePart[numParts];
			parts[0] = new CoinSparkMessagePart();
			parts[0].fileName = null;
			parts[0].mimeType = "text/plain";
			parts[0].content = sendMessage.getBytes("UTF-8");

			//String[] servers = new String[]{"assets1.coinspark.org/","assets1.coinspark.org/abc"};//,"144.76.175.228/" };					
			// Servers are URL encoded, and CSUtils looks for "://" to decide whether
			// or not to add prefix of "http://" but encoded this is %3A%2F%2F.
			String servers = controller.getModel().getUserPreference(CoreModel.MESSAGING_SERVERS);
			if (servers == null) {
			    servers = StringUtils.join(CoreModel.DEFAULT_MESSAGING_SERVER_URLS, "|");
			    controller.getModel().setUserPreference(CoreModel.MESSAGING_SERVERS, servers);
			}
			String[] urls = servers.split("\\|"); // regex so we have to escape | character
			ArrayList<String> list = new ArrayList<>();
			for (String url : urls) {
			    try {
				String decoded = URLDecoder.decode(url, "UTF-8");
				list.add(decoded);
			    } catch (UnsupportedEncodingException ue) {
				// don't add it
			    }
			}
			String[] serverURLs = list.toArray(new String[0]);

			
			sendRequest.setMessage(parts, serverURLs);

			log.debug(">>>> Messaging servers = " + servers);
			log.debug(">>>> parts[0] = " + parts[0]);
			log.debug(">>>> parts[0].fileName = " + parts[0].fileName);
			log.debug(">>>> parts[0].mimeType = " + parts[0].mimeType);
			log.debug(">>>> parts[0].content = " + ArrayUtils.toString(parts[0].content));
			//String message = "Hello, the time is now..." + DateTime.now().toString();
//		parts[2].fileName = imagePath;
//		parts[2].mimeType = "image/png";
//		byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
//		parts[2].content = imageBytes;

		    }
		}

		

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


    private void confirmAction(String sendAddress, String sendAmount) {
        SendBitcoinConfirmDialog sendBitcoinConfirmDialog = null;
        ValidationErrorDialog validationErrorDialog = null;

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


    private void logMessage(Exception e) {
        e.printStackTrace();
        String errorMessage = controller.getLocaliser().getString("sendBitcoinNowAction.bitcoinSendFailed");
        String detailMessage = controller.getLocaliser().getString("deleteWalletConfirmDialog.walletDeleteError2", new String[]{e.getClass().getCanonicalName() + " " + e.getMessage()});
        MessageManager.INSTANCE.addMessage(new Message(errorMessage + " " + detailMessage));
    }
}
