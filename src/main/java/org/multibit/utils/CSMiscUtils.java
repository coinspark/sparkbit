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
package org.multibit.utils;

//import com.google.bitcoin.core.NetworkParameters;
//import static com.google.bitcoin.core.Utils.COIN;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.TransactionOutput;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Date;

import org.coinspark.protocol.CoinSparkAddress;
import org.coinspark.protocol.CoinSparkAssetRef;
import org.coinspark.protocol.CoinSparkGenesis;
import org.coinspark.protocol.CoinSparkPaymentRef;
import org.coinspark.wallet.CSAsset;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import org.coinspark.wallet.CSAssetDatabase;
import org.apache.commons.lang3.StringUtils;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.multibit.viewsystem.swing.ColorAndFontConstants;
import org.multibit.viewsystem.swing.view.components.MultiBitLabel;

import org.multibit.controller.bitcoin.BitcoinController;
import com.google.bitcoin.core.Wallet.SendRequest;
import org.joda.time.LocalDateTime;

/*
 * Mixed bag of tools.
 */

public class CSMiscUtils {
    
    /**
     * The maximum money to be generated
     */
    public static final BigInteger MAXIMUM_AMOUNT_OF_RAW_UNITS = new BigInteger("10").pow(14);

    //public static final int NUMBER_OF_RAW_SIGNIFICANT_DIGITS = 3;
    
    private CSMiscUtils() {
    }
    
    /*
    Convert display units to raw units.
    */
    public static BigInteger toRawUnits(String displayUnits, int movePointRight) {
	if (movePointRight==0) {
	    return new BigInteger(displayUnits);
	}
        BigInteger bigint = new BigDecimal(displayUnits).movePointRight(movePointRight).toBigIntegerExact();
//        if (bigint.compareTo(BigInteger.ZERO) < 0)
//            throw new ArithmeticException("Negative units specified.");
//	if (bigint.equals(BigInteger.ZERO))
//	    throw new ArithmeticException("Amount must be more than zero units.");
//        if (bigint.compareTo(MAXIMUM_AMOUNT_OF_RAW_UNITS) > 0)
//            throw new ArithmeticException("Amount larger than the total quantity of units possible specified.");
        return bigint;
    }
    
    
    /*
    Get the bitcoin address, else null, meaning the input address is invalid.
    */
    public static CoinSparkAddress decodeCoinSparkAddress(String coinSparkAddress) {
	CoinSparkAddress csa = new CoinSparkAddress();
	boolean b = csa.decode(coinSparkAddress);
	if (b && csa.isValid()) {
	    return csa;
	}
	return null;
    }
    
    public static String getBitcoinAddressFromCoinSparkAddress(String s) {
	CoinSparkAddress csa  = decodeCoinSparkAddress(s);
	String result = null;
	if (csa!=null) {
	    result = csa.getBitcoinAddress();
	}
	return result;
    }
    
    public static String getBitcoinAddressStringFromCoinSparkAddress(CoinSparkAddress csa) {
	if (csa==null) return null;
	return csa.getBitcoinAddress();
    }
    
    public static boolean validateBitcoinAddress(String address, BitcoinController bitcoinController) {
	boolean isValid = false;
        if (address != null && !address.isEmpty()) {
            try {
                new Address(bitcoinController.getModel().getNetworkParameters(), address);
                isValid = true;
            } catch (AddressFormatException afe) {
                // Carry on.
            } catch (java.lang.StringIndexOutOfBoundsException e) {
                // Carry on.
            }
	}
	return isValid;
    }
    
    /*
    Based on examples listed: http://coinspark.org/developers/coinspark-addresses/
    */
    public static String convertBitcoinAddressToCoinSparkAddress(String bitcoinAddress) {
	CoinSparkAddress csa = new CoinSparkAddress();
        int flags=CoinSparkAddress.COINSPARK_ADDRESS_FLAG_ASSETS;
	//| CoinSparkAddress.COINSPARK_ADDRESS_FLAG_PAYMENT_REFS;
	csa.setAddressFlags(flags);
	csa.setBitcoinAddress(bitcoinAddress);
	//csa.setPaymentRef(new CoinSparkPaymentRef(0));

	String s = csa.encode();
	return s;
    }
    
