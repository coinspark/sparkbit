/* 
 * SparkBit
 *
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.table.AbstractTableModel;

import org.joda.money.Money;
import org.multibit.controller.Controller;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.exchange.CurrencyConverter;
//import org.multibit.exchange.CurrencyInfo;
import org.multibit.model.bitcoin.WalletAssetTableData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Wallet;
import java.awt.Font;
import org.coinspark.wallet.CSAsset;
import org.coinspark.wallet.CSAssetDatabase;
//import org.coinspark.wallet.CSBalance;
//import org.coinspark.wallet.CSBalanceDatabase;
import java.util.Arrays;
import java.util.HashMap;
import org.joda.time.DateTime;
import org.multibit.utils.CSMiscUtils;
import org.joda.time.*;

import java.util.concurrent.CopyOnWriteArrayList;
import org.coinspark.protocol.CoinSparkGenesis;
import org.coinspark.wallet.CSEvent;
import org.coinspark.wallet.CSEventBus;
import org.coinspark.wallet.CSEventType;
import org.multibit.viewsystem.swing.view.components.FontSizer;

public class WalletAssetTableModel extends AbstractTableModel {

        /**
     * Keys that give column header text for output formatting.
     * TODO Exposing mutable state consider cloning
     */
    public static final String[] COLUMN_HEADER_KEYS = new String[] {
//	    "walletAssetTableColumn.assetID",
	    "walletAssetTableColumn.visibility",
	    "walletAssetTableColumn.name",
	    "walletAssetTableColumn.issuer",
	    "walletAssetTableColumn.quantity",
	    "walletAssetTableColumn.assetRef",
	    "walletAssetTableColumn.state",
//            "walletAssetTableColumn.statusText",
//            "walletAssetTableColumn.dateText",

//            "walletAssetTableColumn.issueDate",
//            "walletAssetTableColumn.contractUrl"
    };
    
    private static final long serialVersionUID = -107886012854496208L;

    private static final Logger log = LoggerFactory.getLogger(WalletAssetTableModel.class);

    protected ArrayList<String> headers;

    protected CopyOnWriteArrayList<WalletAssetTableData> walletAssetData;

    protected final Controller controller;
    protected final BitcoinController bitcoinController;
    
    protected boolean hideInvisible = false; // if true, don't show assets which are invisible

    public WalletAssetTableModel(BitcoinController bitcoinController) {
        this.bitcoinController = bitcoinController;
        this.controller = this.bitcoinController;

        createHeaders();

        //walletAssetData = this.bitcoinController.getModel().createWalletAssetTableData(this.bitcoinController, this.bitcoinController.getModel().getActiveWalletFilename());
        
        //walletAssetData = new ArrayList<WalletAssetTableData>();
        
        String s = this.bitcoinController.getModel().getActiveWalletFilename();
        createAssetDataForActiveWallet();
    }
    
    protected void createAssetDataForActiveWallet() {
           // Create collection of CSAsset
     walletAssetData = new CopyOnWriteArrayList<WalletAssetTableData>();
	Wallet wallet = this.bitcoinController.getModel().getActiveWallet();
	if (wallet != null) {
	    int[] array_ids = wallet.CS.getAssetIDs();
	    if (array_ids != null) {
		for (int i : array_ids) {
		    CSAsset asset = wallet.CS.getAsset(i);
		    if (asset == null) continue; 
		    if (this.hideInvisible==true && !asset.isVisible()) continue; // don't show if invisible
		    WalletAssetTableData w = new WalletAssetTableData(asset);
		    walletAssetData.add(w);
		}
	    }
	}
    }
    
