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

import com.google.bitcoin.core.Wallet;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.swing.ImageIcon;

import javax.swing.table.AbstractTableModel;
import org.coinspark.wallet.CSMessage;
import org.coinspark.wallet.CSMessagePart;
import org.multibit.controller.Controller;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.model.bitcoin.WalletTableData;
import org.multibit.utils.CSMiscUtils;
import org.multibit.utils.ImageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sparkbit.CompoundIcon;



public class WalletTableModel extends AbstractTableModel {

    public static final String[] COLUMN_HEADER_KEYS = new String[]{
	"walletTransactionTableColumn.status",
	"walletTransactionTableColumn.date",
	"walletTransactionTableColumn.description",
//	"walletTransactionTableColumn.btcAmount",
//	"walletTransactionTableColumn.type",
//	"walletTransactionTableColumn.domain",
//	"walletTransactionTableColumn.assetAmount",
	"walletTransactionTableColumn.descriptionOfAssetChanges",
	"walletTransactionTableColumn.extras",
	"walletTransactionTableColumn.message",
//	"walletTransactionTableColumn.attachments",
	};

    private static final long serialVersionUID = -937886012854496208L;

    private static final Logger log = LoggerFactory.getLogger(WalletTableModel.class);

    private ArrayList<String> headers;

    private ArrayList<WalletTableData> walletData;

    private final Controller controller;
    private final BitcoinController bitcoinController;

    public WalletTableModel(BitcoinController bitcoinController) {
        this.bitcoinController = bitcoinController;
        this.controller = this.bitcoinController;

        createHeaders();

        walletData = this.bitcoinController.getModel().createWalletTableData(this.bitcoinController, this.bitcoinController.getModel().getActiveWalletFilename());
    }
    
