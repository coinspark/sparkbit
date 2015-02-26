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
package org.multibit.viewsystem.swing.view.dialogs;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet.BalanceType;
import com.google.bitcoin.core.Wallet.SendRequest;
import org.multibit.controller.Controller;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.exchange.CurrencyConverter;
import org.multibit.model.bitcoin.BitcoinModel;
import org.multibit.utils.ImageLoader;
import org.multibit.viewsystem.swing.ColorAndFontConstants;
import org.multibit.viewsystem.swing.MultiBitFrame;
import org.multibit.viewsystem.swing.action.HelpContextAction;
import org.multibit.viewsystem.swing.action.OkBackToParentAction;
import org.multibit.viewsystem.swing.view.components.*;
import org.multibit.viewsystem.swing.view.panels.HelpContentsPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import org.multibit.utils.CSMiscUtils;

import org.multibit.viewsystem.swing.action.AssetValidator;

/**
 * The validation error dialog - used to tell the user their input is invalid.
 */
public class AssetValidationErrorDialog extends MultiBitDialog {
    private static final long serialVersionUID = 551499812345057705L;

    private static final int HEIGHT_DELTA = 100;
    private static final int WIDTH_DELTA = 100;

    private final Controller controller;
    private final BitcoinController bitcoinController;
    private final SendRequest sendRequest;
    private boolean insufficientFee;
    
    private final AssetValidator assetValidator;
    
    /**
     * Creates a new {@link ValidationErrorDialog}.
     */
    public AssetValidationErrorDialog(BitcoinController bitcoinController, MultiBitFrame mainFrame, SendRequest sendRequest, boolean insufficientFee, AssetValidator assetValidator) {
        super(mainFrame, bitcoinController.getLocaliser().getString("validationErrorView.assetTitle"));
        
        this.bitcoinController = bitcoinController;
        this.controller = this.bitcoinController;
        this.sendRequest = sendRequest;
        this.insufficientFee = insufficientFee;
	this.assetValidator = assetValidator;

        initUI();
    }