//    @Override
//    public Class<?> getColumnClass(int columnIndex) {
//        if (columnIndex == 3 || columnIndex == 4) {
//            return Number.class;
//        } else {
//            return super.getColumnClass(columnIndex);
//        }
//    }

    @Override
    public int getColumnCount() {
        return headers.size();
    }

    @Override
    public int getRowCount() {
        return walletAssetData.size();
    }

    public WalletAssetTableData getRow(int row) {
        return walletAssetData.get(row);
    }
    
    /*
    @param int AssetID
    @return the model row index for the asset ,or -1 if the asset is not found
    */
    public int getRowForAssetID(int assetID) {
	int row = 0;
	for (WalletAssetTableData data : walletAssetData) {
	    CSAsset asset = data.getAsset();
	    if (asset!=null) {
		if (assetID == asset.getAssetID())
		    return row;
	    }
	    row++;
	}
	return -1;
    }
    
    /*
    @param int model row index
    @return int the assetID of the asset at that model row index.
    */
    public int getAssetIDAtRow(int row) {
	if (row < 0) return -1;
	WalletAssetTableData data = walletAssetData.get(row);
	return data.getAsset().getAssetID();
    }
    
     /*
    @param int model row index
    @return CSAsset the asset at that model row index.
    */
    public CSAsset getCSAssetAtRow(int row) {
	if (row < 0) return null;
	WalletAssetTableData data = walletAssetData.get(row);
	return data.getAsset();
    }   

    @Override
    public String getColumnName(int column) {
        return headers.get(column);
    }
    
    // Find model index for a column
    public int getColumnIndex(String key) {
	return Arrays.asList(COLUMN_HEADER_KEYS).indexOf("walletAssetTableColumn." + key);
    }
    

    @Override
    public boolean isCellEditable(int row, int col) {
	if (col == getColumnIndex("visibility")) return true;
	return false;
    }
    

    @Override
    public Object getValueAt(int row, int column) {
        WalletAssetTableData rowObj = null;
        if (row >= 0 && row < walletAssetData.size()) {
            rowObj = walletAssetData.get(row);
        }
        if (rowObj == null) {
            return null;
        }

        CSAsset asset = rowObj.getAsset();
	
	// Switch on string, makes it easier for us to re-order columns without
	// having to reorganize the case statements.
	// http://stackoverflow.com/questions/338206/switch-statement-with-strings-in-java
        switch (COLUMN_HEADER_KEYS[column]) {
        case "walletAssetTableColumn.assetID": {
	    // can return null, date object
            return asset.getAssetID();
        }
	case "walletAssetTableColumn.visibility" : {
	    return new Boolean( asset.isVisible() );
	}
	case "walletAssetTableColumn.state":
	{
	    CSAsset.CSAssetState state = asset.getAssetState();
	    // TODO: Special case where the asset is valid, but we asset reference is nul.
	    if (state==CSAsset.CSAssetState.VALID && asset.getAssetReference() == null) {
		return "Awaiting new asset confirmation...";
	    }
	    return CSMiscUtils.getHumanReadableAssetState(asset.getAssetState());
	}
        case "walletAssetTableColumn.assetRef":
	{
	    if (asset.getAssetReference() == null) {
		return "Awaiting new asset confirmation...";
	    }
	    return CSMiscUtils.getHumanReadableAssetRef(asset);
	}
	case "walletAssetTableColumn.quantity":
	{
	    if (asset.getName() == null) {
		return "...";
	    }
	    Wallet wallet = this.bitcoinController.getModel().getActiveWallet();
	    int assetID = asset.getAssetID();
	    Wallet.CoinSpark.AssetBalance assetBalance = wallet.CS.getAssetBalance(assetID);
	    BigInteger x =  assetBalance.total; //wallet.CS.getUnspentAssetQuantity(assetID);
	    String display = CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, x);
	    if (assetBalance.updatingNow) {
		if (x.intValue()==0) {
		    display = "...";
		} else {
		    display += " + ...";
		}
	    }
	    return display;
	}
	case "walletAssetTableColumn.name":
	{
	    HashMap map = new HashMap<>();
	    String name = asset.getNameShort();
	    Font f = null;
	    if (name == null) {
		CoinSparkGenesis genesis = asset.getGenesis();
		if (genesis != null) {
		    name = genesis.getPagePath();
		}
		if (name != null) {
		    name = "/" + name + "/";
		} else {
		    name = "Unknown Name";
		}
		//name = asset.getAssetWebPageURL();
		f = new Font(null, Font.ITALIC, 12);
	    } else {
		f = FontSizer.INSTANCE.getAdjustedDefaultFont();
	    }
	    map.put("label", name);
	    map.put("font", f);
	    map.put("truncatedTooltip", Boolean.TRUE);
	    return map;
	}
        case "walletAssetTableColumn.issuer":
	{
	    HashMap map = new HashMap<>();
	    Font f = null;
	    String issuer = asset.getIssuer();
	    if (issuer == null) {
		issuer = CSMiscUtils.getDomainHost(asset.getDomainURL());
		f = new Font(null, Font.ITALIC, 12);
	    } else {
		f = FontSizer.INSTANCE.getAdjustedDefaultFont();
	    }
	    map.put("label", issuer);
	    map.put("font", f);
	    map.put("truncatedTooltip", true);
	    return map;
	}
	case "walletAssetTableColumn.issueDate":
	{
	    Date d = asset.getIssueDate();
	    if (d==null) return "";
	    
	    LocalDateTime dt = new DateTime(d).toLocalDateTime();
	    return dt.toString("d MMM y");
	}
        case "walletAssetTableColumn.contractUrl":
            return asset.getContractUrl();
        default:
            return null;
//            return rowObj.getDescription();
//        case 3:
//            // Amount in BTC
//            BigInteger debitAmount = rowObj.getDebit();
//            if (debitAmount != null && debitAmount.compareTo(BigInteger.ZERO) > 0) {
//                return controller.getLocaliser().bitcoinValueToString(debitAmount.negate(), false, true);
//            }
//
//            BigInteger creditAmount = rowObj.getCredit();
//            if (creditAmount != null) {
//                return controller.getLocaliser().bitcoinValueToString(creditAmount, false, true);
//            }
//            
//            return null;         
//        case 4:
//            // Amount in fiat
//            if (rowObj.getDebit() != null  && rowObj.getDebit().compareTo(BigInteger.ZERO) > 0) {
//                Money debitAmountFiat = CurrencyConverter.INSTANCE.convertFromBTCToFiat(rowObj.getDebit());
//                if (debitAmountFiat != null) {
//                    return CurrencyConverter.INSTANCE.getFiatAsLocalisedString(debitAmountFiat.negated(), false, false);
//                }
//            }
//
//            Money creditAmountFiat = CurrencyConverter.INSTANCE.convertFromBTCToFiat(rowObj.getCredit());
//            if (creditAmountFiat != null) {
//                return CurrencyConverter.INSTANCE.getFiatAsLocalisedString(creditAmountFiat, false, false);
//            }
//            
//            return "";
        }
    }

    /**
     * Table model is read only.
     */
    @Override
    public void setValueAt(Object value, int row, int column) {
	WalletAssetTableData rowObj = null;
        if (row >= 0 && row < walletAssetData.size()) {
            rowObj = walletAssetData.get(row);
        }
        if (rowObj != null) {
            CSAsset asset = rowObj.getAsset();
	
	    if (column == getColumnIndex("visibility")) {
		Wallet wallet = this.bitcoinController.getModel().getActiveWallet();
		CSAssetDatabase assetDB = wallet.CS.getAssetDB();
		
		boolean b = ((Boolean)value).booleanValue();
		assetDB.setAssetVisibility(asset, b);
		
		CSEventBus.INSTANCE.postAsyncEvent(CSEventType.ASSET_VISIBILITY_CHANGED, asset.getAssetID());
		return;
	    }
	}
        throw new UnsupportedOperationException();
    }

    public void recreateWalletData() {
        log.info("recreateWalletData()");
        createAssetDataForActiveWallet();
        
        // Recreate the wallet data as the underlying wallet has changed.
        //walletAssetData = this.bitcoinController.getModel().createActiveWalletData(this.bitcoinController);
        // TODO: Refactor the creation of the dadta.
        fireTableDataChanged();
    }
    
    public void createHeaders() {
        headers = new ArrayList<String>();
        for (int j = 0; j < COLUMN_HEADER_KEYS.length; j++) {
//            if ("sendBitcoinPanel.amountLabel".equals(WalletAssetTableData.COLUMN_HEADER_KEYS[j])) {
//                String header = controller.getLocaliser().getString(WalletAssetTableData.COLUMN_HEADER_KEYS[j]) + " (" + controller.getLocaliser().getString("sendBitcoinPanel.amountUnitLabel") + ")";
//                headers.add(header);
//            } else {
            headers.add(controller.getLocaliser().getString(COLUMN_HEADER_KEYS[j]));
//            } 
        }
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