    public static boolean canSendAssetsToCoinSparkAddress(CoinSparkAddress csa) {
	int flags = csa.getAddressFlags();
	boolean b = (flags & CoinSparkAddress.COINSPARK_ADDRESS_FLAG_ASSETS)>0;
	return b;
    }
 
    /*
     http://coinspark.org/developers/asset-genesis-metadata/
     https://github.com/mastercoin-MSC/spec/blob/master/AssetIssuanceStandard.md
     From sample contract:
     Display  Quantity  =  Floor(Raw  Quantity  ×  (1.0  +  (Interest  Rate  ÷  100)) Years  Elapsed )  ×  Display  Multiple  
     where    Floor()    is    a    function    which    rounds    down    to    the    nearest    integer,    and    Years    Elapsed    is    equal    to   
     the    number    of    seconds    that    have    elapsed    within    the    Time    Zone    since    the    Issue    Date    divided    by   
     31,557,600  (i.e.  the  number  of  seconds  in  365.25  days).  
     */
    public static final int COINSPARK_SECONDS_IN_YEAR = 31557600;

    public static String getFormattedDisplayString(CSAsset asset, BigDecimal d) {
	if (asset==null) return "";
	// FIXME: TODO: Format the string, separate method to do it.
	/*
	 https://github.com/mastercoin-MSC/spec/blob/master/AssetIssuanceStandard.md
	 "format": "* dollars",
	 "format_1": "1 dollar",
	 */
	String result = d.toPlainString(); // + " units"; // default
	if (0 == d.compareTo(BigDecimal.ONE)) {
	    String fmt = asset.getFormat1();
	    if (fmt != null && fmt.trim().length()!=0) {
		result = fmt;
		return result;
	    } 
	}
	
	// If value is not 1, or value is 1 but format_1 does not exist, use format.
	String fmt = asset.getFormat();
	// watch out, fmt can be empty string
	if (fmt != null &&
		fmt.trim().length() != 0 &&
		org.apache.commons.lang3.StringUtils.countMatches(fmt, "*")==1)
	{
	    result = fmt.replaceFirst("\\*", d.toPlainString());
	}
	return result;
    }
    
    public static String getFmtLeftPortion(CSAsset asset) {
	String result = null;
	String fmt = asset.getFormat();
	int numAsterisks  = org.apache.commons.lang3.StringUtils.countMatches(fmt, "*");
	if (fmt != null && fmt.trim().length() != 0 && numAsterisks==1) {
	    int i = fmt.indexOf("*");
	    if (i>=1) {
		result = fmt.substring(0, i);
	    }
	}
	return result;
    }
    
    public static String getFmtRightPortion(CSAsset asset) {
	String result = null;
	String fmt = asset.getFormat();
	int numAsterisks  = org.apache.commons.lang3.StringUtils.countMatches(fmt, "*");
	if (fmt != null && fmt.trim().length() != 0  && numAsterisks==1 ) {
	    int i = fmt.indexOf("*");
	    if (i+1 <= fmt.length()) {
		result = fmt.substring(i+1);
	    }
	}
	return result;
    }
    
    public static String getFormattedDisplayStringForRawUnits(CSAsset asset, BigInteger rawQuantity) {
	if (asset==null) return "";
	BigDecimal d = getDisplayUnitsForRawUnits(asset, rawQuantity);
	return getFormattedDisplayString(asset, d);
    }
    