    /**
     * Initialise the validation error dialog.
     */
    private void initUI() {
        // Get the data out of the user preferences.
        String addressValue = this.assetValidator.getState(AssetValidator.VALIDATION_ADDRESS_VALUE);
	String amountValue = this.assetValidator.getState(AssetValidator.VALIDATION_AMOUNT_VALUE);

	String assetAddressValue = this.assetValidator.getState(AssetValidator.VALIDATION_ASSET_ADDRESS_VALUE);
        String assetAmountValue = this.assetValidator.getState(AssetValidator.VALIDATION_ASSET_AMOUNT_VALUE);
  
        String amountPlusConversionToFiat = CurrencyConverter.INSTANCE.prettyPrint(amountValue);
        
	// BITCOIN
        // Invalid address.
        String addressIsInvalid = this.assetValidator.getState(AssetValidator.VALIDATION_ADDRESS_IS_INVALID);
        boolean addressIsInvalidBoolean = false;
        if (Boolean.TRUE.toString().equals(addressIsInvalid)) {
            addressIsInvalidBoolean = true;
        }
	
	// COINSPARK
	// Asset state is not valid
	boolean assetNotValid = Boolean.TRUE.toString().equals( this.assetValidator.getState(AssetValidator.VALIDATION_ASSET_STATE_NOT_VALID));
	
	// COINSPARK
	// VALIDATION_ASSET_ADDRESS_DOES_NOT_ACCEPT_TRANSFER
	String assetAddressDoesNotAcceptTransfer = this.assetValidator.getState(AssetValidator.VALIDATION_ASSET_ADDRESS_DOES_NOT_ACCEPT_TRANSFER);
        boolean addressDoesNotAcceptTransfer = false;
        if (Boolean.TRUE.toString().equals(assetAddressDoesNotAcceptTransfer)) {
            addressDoesNotAcceptTransfer = true;
        }
	
	// BITCOIN
        // Amount is missing.
        String amountIsMissing = this.assetValidator.getState(AssetValidator.VALIDATION_AMOUNT_IS_MISSING);
        boolean amountIsMissingBoolean = false;
        if (Boolean.TRUE.toString().equals(amountIsMissing)) {
            amountIsMissingBoolean = true;
        }

	// BITCOIN
        // Invalid amount i.e. not a number or could not parse.
        String amountIsInvalid = this.assetValidator.getState(AssetValidator.VALIDATION_AMOUNT_IS_INVALID);
        boolean amountIsInvalidBoolean = false;
        if (Boolean.TRUE.toString().equals(amountIsInvalid)) {
            amountIsInvalidBoolean = true;
        }

	// BITCOIN
        // Amount is negative or zero.
        String amountIsNegativeOrZero = this.assetValidator.getState(AssetValidator.VALIDATION_AMOUNT_IS_NEGATIVE_OR_ZERO);
        boolean amountIsNegativeOrZeroBoolean = false;
        if (Boolean.TRUE.toString().equals(amountIsNegativeOrZero)) {
            amountIsNegativeOrZeroBoolean = true;
        }

	// Amount too big to preserve bitcoins for migration of assets
        String amountIsTooBigForMigration =this.assetValidator.getState(AssetValidator.VALIDATION_AMOUNT_IS_TOO_BIG_FOR_MIGRATION);
        boolean amountIsTooBigForMigrationBoolean = false;
        if (Boolean.TRUE.toString().equals(amountIsTooBigForMigration)) {
            amountIsTooBigForMigrationBoolean = true;
        }
	
	
	// BITCOIN
        // Amount is too small.
        String amountIsTooSmall = this.assetValidator.getState(AssetValidator.VALIDATION_AMOUNT_IS_TOO_SMALL);
        boolean amountIsTooSmallBoolean = false;
        if (Boolean.TRUE.toString().equals(amountIsTooSmall)) {
            amountIsTooSmallBoolean = true;
        }

	// BITCOIN
        // Amount is more than available funds.
        String notEnoughFunds = this.assetValidator.getState(AssetValidator.VALIDATION_NOT_ENOUGH_FUNDS);
        boolean notEnoughFundsBoolean = false;
        if (Boolean.TRUE.toString().equals(notEnoughFunds) || insufficientFee) {
            notEnoughFundsBoolean = true;
        }
	
	
	
	
	// BITCOIN
        // Invalid address.
        String assetAddressIsInvalid = this.assetValidator.getState(AssetValidator.VALIDATION_ASSET_ADDRESS_IS_INVALID);
        boolean assetAddressIsInvalidBoolean = false;
        if (Boolean.TRUE.toString().equals(assetAddressIsInvalid)) {
            assetAddressIsInvalidBoolean = true;
        }

	// BITCOIN
        // Amount is missing.
        String assetAmountIsMissing = this.assetValidator.getState(AssetValidator.VALIDATION_ASSET_AMOUNT_IS_MISSING);
        boolean assetAmountIsMissingBoolean = false;
        if (Boolean.TRUE.toString().equals(assetAmountIsMissing)) {
            assetAmountIsMissingBoolean = true;
        }

	// BITCOIN
        // Invalid amount i.e. not a number or could not parse.
        String assetAmountIsInvalid = this.assetValidator.getState(AssetValidator.VALIDATION_ASSET_AMOUNT_IS_INVALID);
        boolean assetAmountIsInvalidBoolean = false;
        if (Boolean.TRUE.toString().equals(assetAmountIsInvalid)) {
            assetAmountIsInvalidBoolean = true;
        }

	// BITCOIN
        // Amount is negative or zero.
        String assetAmountIsNegativeOrZero = this.assetValidator.getState(AssetValidator.VALIDATION_ASSET_AMOUNT_IS_NEGATIVE_OR_ZERO);
        boolean assetAmountIsNegativeOrZeroBoolean = false;
        if (Boolean.TRUE.toString().equals(assetAmountIsNegativeOrZero)) {
            assetAmountIsNegativeOrZeroBoolean = true;
        }

	// BITCOIN
        // Amount is too small.
        String assetAmountIsTooSmall = this.assetValidator.getState(AssetValidator.VALIDATION_ASSET_AMOUNT_IS_TOO_SMALL);
        boolean assetAmountIsTooSmallBoolean = false;
        if (Boolean.TRUE.toString().equals(assetAmountIsTooSmall)) {
            assetAmountIsTooSmallBoolean = true;
        }
	
	
	// ASSET
	String notEnoughUnits = this.assetValidator.getState(AssetValidator.VALIDATION_ASSET_NOT_ENOUGH_UNITS);
        boolean notEnoughUnitsBoolean = false;
        if (Boolean.TRUE.toString().equals(notEnoughUnits)) {// || insufficientFee) {
            notEnoughUnitsBoolean = true;
        }

        // Get localised validation messages.
        StringBuilder completeMessage = new StringBuilder();

        int rows = 0;
        String longestRow = "";

	
	// ASSET
	if (assetNotValid) {
	    if (completeMessage.length() > 0) {
		completeMessage.append("\n");
	    }
            String textToAdd = controller.getLocaliser().getString("validationErrorView.assetNotValid");
            if (textToAdd.length() > longestRow.length()) {
                longestRow = textToAdd;
            }
            completeMessage.append(textToAdd);
	    rows++;	    
	}
	
	
	if (assetAddressIsInvalidBoolean) {
	    if (completeMessage.length() > 0) {
		completeMessage.append("\n");
	    }
            String textToAdd = controller.getLocaliser().getString("validationErrorView.assetAddressInvalidMessage",
                    new String[] { assetAddressValue });
            if (textToAdd.length() > longestRow.length()) {
                longestRow = textToAdd;
            }
            completeMessage.append(textToAdd);
	    rows++;
	}
	
	// ASSET
	if (addressDoesNotAcceptTransfer) {
	    if (completeMessage.length() > 0) {
		completeMessage.append("\n");
	    }
	    String textToAdd = controller.getLocaliser().getString("validationErrorView.assetAddressDoesNotAcceptTransferMessage");
            if (textToAdd.length() > longestRow.length()) {
                longestRow = textToAdd;
            }
            completeMessage.append(textToAdd);
	    rows++;
	}
	
	// BITCOIN
	if (addressIsInvalidBoolean) {
	    if (completeMessage.length() > 0) {
		completeMessage.append("\n");
	    }
            String textToAdd = controller.getLocaliser().getString("validationErrorView.bitcoinAddressInvalidMessage",
                    new String[] { addressValue });
            if (textToAdd.length() > longestRow.length()) {
                longestRow = textToAdd;
            }
            completeMessage.append(textToAdd);
	    rows++;
        }
        if (amountIsMissingBoolean) {
            if (completeMessage.length()>0) {
                completeMessage.append("\n");
            }
            String textToAdd = controller.getLocaliser().getString("validationErrorView.amountIsMissingMessage");
            if (textToAdd.length() > longestRow.length()) {
                longestRow = textToAdd;
            }
            completeMessage.append(textToAdd);
            rows++;
        }
        if (amountIsInvalidBoolean) {
            if (completeMessage.length() > 0) {
                completeMessage.append("\n");
            }
            String textToAdd = controller.getLocaliser().getString("validationErrorView.amountInvalidMessage",
                    new String[] { amountValue });
            if (textToAdd.length() > longestRow.length()) {
                longestRow = textToAdd;
            }
            completeMessage.append(textToAdd);

            rows++;
        }
      if (amountIsNegativeOrZeroBoolean) {
          if (completeMessage.length()>0) {
              completeMessage.append("\n");
          }

          String textToAdd = controller.getLocaliser().getString("validationErrorView.amountIsNegativeOrZeroMessage");
          if (textToAdd.length() > longestRow.length()) {
              longestRow = textToAdd;
          }
          completeMessage.append(textToAdd);

          rows++;
      }
      
            
      // Must keep a minimum balance in the wallet to preserve assets, unless there are no assets.
      if (amountIsTooBigForMigrationBoolean) {
          if (completeMessage.length()>0) {
              completeMessage.append("\n");
          }
	  BigInteger migrationCost = CSMiscUtils.calcMigrationFeeSatoshis(this.bitcoinController, this.bitcoinController.getModel().getActiveWallet());
	  String s = Utils.bitcoinValueToFriendlyString(migrationCost);
// TODO: Print out the migration fee , in BTC amount, saying must keep at least this amount in wallet,
          String textToAdd = controller.getLocaliser().getString("validationErrorView.amountIsTooBigForMigrationMessage", new String[]{s});
          if (textToAdd.length() > longestRow.length()) {
              longestRow = textToAdd;
          }
          completeMessage.append(textToAdd);
          rows++;
      }
      
      
      if (amountIsTooSmallBoolean) {
          if (completeMessage.length()>0) {
              completeMessage.append("\n");
          }

          String textToAdd = controller.getLocaliser().getString("validationErrorView.amountIsTooSmallMessage", new String[]{Transaction.MIN_NONDUST_OUTPUT.toString()});
          if (textToAdd.length() > longestRow.length()) {
              longestRow = textToAdd;
          }
          completeMessage.append(textToAdd);

          rows++;
      }
        if (notEnoughFundsBoolean) {
            if (completeMessage.length()>0) {
                completeMessage.append("\n");
            }

            String textToAdd = controller.getLocaliser().getString("validationErrorView.notEnoughFundsMessage",
                    new String[] { amountPlusConversionToFiat});
            if (this.bitcoinController.getModel().getActiveWallet().getBalance(BalanceType.AVAILABLE).compareTo(this.bitcoinController.getModel().getActiveWallet().getBalance(BalanceType.ESTIMATED)) != 0) {
                textToAdd = controller.getLocaliser().getString("validationErrorView.notEnoughFundsMessage2",
                        new String[] { amountPlusConversionToFiat});
            }
            // There is an extra "BTC." in the translations - remove and add a return.
            textToAdd = textToAdd.replaceAll("BTC\\.", "\\.");
             
            String[] lines = textToAdd.split("\\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i] != null && lines[i].length() > longestRow.length()) {
                    longestRow = lines[i];
                }
                if (lines[i] != null && lines[i].length() > 0) {
                    if (i == 0) {
                        completeMessage.append(lines[i]);
                    } else {
                        completeMessage.append("\n").append(lines[i]);
                    }
                    rows++;
                }
            }
        }
	
	
	
