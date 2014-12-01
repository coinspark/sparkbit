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

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.BalanceType;
import com.google.bitcoin.wallet.CoinSelector;
import org.multibit.controller.Controller;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.exchange.CurrencyConverter;
import org.multibit.exchange.CurrencyConverterResult;
import org.multibit.model.bitcoin.BitcoinModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import java.util.concurrent.ConcurrentHashMap;
import org.coinspark.wallet.*;
import org.coinspark.wallet.CSAsset.CSAssetState;
import com.google.common.primitives.Ints;
import org.coinspark.protocol.CoinSparkAddress;
import org.multibit.utils.CSMiscUtils;

/**
 * A class to validate String addresses and amounts.
 * TODO - this should create a validation state object and have a getter
 * 
 * @author jim
 * 
 * SparkBit - we use a hash map to store info rather than set wallet preferences.
 * 
 */
public class AssetValidator {
    // Validation, originally taken from BitcoinModel.
    public static final String VALIDATION_ADDRESS_IS_INVALID = "validationAddressIsInvalid";
    public static final String VALIDATION_AMOUNT_IS_INVALID = "validationAmountIsInvalid";
    public static final String VALIDATION_AMOUNT_IS_MISSING = "validationAmountIsMissing";
    public static final String VALIDATION_AMOUNT_IS_NEGATIVE_OR_ZERO = "validationAmountIsNegativeOrZero";
    public static final String VALIDATION_AMOUNT_IS_TOO_SMALL = "validationAmountIsTooSmall";
    public static final String VALIDATION_NOT_ENOUGH_FUNDS = "validationNotEnoughFunds";
    public static final String VALIDATION_AMOUNT_VALUE = "validationAmountValue";
    public static final String VALIDATION_ADDRESS_VALUE = "validationAddressValue";
    
    public static final String VALIDATION_ASSET_ADDRESS_IS_INVALID = "validationAssetAddressIsInvalid";
    public static final String VALIDATION_ASSET_ADDRESS_DOES_NOT_ACCEPT_TRANSFER = "validationAssetDoesNotAcceptTransfer";
    public static final String VALIDATION_ASSET_AMOUNT_IS_INVALID = "validationAssetAmountIsInvalid";
    public static final String VALIDATION_ASSET_AMOUNT_IS_MISSING = "validationAssetAmountIsMissing";
    public static final String VALIDATION_ASSET_AMOUNT_IS_NEGATIVE_OR_ZERO = "validationAssetAmountIsNegativeOrZero";
    public static final String VALIDATION_ASSET_AMOUNT_IS_TOO_SMALL = "validationAssetAmountIsTooSmall";
    public static final String VALIDATION_ASSET_NOT_ENOUGH_UNITS = "validationAssetNotEnoughUnits";
    public static final String VALIDATION_ASSET_AMOUNT_VALUE = "validationAssetAmountValue";
    public static final String VALIDATION_ASSET_ADDRESS_VALUE = "validationAssetAddressValue";

    public static final String VALIDATION_AMOUNT_IS_TOO_BIG_FOR_MIGRATION = "validationAmountIsTooBigForMigration";
    
    public static final String VALIDATION_ASSET_STATE_NOT_VALID = "validationAssetStateNotValid";
    
    private static final Logger log = LoggerFactory.getLogger(AssetValidator.class);

    private final Controller controller;
    private final BitcoinController bitcoinController;
    
    private ConcurrentHashMap<String, String> validationState;

//    public ConcurrentHashMap<String, String> getValidationState() {
//	return validationState;
//    }
    
    public String getState(String key) {
	return validationState.get(key);
    }

    public AssetValidator(BitcoinController bitcoinController) {
        this.bitcoinController = bitcoinController;
        this.controller = this.bitcoinController;
	
	validationState = new ConcurrentHashMap<>();
    }

    /**
     * Validate a String address and amount.
     * 
     * @param address
     * @param amount
     * @param assetId
     * @param assetAmount This must be in raw units
     * @return
     */
    public boolean validate(String address, String amount, int assetId, String assetAmount) {
        clearValidationState();

	//boolean validAddress = validateAddress(address);
	boolean validAsset = validateAssetState(assetId);
        boolean validAmount = validateAmount(amount);
        boolean validAssetAddress = validateAssetAddress(address);
	boolean validAssetAmount = validateAssetAmount(assetId, assetAmount);
	
	log.debug("validator state = " + validationState);
	
        return validAsset && validAmount && validAssetAddress && validAssetAmount;
    }