    public static BigDecimal getDisplayUnitsForRawUnits(CSAsset asset, BigInteger rawQuantity) {
	if (asset==null) return BigDecimal.ZERO;
	CoinSparkGenesis genesis = asset.getGenesis();
	if (genesis==null) return BigDecimal.ZERO; // This can happen with brand new Manually transferred asset which has not yet been validated.
	int chargeBasisPoints = genesis.getChargeBasisPoints();
	int chargeExponent = genesis.getChargeFlatExponent();
	int chargeMantissa = genesis.getChargeFlatMantissa();
	int qtyExponent = genesis.getQtyExponent();
	int qtyMantissa = genesis.getQtyMantissa();

	double interestRate = asset.getInterestRate();
	Date issueDate = asset.getIssueDate();

	BigDecimal result = new BigDecimal(rawQuantity.toString());

	//System.out.println("interest rate = " + interestRate);
	//System.out.println("issue date = " + issueDate);

	//System.out.println("raw units =" + result);

	// 1. Compute interest
	if (issueDate != null && interestRate != 0.0) {

	    BigDecimal rate = new BigDecimal(String.valueOf(interestRate));
	    rate = rate.divide(new BigDecimal(100));
	    rate = rate.add(BigDecimal.ONE);
	    //interestRate = interestRate / 100;

	    //System.out.println("interest rate 1 + ir/100 = " + rate.toPlainString());

	    // get years elapsed
	    DateTime d1 = new DateTime(issueDate);
	    DateTime d2 = new DateTime();

	    //System.out.println("Issue: " + d1 + "   Now: " + d2);
	    int seconds = Math.abs(Seconds.secondsBetween(d1, d2).getSeconds());

	    //System.out.println("...Number of seconds difference: " + seconds);

	    BigDecimal elapsedSeconds = new BigDecimal(seconds);

	    //System.out.println("...Number of seconds difference: " + elapsedSeconds.toPlainString());

	    // To avoid exception, we need to set a precision.
	    // java.lang.ArithmeticException: Non-terminating decimal expansion; no exact representable decimal result.
	    // http://stackoverflow.com/questions/4591206/arithmeticexception-non-terminating-decimal-expansion-no-exact-representable
	    BigDecimal elapsedYears = elapsedSeconds.divide(new BigDecimal(COINSPARK_SECONDS_IN_YEAR), MathContext.DECIMAL32);
	    //System.out.println("...Number of years difference: " + elapsedYears.toPlainString());

	    double base = elapsedSeconds.doubleValue();
	    double exp = elapsedYears.doubleValue();
	    //System.out.println("...base=" + base + "  exponent=" + exp);
	    double interestMultipler = Math.pow(rate.doubleValue(), elapsedYears.doubleValue());

	    //System.out.println("interest multipler =" + interestMultipler);

	    result = result.multiply(new BigDecimal(interestMultipler));

	    //System.out.println("raw units with interest multiplier =" + result);

	    result = result.setScale(0, RoundingMode.DOWN);

	    //System.out.println("raw units with interest multiplier, floored =" + result);

	}

	// 2. Apply multiple
	int decimalPlaces = CSMiscUtils.getNumberOfDisplayDecimalPlaces(asset);
	BigDecimal display = result;
	if (decimalPlaces != 0) {
//	    System.out.println(">>>>> display = " + display.toPlainString());
	    display = result.movePointLeft(decimalPlaces);
//	    System.out.println(">>>>> display = " + display.toPlainString());
	}
	
	
	//long qty = Utils.mantissaExponentToQty(qtyMantissa, qtyExponent);
//	double multiple = asset.getMultiple();
// let's just do it for now to make sure code is corret	
//if (multiple != 1.0)
//	BigDecimal m = new BigDecimal(String.valueOf(multiple));
//	BigDecimal display = result.multiply(m);

	//System.out.println("multiplier=" + m + ", display=" + display);
	// Stripping zeros from internal zero with different scale does not work, so use 
	// JDK bug still seems to exist
	// http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6480539
	// http://stackoverflow.com/questions/5239137/clarification-on-behavior-of-bigdecimal-striptrailingzeroes
	int cmpZeroResult = display.compareTo(BigDecimal.ZERO);
	if (decimalPlaces==0) {
	    display = display.stripTrailingZeros();
	}
	
	// Stripping trailing zeros from internal zero with different scale does not work, so set to ZERO instead.
	if (0 == cmpZeroResult) {
	    display = BigDecimal.ZERO;
	}
	return display;
    }

