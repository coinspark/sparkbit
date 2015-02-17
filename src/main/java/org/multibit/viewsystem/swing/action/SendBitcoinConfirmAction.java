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
import java.awt.Dimension;
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
import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.ArrayUtils;
import org.coinspark.protocol.CoinSparkAddress;
/* CoinSpark START */
import org.multibit.utils.CSMiscUtils;
import org.coinspark.protocol.*;
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

//        SendBitcoinConfirmDialog sendBitcoinConfirmDialog = null;
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
                final SendRequest sendRequest = SendRequest.to(sendAddressObject, Utils.toNanoCoins(sendAmount));
//                SendRequest sendRequest = SendRequest.to(sendAddressObject, Utils.toNanoCoins(sendAmount), 6, new BigInteger("10000"),1);
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
/*
		// Show option pane
		 final JOptionPane optionPane = new JOptionPane("Contacting message delivery servers...", JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
		 final JDialog dialog = new JDialog(mainFrame, "SparkBit", Dialog.ModalityType.APPLICATION_MODAL, null);
		 dialog.setContentPane(optionPane);
		 dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		 dialog.setLocationRelativeTo(mainFrame);
		 dialog.pack();
		 */
		// Show dialog with indeterminate progress bar
		final JDialog dialog = new JDialog(mainFrame, "SparkBit", Dialog.ModalityType.APPLICATION_MODAL);
		JProgressBar progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		BorderLayout bl= new BorderLayout();
		JPanel panel = new JPanel(bl);
		bl.setVgap(20);
		panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		// panel.setPreferredSize(new Dimension(600, 200));
		panel.add(new JLabel("Contacting message delivery servers..."), BorderLayout.PAGE_START);
		panel.add(progressBar, BorderLayout.CENTER);
		
		//dialog.getContentPane().add(panel);
		dialog.add(panel);
		dialog.pack();
		dialog.setLocationRelativeTo(mainFrame);

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
		
		Futures.addCallback(future, new FutureCallback<Boolean>() {
		    public void onSuccess(Boolean b) {

			// There is enough money.
			SwingUtilities.invokeLater(new Runnable() {
			    @Override
			    public void run() {
				dialog.dispose();

				SendBitcoinConfirmDialog mySendBitcoinConfirmDialog = new SendBitcoinConfirmDialog(bitcoinController, mainFrame, sendRequest);
				mySendBitcoinConfirmDialog.setVisible(true);
			    }
			});

		    }

		    public void onFailure(Throwable thrown) {
			final String failureReason = thrown.getMessage();
			final boolean isCSException = thrown instanceof org.coinspark.core.CSExceptions.CannotEncode;			
			// There is not enough money.
			// TODO setup validation parameters accordingly so that it displays ok.
			SwingUtilities.invokeLater(new Runnable() {
			    @Override
			    public void run() {
				dialog.dispose();

				if (isCSException) {
				    JOptionPane.showMessageDialog(mainFrame, "SparkBit is unable to proceed with this transaction:\n\n"+failureReason, "SparkBit Error", JOptionPane.ERROR_MESSAGE);
				} else {

				    ValidationErrorDialog myValidationErrorDialog = new ValidationErrorDialog(bitcoinController, mainFrame, sendRequest, true);
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

                    sendBitcoinConfirmDialog = new SendBitcoinConfirmDialog(super.bitcoinController, mainFrame, sendRequest);
                    sendBitcoinConfirmDialog.setVisible(true);
                } else {
                    // There is not enough money.
                    // TODO setup validation parameters accordingly so that it displays ok.
                    validationErrorDialog = new ValidationErrorDialog(super.bitcoinController, mainFrame, sendRequest, true);
                    validationErrorDialog.setVisible(true);
                }
	*/

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