    private boolean validateAssetState(int assetId) {
	Wallet wallet = this.bitcoinController.getModel().getActiveWallet();
	CSAsset asset = wallet.CS.getAsset(assetId);
	boolean isValid = false;
	if (asset!=null) {
	    CSAssetState state = asset.getAssetState();
	    if (state==CSAssetState.VALID || CSMiscUtils.canSendInvalidAsset(this.bitcoinController)) {
		isValid = true;
	    }
	}
	
	validationState.put(VALIDATION_ASSET_STATE_NOT_VALID, (new Boolean(!isValid)).toString());

	return isValid;
    }
    
    private boolean validateAssetAmount(int assetId, String amount) {
	//boolean result = false;
	Wallet wallet = this.bitcoinController.getModel().getActiveWallet();
	int[] array = wallet.CS.getAssetIDs();
	boolean assetExists = false;
	if (array!=null) {
	    assetExists = Ints.contains(array, assetId);
	}
	CSAsset asset = wallet.CS.getAsset(assetId);
	if (asset!=null) {
	    CSAssetState state = asset.getAssetState();
	    if (state!=CSAssetState.VALID) {
		// TODO: Set error message that the asset state is no longer valid.
	    }
	}
    
	BigInteger spendableAmount =  wallet.CS.getAssetBalance(assetId).spendable; 
//	BigInteger spendableAmount = wallet.CS.getUnspentAssetQuantity(assetId, true);
//	BigInteger availableAmount = wallet.CS.getUnspentAssetQuantity(assetId);

	// Convert assetAmount to 
	//log.info("asset amount available to send is : "+availableQuantity);

        Boolean amountValidatesOk = Boolean.TRUE;

        Boolean amountIsInvalid = Boolean.FALSE;
        Boolean notEnoughUnits = Boolean.FALSE;
        Boolean amountIsMissing = Boolean.FALSE;
        Boolean amountIsNegativeOrZero = Boolean.FALSE;
        Boolean amountIsTooSmall = Boolean.FALSE;

        // See if the amount is missing.
        if (amount == null || "".equals(amount) || amount.trim().length() == 0) {
            amountIsMissing = Boolean.TRUE;
            amountValidatesOk = Boolean.FALSE;
        } else {
            // See if the amount is a number.
            BigInteger amountBigInteger = null;
            try {
		amountBigInteger = new BigInteger(amount);
		validationState.put(VALIDATION_ASSET_AMOUNT_VALUE, CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, amountBigInteger));
//                CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.parseToBTCNotLocalised(amount);
//                if (converterResult.isBtcMoneyValid()) {
//                    // Parses ok.
//                    amountBigInteger = converterResult.getBtcMoney().getAmount().toBigInteger();
//                } else {
//                    amountIsInvalid = Boolean.TRUE;
//                    amountValidatesOk = Boolean.FALSE;
//                }
            } catch (NumberFormatException nfe) {
                amountValidatesOk = Boolean.FALSE;
                amountIsInvalid = Boolean.TRUE;
            } catch (ArithmeticException ae) {
                amountValidatesOk = Boolean.FALSE;
                amountIsInvalid = Boolean.TRUE;
            }

            // See if the amount is negative or zero.
            if (amountValidatesOk.booleanValue()) {
		//        if (bigint.compareTo(BigInteger.ZERO) < 0)
//            throw new ArithmeticException("Negative units specified.");
//	if (bigint.equals(BigInteger.ZERO))
//	    throw new ArithmeticException("Amount must be more than zero units.");
//        if (bigint.compareTo(MAXIMUM_AMOUNT_OF_RAW_UNITS) > 0)
//            throw new ArithmeticException("Amount larger than the total quantity of units possible specified.");

                if (amountBigInteger.compareTo(BigInteger.ZERO) <= 0) {
                    amountValidatesOk = Boolean.FALSE;
                    amountIsNegativeOrZero = Boolean.TRUE;
                } else {
		    if (amountBigInteger.compareTo(CSMiscUtils.MAXIMUM_AMOUNT_OF_RAW_UNITS)>0 ||
			    amountBigInteger.compareTo(spendableAmount)>0) {
			amountValidatesOk = Boolean.FALSE;
			notEnoughUnits = Boolean.TRUE;
		    }
		}
		    
		  // TODO: Need to validate BTC amount is valid, given tx fee.
//                  if (amountBigInteger.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0) {
//                    amountValidatesOk = Boolean.FALSE;
//                    amountIsTooSmall = Boolean.TRUE;
//                  } else {
//                    // The fee is worked out in detail later, but we know it will be at least the minimum reference amount.
//                    BigInteger totalSpend = amountBigInteger.add(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);
//                    BigInteger availableBalance = this.bitcoinController.getModel().getActiveWallet().getBalance(BalanceType.AVAILABLE);
//                    BigInteger estimatedBalance = this.bitcoinController.getModel().getActiveWallet().getBalance(BalanceType.ESTIMATED);
//
//                    log.debug("Amount = " + amountBigInteger.toString() + ", fee of at least " + Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.toString()
//                            + ", totalSpend = " + totalSpend.toString() + ", availableBalance = " + availableBalance.toString() + ", estimatedBalance = " + estimatedBalance.toString());
//                    if (totalSpend.compareTo(availableBalance) > 0) {
//                      // Not enough funds.
//                      amountValidatesOk = Boolean.FALSE;
//                      notEnoughFunds = Boolean.TRUE;
//                    }
                  }
	}
	