    // TODO: Not the whole formatted display string, just the string representing number
    public static BigInteger getRawUnitsFromDisplayString(CSAsset asset, String display) {
	BigDecimal result = null;
	try {
	    //System.out.println("Start to get raw units from: " + display);
	    result = new BigDecimal(display);
	} catch (NumberFormatException nfe) {
	    nfe.printStackTrace();
	    return null;
	}

	// Reverse apply the multiple
	int decimalPlaces = CSMiscUtils.getNumberOfDisplayDecimalPlaces(asset);
	if (decimalPlaces != 0) {
	    result = result.movePointRight(decimalPlaces);
	}
	// FIXME: what if multiple is 0.0? ignore? error?
//	double multiple = asset.getMultiple();
//	BigDecimal m = new BigDecimal(String.valueOf(multiple));
//	result = result.divide(m, MathContext.DECIMAL32);

	//System.out.println("multiplier=" + m + ", removed multiplier =" + display);

	double interestRate = asset.getInterestRate();

	BigDecimal rate = new BigDecimal(String.valueOf(interestRate));
	rate = rate.divide(new BigDecimal(100));
	rate = rate.add(BigDecimal.ONE);

	Date issueDate = asset.getIssueDate();
	DateTime d1 = new DateTime(issueDate);
	DateTime d2 = new DateTime();
	int seconds = Math.abs(Seconds.secondsBetween(d1, d2).getSeconds());

	//System.out.println("...Number of seconds difference: " + seconds);

	BigDecimal elapsedSeconds = new BigDecimal(seconds);
	BigDecimal elapsedYears = elapsedSeconds.divide(new BigDecimal(COINSPARK_SECONDS_IN_YEAR), MathContext.DECIMAL32);
	//System.out.println("...Number of years difference: " + elapsedYears.toPlainString());

	double base = elapsedSeconds.doubleValue();
	double exp = elapsedYears.doubleValue();
	//System.out.println("...base=" + base + "  exponent=" + exp);
	double interestMultipler = Math.pow(rate.doubleValue(), elapsedYears.doubleValue());

	//System.out.println("interest multipler =" + interestMultipler);

	result = result.divide(new BigDecimal(String.valueOf(interestMultipler)), MathContext.DECIMAL32);

	//System.out.println("result = " + result.toPlainString());

	result = result.setScale(0, RoundingMode.DOWN);
	result = result.stripTrailingZeros();

	//System.out.println("result floored = " + result.toPlainString());

	String resultString = result.toPlainString();
	return new BigInteger(resultString);
    }
    
    @Deprecated
    public static BigInteger calcTotalRawCharge(CSAsset asset, BigInteger numRawUnits) {
	BigInteger interestCharge = calcRawPercentageCharge(asset, numRawUnits);
	BigInteger flatCharge = calcRawFlatCharge(asset);
	System.out.println(">>>>> raw flat charge = " + flatCharge);
	return interestCharge.add(flatCharge);
    }
    

    // Charge basis points must be between 0 and 2.50%, with two significant digits.
    // TODO: Confirm rounding strategy.
    // Currently Rounds up if equidistant, else rounds to nearest neighbor.
    @Deprecated
    public static BigInteger calcRawPercentageCharge(CSAsset asset, BigInteger transferAmount) {
	CoinSparkGenesis genesis = asset.getGenesis();
	int chargeBasisPoints = genesis.getChargeBasisPoints();
	if (chargeBasisPoints == 0) return BigInteger.ZERO;
	BigDecimal d = new BigDecimal(chargeBasisPoints);
	d = d.movePointLeft(2 + 2); // 2 to get whole percent number, another 2 to get fraction
	d = d.multiply(new BigDecimal(transferAmount), MathContext.UNLIMITED);
	return d.toBigInteger();
    }
    
    	    /*
	     the chargeFlatMantissa and chargeFlatExponent fields are combined according to the formula chargeFlatMantissa*pow(10, chargeFlatExponent) to create a final flat charge between 0 and 5,000 asset units with 2 significant digits of accuracy.
	    */
    @Deprecated
    public static BigInteger calcRawFlatCharge(CSAsset asset) {
	CoinSparkGenesis genesis = asset.getGenesis();
	short chargeExponent = genesis.getChargeFlatExponent();
	short chargeMantissa = genesis.getChargeFlatMantissa();
	if (0==chargeMantissa) return BigInteger.ZERO;
	BigInteger n = new BigInteger("10").pow(chargeExponent);
	n = n.multiply(new BigInteger(String.valueOf(chargeMantissa)));
	return n;
    }
    