	// Spacer row between bitcoin and asset mesages
//        rows = rows + 1;
//	completeMessage.append("\n");
	
	
	
	// ASSET
//        if (assetAddressIsInvalidBoolean) {
//            completeMessage.append(controller.getLocaliser().getString("validationErrorView.assetAddressInvalidMessage",
//                    new String[] { assetAddressValue }));
//            longestRow = completeMessage.toString();
//            rows++;
//        }
        if (assetAmountIsMissingBoolean) {
            if (completeMessage.length()>0) {
                completeMessage.append("\n");
            }
            String textToAdd = controller.getLocaliser().getString("validationErrorView.assetAmountIsMissingMessage");
            if (textToAdd.length() > longestRow.length()) {
                longestRow = textToAdd;
            }
            completeMessage.append(textToAdd);
            rows++;
        }
        if (assetAmountIsInvalidBoolean) {
            if (completeMessage.length() > 0) {
                completeMessage.append("\n");
            }
            String textToAdd = controller.getLocaliser().getString("validationErrorView.assetAmountInvalidMessage",
                    new String[] { assetAmountValue });
            if (textToAdd.length() > longestRow.length()) {
                longestRow = textToAdd;
            }
            completeMessage.append(textToAdd);

            rows++;
        }
      if (assetAmountIsNegativeOrZeroBoolean) {
          if (completeMessage.length()>0) {
              completeMessage.append("\n");
          }

          String textToAdd = controller.getLocaliser().getString("validationErrorView.assetAmountIsNegativeOrZeroMessage");
          if (textToAdd.length() > longestRow.length()) {
              longestRow = textToAdd;
          }
          completeMessage.append(textToAdd);

          rows++;
      }
      
      
//      if (assetAmountIsTooSmallBoolean) {
//          if (completeMessage.length()>0) {
//              completeMessage.append("\n");
//          }
//
//          String textToAdd = controller.getLocaliser().getString("validationErrorView.assetAmountIsTooSmallMessage", new String[]{Transaction.MIN_NONDUST_OUTPUT.toString()});
//          if (textToAdd.length() > longestRow.length()) {
//              longestRow = textToAdd;
//          }
//          completeMessage.append(textToAdd);
//
//          rows++;
//      }
      