        validationState.put(VALIDATION_ASSET_AMOUNT_IS_MISSING, amountIsMissing.toString());
        validationState.put(VALIDATION_ASSET_AMOUNT_IS_NEGATIVE_OR_ZERO, amountIsNegativeOrZero.toString());
        //validationState.put(VALIDATION_ASSET_AMOUNT_IS_TOO_SMALL, amountIsTooSmall.toString());
        validationState.put(VALIDATION_ASSET_AMOUNT_IS_INVALID, amountIsInvalid.toString());
        validationState.put(VALIDATION_ASSET_NOT_ENOUGH_UNITS, notEnoughUnits.toString());

        return amountValidatesOk.booleanValue();
    }

    // TODO: We check if the CoinSpark address is valid and flags allow transfer or not.
    private boolean validateAssetAddress(String address) {
	boolean result = false;
        Boolean sparkAddressIsInvalid = Boolean.TRUE;

        if (address != null && !address.isEmpty()) {
            // Copy address to wallet preferences.
            //this.bitcoinController.getModel().setActiveWalletPreference(BitcoinModel.VALIDATION_ADDRESS_VALUE, address);
	    validationState.put(VALIDATION_ASSET_ADDRESS_VALUE, address);
	    
	    // First, let's validate the coinspark address, and check the address flags to see if they allow transfer.
	    CoinSparkAddress csa = CSMiscUtils.decodeCoinSparkAddress(address);
	    String btcAddress = null;
	    if (csa!=null) {
		btcAddress = CSMiscUtils.getBitcoinAddressStringFromCoinSparkAddress(csa);
		boolean canTransfer = (csa.getAddressFlags() & CoinSparkAddress.COINSPARK_ADDRESS_FLAG_ASSETS) > 0;		
		boolean validBtcAddress = false;
		if (btcAddress!=null) {
		    validationState.put(VALIDATION_ADDRESS_VALUE, btcAddress);

		    // Second, let's check the underlying bitcoin address.
		    try {
			if (btcAddress != null) {
			    new Address(this.bitcoinController.getModel().getNetworkParameters(), btcAddress);
			    validBtcAddress = true;
			}
		    } catch (AddressFormatException afe) {
			// Carry on.
		    } catch (java.lang.StringIndexOutOfBoundsException e) {
			// Carry on.
		    }
		} else {
		    validationState.put(VALIDATION_ADDRESS_VALUE, "");
		}
		
		if (validBtcAddress) {
		    sparkAddressIsInvalid = Boolean.FALSE;

		    if (!canTransfer) {
			validationState.put(VALIDATION_ASSET_ADDRESS_DOES_NOT_ACCEPT_TRANSFER, Boolean.TRUE.toString());
		    } else {
			result = true; // everthing is good, so return value is true so validation passes.
		    }
		    validationState.put(VALIDATION_ADDRESS_IS_INVALID, Boolean.FALSE.toString());
		} else {
		    validationState.put(VALIDATION_ADDRESS_IS_INVALID, Boolean.TRUE.toString());
		}
		
	    }

        } else {
	    validationState.put(VALIDATION_ASSET_ADDRESS_VALUE, "");
        }
	validationState.put(VALIDATION_ASSET_ADDRESS_IS_INVALID, sparkAddressIsInvalid.toString());

        //return !sparkAddressIsInvalid;
	return result;
    }

    
    // Adapted from Validator.java, to check BTC address is valid.
    private boolean validateAddress(String address) {
        Boolean addressIsInvalid = Boolean.TRUE;

        if (address != null && !address.isEmpty()) {
            // Copy address to wallet preferences.
	    validationState.put(VALIDATION_ADDRESS_VALUE, address);

            try {
                new Address(this.bitcoinController.getModel().getNetworkParameters(), address);
                addressIsInvalid = Boolean.FALSE;
            } catch (AddressFormatException afe) {
                // Carry on.
            } catch (java.lang.StringIndexOutOfBoundsException e) {
                // Carry on.
            }
        } else {
	    validationState.put(VALIDATION_ADDRESS_VALUE, "");
        }
	validationState.put(VALIDATION_ADDRESS_IS_INVALID, addressIsInvalid.toString());

        return !addressIsInvalid.booleanValue();
    }

    // Adapted from Validator.java
    // TODO: Add a new key and check that the coinspark wallet's minimum required BTC balance is okay. 
    private boolean validateAmount(String amount) {
        // Copy amount to wallet preferences.
        validationState.put(VALIDATION_AMOUNT_VALUE, amount);

        Boolean amountValidatesOk = Boolean.TRUE;
	
        Boolean amountIsInvalid = Boolean.FALSE;
        Boolean notEnoughFunds = Boolean.FALSE;
        Boolean amountIsMissing = Boolean.FALSE;
        Boolean amountIsNegativeOrZero = Boolean.FALSE;
        Boolean amountIsTooSmall = Boolean.FALSE;
	Boolean amountIsTooBigForMigration = Boolean.FALSE;	    

        // See if the amount is missing.
        if (amount == null || "".equals(amount) || amount.trim().length() == 0) {
            amountIsMissing = Boolean.TRUE;
            amountValidatesOk = Boolean.FALSE;
        } else {
            // See if the amount is a number.
            BigInteger amountBigInteger = null;
            try {
                CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.parseToBTCNotLocalised(amount);
                if (converterResult.isBtcMoneyValid()) {
                    // Parses ok.
                    amountBigInteger = converterResult.getBtcMoney().getAmount().toBigInteger();
                } else {
                    amountIsInvalid = Boolean.TRUE;
                    amountValidatesOk = Boolean.FALSE;
                }
            } catch (NumberFormatException nfe) {
                amountValidatesOk = Boolean.FALSE;
                amountIsInvalid = Boolean.TRUE;
            } catch (ArithmeticException ae) {
                amountValidatesOk = Boolean.FALSE;
                amountIsInvalid = Boolean.TRUE;
            }

            // See if the amount is negative or zero.
            if (amountValidatesOk.booleanValue()) {
                if (amountBigInteger.compareTo(BigInteger.ZERO) <= 0) {
                    amountValidatesOk = Boolean.FALSE;
                    amountIsNegativeOrZero = Boolean.TRUE;
                } else {
                  if (amountBigInteger.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0) {
                    amountValidatesOk = Boolean.FALSE;
                    amountIsTooSmall = Boolean.TRUE;
                  } else {
                    // The fee is worked out in detail later, but we know it will be at least the minimum reference amount.
                    BigInteger totalSpend = amountBigInteger.add(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);
                    BigInteger availableBalance = this.bitcoinController.getModel().getActiveWallet().getBalance(BalanceType.AVAILABLE);
                    BigInteger estimatedBalance = this.bitcoinController.getModel().getActiveWallet().getBalance(BalanceType.ESTIMATED);

                    log.debug("Amount = " + amountBigInteger.toString() + ", fee of at least " + Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.toString()
                            + ", totalSpend = " + totalSpend.toString() + ", availableBalance = " + availableBalance.toString() + ", estimatedBalance = " + estimatedBalance.toString());
                    if (totalSpend.compareTo(availableBalance) > 0) {
                      // Not enough funds.
                      amountValidatesOk = Boolean.FALSE;
                      notEnoughFunds = Boolean.TRUE;
                    }
	
		    // Min balance for migration?
		    if (amountValidatesOk.booleanValue()) {
			boolean b = CSMiscUtils.canSafelySpendWhileRespectingMigrationFee(this.bitcoinController, this.bitcoinController.getModel().getActiveWallet(), amountBigInteger);
			if (!b) {
			    amountValidatesOk = Boolean.FALSE;
			    amountIsTooBigForMigration = Boolean.TRUE;
			}
		    }
                  }
                }
            }
        }
	validationState.put(VALIDATION_AMOUNT_IS_MISSING, amountIsMissing.toString());
        validationState.put(VALIDATION_AMOUNT_IS_NEGATIVE_OR_ZERO, amountIsNegativeOrZero.toString());
        validationState.put(VALIDATION_AMOUNT_IS_TOO_SMALL, amountIsTooSmall.toString());
        validationState.put(VALIDATION_AMOUNT_IS_INVALID, amountIsInvalid.toString());
        validationState.put(VALIDATION_NOT_ENOUGH_FUNDS, notEnoughFunds.toString());
	validationState.put(VALIDATION_AMOUNT_IS_TOO_BIG_FOR_MIGRATION, amountIsTooBigForMigration.toString());
        return amountValidatesOk.booleanValue();
    }
	
	
	
    public void clearValidationState() {
	validationState.clear();
    }
    
    

}