    public static String getHumanReadableAssetState(CSAsset.CSAssetState state) {
	if (state==null) return "";
	switch(state) {
	    case INVALID:
		return "Invalid"; // for some reason.";
	    case NO_KEY:
		return "Asset reference and genesis information are missing";//Neither AssetRef nor genesis is set";
	    case ASSET_REF_ONLY:
		return "Locating new asset";
	    case BLOCK_NOT_FOUND:
		return "Genesis block not found";
	    case TX_NOT_FOUND:
		return "Asset reference is not valid, genesis transaction cannot be found";
	    case GENESIS_NOT_FOUND:
		return "Asset reference is valid, but it does not identify a genesis transaction";
	    case NOT_VALIDATED_YET:
		return "Genesis transaction has not been validated yet";
	    case ASSET_WEB_PAGE_NOT_FOUND:
		return "Asset web page not found";
	    case ASSET_SPECS_NOT_FOUND:
		return "Asset web page not valid";
	    case ASSET_SPECS_NOT_PARSED:
		return "Asset JSON specs cannot be parsed";
	    case REQUIRED_FIELD_MISSING:
		return "Some required fields are missing from Asset JSON specs";
	    case CONTRACT_NOT_FOUND:
		return "Contract not found";
	    case CONTRACT_INVALID:
		return "Contract file is invalid";
	    case HASH_MISMATCH:
		return "Asset changed since it was issued";
	    case REFRESH:
		return "Refreshing..."; // Validity should be checked (manually set), if genesis is set - starts from Asset web page
	    case VALID:
		return "Valid"; // Asset passed validity check
	    case DUPLICATE:
		return "Duplicate asset detected"; // There is Asset with the same keys, this one should be deleted after balances db updated
	}
	return "Unknown State";
    }
    
    public static String getHumanReadableAssetRef(CSAsset asset) {
	CoinSparkAssetRef assetRef = asset.getAssetReference();	
	if (assetRef == null) return null;
	return assetRef.encode(); // if null, it failed
    }
    