	if (notEnoughUnitsBoolean) {
	    if (completeMessage.length() > 0) {
		completeMessage.append("\n");
	    }

	    String textToAdd = controller.getLocaliser().getString("validationErrorView.assetNotEnoughUnitsMessage", new String[]{assetAmountValue});
//	    if (textToAdd.length() > longestRow.length()) {
//		longestRow = textToAdd;
//	    }
	    //completeMessage.append(textToAdd);

	    // With a multi-line string, the existing validation code computes row length of each segment separated by newline char.
	    String[] lines = textToAdd.split("\\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i] != null && lines[i].length() > longestRow.length()) {
                    longestRow = lines[i];
                }
                if (lines[i] != null && lines[i].length() > 0) {
                    if (i == 0) {
                        completeMessage.append(lines[i]);
                    } else {
                        completeMessage.append("\n").append(lines[i]);
                    }
                    rows++;
                }
            }
	}
      /*
        if (notEnoughFundsBoolean) {
            if (completeMessage.length()>0) {
                completeMessage.append("\n");
            }

            String textToAdd = controller.getLocaliser().getString("validationErrorView.notEnoughFundsMessage",
                    new String[] { amountPlusConversionToFiat});
            if (this.bitcoinController.getModel().getActiveWallet().getBalance(BalanceType.AVAILABLE).compareTo(this.bitcoinController.getModel().getActiveWallet().getBalance(BalanceType.ESTIMATED)) != 0) {
                textToAdd = controller.getLocaliser().getString("validationErrorView.notEnoughFundsMessage2",
                        new String[] { amountPlusConversionToFiat});
            }
            // There is an extra "BTC." in the translations - remove and add a return.
            textToAdd = textToAdd.replaceAll("BTC\\.", "\\.");
             

        }
*/
	
	
        // Spacer row at top and bottom.