    // Find model index for a column
    public int getColumnIndex(String key) {
	return Arrays.asList(COLUMN_HEADER_KEYS).indexOf("walletTransactionTableColumn." + key);
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == getColumnIndex("btcAmount") || columnIndex == getColumnIndex("assetAmount")) {
            return Number.class;
        } else {
            return super.getColumnClass(columnIndex);
        }
    }

    @Override
    public int getColumnCount() {
        return headers.size();
    }

    @Override
    public int getRowCount() {
        return walletData.size();
    }

    public WalletTableData getRow(int row) {
        return walletData.get(row);
    }

    @Override
    public String getColumnName(int column) {
        return headers.get(column);
    }

    @Override
    public Object getValueAt(int row, int column) {
        WalletTableData walletDataRow = null;
        if (row >= 0 && row < walletData.size()) {
            walletDataRow = walletData.get(row);
        }
        if (walletDataRow == null) {
            return null;
        }

	String name = COLUMN_HEADER_KEYS[column];
        switch (name) {
	    case "walletTransactionTableColumn.attachments": {
		HashMap<String, Object> map = new HashMap<>();
		Object icon = null; // could be ImageIcon or CompoundIcon
		String tip = null;
		if (walletDataRow.getTransaction() != null) {
		    String txid = walletDataRow.getTransaction().getHashAsString();
		    Wallet w = this.bitcoinController.getModel().getActiveWallet();
		    CSMessage message = w.CS.getMessageDB().getMessage(txid);
		    if (message != null) {

			int state = message.getMessageState();
			if (state == CSMessage.CSMessageState.PAYMENTREF_ONLY) {
			} else if (state == CSMessage.CSMessageState.SELF
				|| state == CSMessage.CSMessageState.VALID) {

			    List<CSMessagePart> parts = message.getMessagePartsSortedByPartID();
			    int n = parts.size();
			    ImageIcon attachIcon = ImageLoader.fatCow16(ImageLoader.FATCOW.attach);
			    if (n == 1) {
				icon = attachIcon;
				tip = "1 attachment";
			    } else if (n > 1) {
				String counterName = null;
				if (n > 20) {
				    counterName = "notification_counter_20_plus";
				} else {
				    counterName = String.format("notification_counter_%02d", n);
				}
				ImageIcon counterIcon = ImageLoader.fugue(counterName);
				CompoundIcon badgeIcon = new CompoundIcon(CompoundIcon.Axis.X_AXIS, -10, CompoundIcon.RIGHT, CompoundIcon.BOTTOM, attachIcon, counterIcon);
				icon = badgeIcon;
				tip = n + " attachments";
			    }

			}
			else {
//			    switch (state) {
//				case CSMessage.CSMessageState.NEVERCHECKED:
//				    lastChecked = new Date();
//				    failures = 0;
//				    break;
//	    case CSMessage.CSMessageState.NOT_FOUND:
//	    case CSMessage.CSMessageState.PENDING:
//	    case CSMessage.CSMessageState.EXPIRED:
//	    case CSMessage.CSMessageState.INVALID:
//	    case CSMessage.CSMessageState.HASH_MISMATCH:
//	    case CSMessage.CSMessageState.SERVER_NOT_RESPONDING:
//	    case CSMessage.CSMessageState.SERVER_ERROR:
//	    case CSMessage.CSMessageState.ENCRYPTED_KEY:
//		lastChecked = new Date();
//		failures++;
//		break;
//	    case CSMessage.CSMessageState.SELF:
//	    case CSMessage.CSMessageState.VALID:
//	    case CSMessage.CSMessageState.PAYMENTREF_ONLY:
//		lastChecked = new Date();
//		failures = 0;
//		break;
//	    case CSMessage.CSMessageState.REFRESH:
//	    case CSMessage.CSMessageState.DELETED:
//		lastChecked = null;
//		failures = 0;
//		break;
			    //if (state==CSMessage.CSMessageState.EXPIRED) {
			    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.server_error);
			    tip = "Problem!";
			}
		    }
//
//		    String shortMessage = CSMiscUtils.getShortTextMessage(w, txid);
//		    if (shortMessage != null) {
//			icon = ImageLoader.fugue(ImageLoader.FUGUE.balloon_white);
//				//fatCow16(ImageLoader.FATCOW.email);
//		    }

		}
		map.put("tooltip", tip);
		map.put("icon", icon);
		return map;
	    }
	    case "walletTransactionTableColumn.message": {
		HashMap<String, Object> map = new HashMap<>();
		ImageIcon icon = null;
		String tip = null;
		if (walletDataRow.getTransaction() != null) {
		    String txid = walletDataRow.getTransaction().getHashAsString();
		    Wallet w = this.bitcoinController.getModel().getActiveWallet();
		    CSMessage message = w.CS.getMessageDB().getMessage(txid);
		    if (txid!=null && message!=null) {
		    int state = message.getMessageState();
		    if (state == CSMessage.CSMessageState.PAYMENTREF_ONLY) {
			// Do nothing, it's a payment ref only.
		    }
		    else if (state == CSMessage.CSMessageState.SELF || state == CSMessage.CSMessageState.VALID)
		    {

			String shortMessage = CSMiscUtils.getShortTextMessage(w, txid);
			if (shortMessage != null) {
			    icon = ImageLoader.fugue(ImageLoader.FUGUE.balloon_white);

			// Tooltip is a short preview.  We have to show newlines properly,
			    // so we convert to HTML.
			    final int limit = 140 * 2; // 2 tweets worth...
			    tip = shortMessage;
			    if (shortMessage.length() > limit) {
				tip = shortMessage.substring(0, limit);
			    }
			    tip = tip.replace("\n\n", "<p>").replace("\n", "<br>");
			    tip = "<html>" + tip + "</html>";
			}
		    }
		    else {
			// Something else has happened, so let's check and show
			// the appropriate icon.
			switch (state) {
			    case CSMessage.CSMessageState.NEVERCHECKED:
				tip = "Never checked";
				icon = ImageLoader.fatCow16(ImageLoader.FATCOW.hourglass);
				break;
			    case CSMessage.CSMessageState.NOT_FOUND:
				tip = "Not found";
				icon = ImageLoader.fatCow16(ImageLoader.FATCOW.find);
				break;				
			    case CSMessage.CSMessageState.PENDING:
				tip = "Pending";
				icon = ImageLoader.fugue(ImageLoader.FUGUE.balloon_ellipsis);
				break;						
			    case CSMessage.CSMessageState.EXPIRED:
				tip = "Expired, cannot retrieve message";
				icon = ImageLoader.fugue(ImageLoader.FUGUE.clock__exclamation);
				break;						
			    case CSMessage.CSMessageState.INVALID:
				tip = "Message was invalid";
				icon = ImageLoader.fatCow16(ImageLoader.FATCOW.exclamation);
				break;					
			    case CSMessage.CSMessageState.HASH_MISMATCH:
				tip = "Error, message could not be authenticated, may have been tampered with.";
				icon = ImageLoader.fugue(ImageLoader.FUGUE.traffic_cone__exclamation);
				break;	
			    case CSMessage.CSMessageState.SERVER_NOT_RESPONDING:
				tip = "Server not responding";
				icon = ImageLoader.fatCow16(ImageLoader.FATCOW.server_connect);
				break;	
			    case CSMessage.CSMessageState.SERVER_ERROR:
				tip = "Server returned an error";
				icon = ImageLoader.fatCow16(ImageLoader.FATCOW.server_error);
				break;	
			    case CSMessage.CSMessageState.ENCRYPTED_KEY:
				tip = "Cannot open encrypted message";
				icon = ImageLoader.fatCow16(ImageLoader.FATCOW.lock);
				break;
//			    case CSMessage.CSMessageState.SELF:
//			    case CSMessage.CSMessageState.VALID:
//			    case CSMessage.CSMessageState.PAYMENTREF_ONLY:
//				break;
			    case CSMessage.CSMessageState.REFRESH:
				tip = "Waiting to refresh";
				icon = ImageLoader.fatCow16(ImageLoader.FATCOW.arrow_refresh);
				break;	
			    case CSMessage.CSMessageState.DELETED:
				tip = "Error, message was deleted";
				icon = ImageLoader.fatCow16(ImageLoader.FATCOW.delete);
				break;
			    default:
				break;
			}
		    }
		    }
		}

		map.put("tooltip", tip);
		map.put("icon", icon);
		return map;
	    }
	    case "walletTransactionTableColumn.extras": {
		HashMap<String, Object> map = new HashMap<>();
		ImageIcon icon = null;
		String tip = null;
		if (walletDataRow.getTransaction() != null) {
		    String txid = walletDataRow.getTransaction().getHashAsString();
		    Wallet w = this.bitcoinController.getModel().getActiveWallet();
		    long l = CSMiscUtils.getPaymentRefFromTx(w, txid);
		    if (l>0) {
			icon = ImageLoader.fugue(ImageLoader.FUGUE.tag_hash); //edit_number);
			tip = "Transaction payment reference: " + l;
		    }
		}
		map.put("tooltip", tip);
		map.put("icon", icon);
		return map;
	    }
        case "walletTransactionTableColumn.status": {
            if (walletDataRow.getTransaction() != null && walletDataRow.getTransaction().getConfidence() != null) {
                return walletDataRow.getTransaction();
            } else {
                return null;
            }
        }
        case "walletTransactionTableColumn.date": {
            if (walletDataRow.getDate() == null) {
                return new Date(0); // the earliest date (for sorting)
            } else {
                return walletDataRow.getDate();
            }
        }
        case "walletTransactionTableColumn.description":
            return walletDataRow.getDescription();
        case "walletTransactionTableColumn.btcAmount":
            // Amount in BTC
            BigInteger debitAmount = walletDataRow.getDebit();
            if (debitAmount != null && debitAmount.compareTo(BigInteger.ZERO) > 0) {
                return controller.getLocaliser().bitcoinValueToString(debitAmount.negate(), false, true);
            }

            BigInteger creditAmount = walletDataRow.getCredit();
            if (creditAmount != null) {
                return controller.getLocaliser().bitcoinValueToString(creditAmount, false, true);
            }
            
            return null;         
//        case 4:
//            // Amount in fiat
//            if (walletDataRow.getDebit() != null  && walletDataRow.getDebit().compareTo(BigInteger.ZERO) > 0) {
//                Money debitAmountFiat = CurrencyConverter.INSTANCE.convertFromBTCToFiat(walletDataRow.getDebit());
//                if (debitAmountFiat != null) {
//                    return CurrencyConverter.INSTANCE.getFiatAsLocalisedString(debitAmountFiat.negated(), false, false);
//                }
//            }
//
//            Money creditAmountFiat = CurrencyConverter.INSTANCE.convertFromBTCToFiat(walletDataRow.getCredit());
//            if (creditAmountFiat != null) {
//                return CurrencyConverter.INSTANCE.getFiatAsLocalisedString(creditAmountFiat, false, false);
//            }
//            
//            return "";

        case "walletTransactionTableColumn.type":
            if (walletDataRow.isGenesis()) {
               return "Genesis";
            } else if (walletDataRow.isTransfer()) {
                return "Transfer";
            }
            return "N/A";
        case "walletTransactionTableColumn.domain":
	{
	    Wallet wallet = this.bitcoinController.getModel().getActiveWallet();
            String s = walletDataRow.getAssetDomain(wallet);
            return s;
	}
	case "walletTransactionTableColumn.descriptionOfAssetChanges":
	{
	    Wallet wallet = this.bitcoinController.getModel().getActiveWallet();
	    return CSMiscUtils.getDescriptionOfTransactionAssetChanges(wallet, walletDataRow.getTransaction(), false, false);
	}
        case "walletTransactionTableColumn.assetAmount":
	{
	    Wallet wallet = this.bitcoinController.getModel().getActiveWallet();
	    return walletDataRow.getFormattedAssetDebitCredit(wallet);
	}
//            long x = walletDataRow.getGenesisQuantity();
//            return x;
//        case 5:
//            return walletDataRow.getAssetName();
//        case 6:
//            return walletDataRow.getAssetDomain();
//        case 7:
//            return walletDataRow.getAssetValidity();
//        case 8:
//            return walletDataRow.getAssetAge();
//        case 9:
//            return "<html><a href=\"http://"+walletDataRow.getAssetPage()+"\">Web Page</a>";
//        case 10:
//            return "<html><a href=\"http://"+walletDataRow.getAssetContract()+"\">Contract</a>";
//            //return ;
/*CoinSpark*/
            default:
            return null;
        }
    }

    /**
     * Table model is read only.
     */
    @Override
    public void setValueAt(Object value, int row, int column) {
        throw new UnsupportedOperationException();
    }

    public void recreateWalletData() {
        // Recreate the wallet data as the underlying wallet has changed.
        walletData = this.bitcoinController.getModel().createActiveWalletData(this.bitcoinController);
        fireTableDataChanged();
    }

    public void createHeaders() {
        headers = new ArrayList<String>();
        for (int j = 0; j < COLUMN_HEADER_KEYS.length; j++) {
            headers.add(controller.getLocaliser().getString( COLUMN_HEADER_KEYS[j]));
        }
		
//        for (int j = 0; j < WalletTableData.COLUMN_HEADER_KEYS.length; j++) {
//            if ("sendBitcoinPanel.amountLabel".equals(WalletTableData.COLUMN_HEADER_KEYS[j])) {
//                String header = controller.getLocaliser().getString(WalletTableData.COLUMN_HEADER_KEYS[j]) + " (" + controller.getLocaliser().getString("sendBitcoinPanel.amountUnitLabel") + ")";
//                headers.add(header);
//            } else {
//                headers.add(controller.getLocaliser().getString(WalletTableData.COLUMN_HEADER_KEYS[j]));                
//            } 
//        }
        
        // Add in the converted fiat, if appropriate
//        if (CurrencyConverter.INSTANCE.isShowingFiat()) {
//            CurrencyInfo currencyInfo = CurrencyConverter.INSTANCE.getCurrencyCodeToInfoMap().get(CurrencyConverter.INSTANCE.getCurrencyUnit().getCode());
//            String currencySymbol = CurrencyConverter.INSTANCE.getCurrencyUnit().getCode();
//            if (currencyInfo != null) {
//                currencySymbol = currencyInfo.getCurrencySymbol();
//            }
//            String header = controller.getLocaliser().getString("sendBitcoinPanel.amountLabel") + " (" + currencySymbol + ")";
//            headers.add(header);
//        }
    }
}