    // Return number of satoshis required to migrate wallet
    // Note: This shouldn't matter if there are no assets.
    // Return null if there is no migration fee necessary, i.e. you have no balance of assets.
    public static BigInteger calcMigrationFeeSatoshis(BitcoinController bitcoinController, Wallet wallet) {
	BigInteger migrationFee = null;
	Address sendAddressObject;
        try {
	    /* We allow calculation of the migration fee even if there is no send address specified,
	    so we could generate an address from utxo, or just use a dummy address.
	    TODO: Get transc
	    */
	    String address = null;
	    NetworkParameters params = bitcoinController.getModel().getNetworkParameters();
	    if (params.getId().equals(NetworkParameters.ID_MAINNET)) {
		address = "1LLBoY7gp4B9WUtBdgJQLyNVS7doskFQcn"; //production
	    } else {
		address = "mzc55AvWnAmk48UfN74ktw4pxkiKfscNgU"; //testnet3
	    }

	    // TODO: Instead of this dummy address, use a parameter
            sendAddressObject = new Address(bitcoinController.getModel().getNetworkParameters(), address);
            SendRequest sendRequest = SendRequest.emptyWallet(sendAddressObject);
            sendRequest.ensureMinRequiredFee = true;
            sendRequest.fee = BigInteger.ZERO;
            wallet.completeTx(sendRequest, false);
	    migrationFee = sendRequest.fee;
        } catch (Exception ex) {
        }
	return migrationFee;
    }
    
    
    public static boolean canSafelySpendWhileRespectingMigrationFee(BitcoinController bitcoinController, Wallet wallet, BigInteger amountSatoshis) {
	BigInteger migrationFee = calcMigrationFeeSatoshis(bitcoinController, wallet);
	if (migrationFee==null) return false;
	
	BigInteger availableBalance = wallet.getBalance(Wallet.BalanceType.AVAILABLE);
	BigInteger spendingLimit = availableBalance.subtract(migrationFee);
	
//	System.out.println(">>>> Available     = "+availableBalance);
//	System.out.println(">>>> Migration fee = "+migrationFee);
//	System.out.println(">>>> Spending limit= "+spendingLimit);	
//	System.out.println(">>>> Spend amount  = "+amountSatoshis);
	
	return (amountSatoshis.compareTo(spendingLimit) <= 0);
    }

    
    public static String getDescriptionOfTransactionAssetChanges(Wallet wallet, Transaction tx) {
	if (wallet==null || tx==null) return "";
	
	Map<Integer, BigInteger> receiveMap = wallet.CS.getAssetsSentToMe(tx);
	Map<Integer, BigInteger> sendMap = wallet.CS.getAssetsSentFromMe(tx);

//	System.out.println(">>>> tx = " + tx.getHashAsString());
//	System.out.println(">>>>     receive map = " +  receiveMap);
//	System.out.println(">>>>     send map = " +  sendMap);
	
	//Map<String, String> nameAmountMap = new TreeMap<>();
	ArrayList<String> nameAmounts = new ArrayList<>();
	
	boolean isSentByMe = tx.sent(wallet);
	Map<Integer, BigInteger> loopMap = (isSentByMe) ? sendMap : receiveMap;
	
//	Integer assetID = null;
	BigInteger netAmount = null;
	
//	for (Map.Entry<Integer, BigInteger> entry : loopMap.entrySet()) {
	for (Integer assetID : loopMap.keySet()) {
//	    assetID = entry.getKey();
	    if (assetID == null || assetID == 0) continue; // skip bitcoin

	    BigInteger receivedAmount = receiveMap.get(assetID); // should be number of raw units
	    BigInteger sentAmount = sendMap.get(assetID);
	    boolean isReceivedAmountMissing = (receivedAmount==null);
	    boolean isSentAmountMissing = (sentAmount==null);
	    
	    netAmount = BigInteger.ZERO;
	    if (!isReceivedAmountMissing) netAmount = netAmount.add(receivedAmount);
	    if (!isSentAmountMissing) netAmount = netAmount.subtract(sentAmount);
	    
	    if (isSentByMe && !isSentAmountMissing && sentAmount.equals(BigInteger.ZERO)) {
		// Catch a case where for a send transaction, the send amount for an asset is 0,
		// but the receive cmount is not 0.  Also the asset was not valid.
		continue;
	    }
	    
	    
	    CSAsset asset = wallet.CS.getAsset(assetID);
	    if (asset==null) {
		// something went wrong, we have asset id but no asset, probably deleted.
		// For now, we carry on, and we display what we know.
	    }	    
	    
	    if (netAmount.equals(BigInteger.ZERO) && isSentByMe) {
		// If net asset is 0 and this is our send transaction,
		// we don't need to show anything, as this probably due to implicit transfer.
		// So continue the loop.
		continue;
	    }
	    
	    if (netAmount.equals(BigInteger.ZERO) && !isSentByMe) {
		// Receiving an asset, where the value is 0 because its not confirmed yet,
		// or not known because asset files not uploaded so we dont know display format.
		// Anyway, we don't do anything here as we do want to display this incoming
		// transaction the best we can.
	    }
	    
//	    System.out.println(">>>>     isSentAmountMissing = " + isSentAmountMissing);
//	    System.out.println(">>>>     asset reference = " + asset.getAssetReference());
//	    System.out.println(">>>>     asset name = " + asset.getName());
	    
	    String name = null;
	    CoinSparkGenesis genesis = null;
	    boolean isUnknown = false;
	    if (asset!=null) {
		genesis = asset.getGenesis();
		name = asset.getNameShort(); // could return null?
	    }
	    if (name == null) {
		isUnknown = true;
		if (genesis!=null) {
		    name = "Asset from " + genesis.getDomainName();
		} else {
		    // No genesis block found yet
		    name = "Other Asset";
		}
	    }
	    
	    String s1 = null;
	    if (asset == null ||
		isUnknown==true ||
		(netAmount.equals(BigInteger.ZERO) && !isSentByMe)
		    ) {
		// We don't have formatting details since asset is unknown or deleted
		// If there is a quantity, we don't display it since we don't have display format info
		// Of if incoming asset transfer, unconfirmed, it will be zero, so show ... instead
		s1 = "...";
	    } else {
		BigDecimal displayUnits = getDisplayUnitsForRawUnits(asset, netAmount);
		s1 = CSMiscUtils.getFormattedDisplayString(asset, displayUnits);
	    }
	    String s2 = name + ": " + s1;
	    nameAmounts.add(s2);
	    //break; // TODO: return the first asset we find, in future return map<Integer,BigInteger>
	}
	
	if (!nameAmounts.isEmpty()) {
	    Collections.sort(nameAmounts);
	}
	BigInteger satoshiAmount = receiveMap.get(0);
	satoshiAmount = satoshiAmount.subtract(sendMap.get(0));
	String btcAmount = Utils.bitcoinValueToFriendlyString(satoshiAmount);
	nameAmounts.add("BTC: " + btcAmount);
	
	String result = StringUtils.join(nameAmounts, ", ");
	
//	System.out.println(">>>>     result = " +  result);

	return result;
    }
    