//        rows = rows + 2;
	
	/*
			JOptionPane.showMessageDialog(
			this.mainFrame,
			completeMessage.toString(),
			"Send Asset Error",
			JOptionPane.ERROR_MESSAGE, // INFORMATION_MESSAGE,
			ImageLoader.createImageIcon(ImageLoader.EXCLAMATION_MARK_ICON_FILE)
			);
			
			this.setVisible(false);
	*/		
			
        
        // Tell user validation messages.
        Action availableToSpendHelpAction = new HelpContextAction(controller, null, "validationErrorView.moreHelp",
                "multiBitFrame.helpMenuTooltip", "multiBitFrame.helpMenuText", HelpContentsPanel.HELP_AVAILABLE_TO_SPEND_URL);
        HelpButton availableToSpendHelpButton = new HelpButton(availableToSpendHelpAction, controller, true);
        final AssetValidationErrorDialog finalValidationErrorDialog = this;
        availableToSpendHelpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                finalValidationErrorDialog.setVisible(false);
                
            }});

        OkBackToParentAction okAction = new OkBackToParentAction(controller, this);
        MultiBitButton okButton = new MultiBitButton(okAction, controller);
        okButton.setOpaque(true);
        okButton.setBackground(ColorAndFontConstants.BACKGROUND_COLOR);

        Object[] options = {okButton};
        if (this.bitcoinController.getModel().getActiveWallet().getBalance(BalanceType.AVAILABLE).compareTo(this.bitcoinController.getModel().getActiveWallet().getBalance(BalanceType.ESTIMATED)) != 0) {
            options = new Object[] { okButton, availableToSpendHelpButton};
        }
	
//	System.out.println(">>>> ERROR MESSAGE\n"+completeMessage.toString()+"\n>>>> END ERROR MESSAGE");
//	System.out.println("rows = " + rows);
	/*
	MultiBitTextArea completeMessageTextArea = new MultiBitTextArea(completeMessage.toString(), rows, 20, controller);
//        MultiBitTextArea completeMessageTextArea = new MultiBitTextArea("\n" + completeMessage.toString() + "\n", rows, 20, controller);
        completeMessageTextArea.setOpaque(false);
        completeMessageTextArea.setBackground(ColorAndFontConstants.BACKGROUND_COLOR);
        completeMessageTextArea.setEditable(false);
        completeMessageTextArea.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        completeMessageTextArea.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
		*/

	ImageIcon icon = ImageLoader.createImageIcon(ImageLoader.EXCLAMATION_MARK_ICON_FILE);
        JOptionPane optionPane = new JOptionPane(
		completeMessage.toString(),
		//completeMessageTextArea,	
		JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION,
                icon, options, options[0]);
		
        add(optionPane);
        optionPane.setBackground(ColorAndFontConstants.BACKGROUND_COLOR);
        optionPane.setOpaque(true);
        FontMetrics fontMetrics = optionPane.getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont());

	rows += 1; // single error, dialog not tall enough

	// FUDGE: The validation dialog is a bit of a mess and needs to be fixed.  This is a temporary hack
	// to make it look nicer instead of stretching across the screen.
        String[] lines = completeMessage.toString().split("\\n");
	rows = lines.length;
	longestRow = "";
	for (String line : lines) {
	    if (line.length() > longestRow.length()) {
		longestRow = line;
	    }
	}
	
        int minimumHeight = fontMetrics.getHeight() * rows + HEIGHT_DELTA + (2 * okButton.getHeight());
        int minimumWidth = fontMetrics.stringWidth(longestRow) + WIDTH_DELTA;
        setMinimumSize(new Dimension(minimumWidth, minimumHeight));
        positionDialogRelativeToParent(this, 0.5D, 0.47D);
    }
}