    public static void updateForegroundColorOfAmountChangesLabel(MultiBitLabel label) {
	String s = label.getText();
	if (s==null) return;
	String[] a = StringUtils.split(s, ",");
	for (String entry : a) {
	    entry = entry.trim();
	    if (StringUtils.countMatches(entry, ":")==1) {
		if (entry.startsWith("BTC: -")) {
		    label.setForeground(ColorAndFontConstants.DEBIT_FOREGROUND_COLOR);
		    return;
		} else if (entry.startsWith("BTC:")) {
		    label.setForeground(ColorAndFontConstants.CREDIT_FOREGROUND_COLOR);
		    return;
		}
	    }
	}
    }
//	if (a.length >= 1) {
//	    String[] b = StringUtils.split(a[0], ":");
//
//	    if (b.length >= 2) {
//		String c = b[b.length - 1]; // in case short name has :, get last one.
//
//		try {
//		    double d = Double.parseDouble(c);
//		    if (d >= 0) {
//			label.setForeground(ColorAndFontConstants.CREDIT_FOREGROUND_COLOR);
//		    } else {
//			label.setForeground(ColorAndFontConstants.DEBIT_FOREGROUND_COLOR);
//		    }
//		} catch (NumberFormatException nfe) {
//		    // not a number, don't change color
//		}
//	    }
//	}
//    }

    
    public static String getDomainHost(String url) {
	String domain = null;
	try {
	    if (url != null) {
		URI uri = new URI(url);
		domain = uri.getHost();
		if (domain.startsWith("www.")) {
		    domain = domain.substring(4);
		}
	    }
	} catch (URISyntaxException e) {
	}
	return domain;
    }
    
    public static int getNumberOfDisplayDecimalPlaces(Wallet wallet, int assetID) {
	if (assetID == 0) return 0;
	if (wallet == null) return 0;
	return getNumberOfDisplayDecimalPlaces( wallet.CS.getAsset(assetID) );
    }
    public static int getNumberOfDisplayDecimalPlaces(CSAsset asset) {
	if (asset==null) return 0;
	double d = asset.getMultiple();
	if (d == 0.0) return 0;
	d = Math.log10(d);
	d = -d; // negate
	if (d<0.0) {
	    d = Math.floor(d);
	} else if (d>0.0) {
	    d = Math.ceil(d);
	}
	int result = new Double(d).intValue();
	return result;
    }
    
    public static int getNumberOfDecimalPlaces(BigDecimal bigDecimal) {
	String string = bigDecimal.stripTrailingZeros().toPlainString();
	int index = string.indexOf(".");
	return index < 0 ? 0 : string.length() - index - 1;
    }
    
    public static String prettyFormatDate(Date d) {
	if (d == null) return "";
	LocalDateTime dt = new DateTime(d).toLocalDateTime();
	return dt.toString("d MMM y, HH:mm:ss z");
    }
}
