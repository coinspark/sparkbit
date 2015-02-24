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

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.InventoryItem;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Wallet;
import com.jidesoft.swing.StyleRange;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import org.coinspark.wallet.CSAsset;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.model.bitcoin.WalletAssetTableData;
import org.multibit.utils.ImageLoader;

//import javax.swing.Icon;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Date;
import java.awt.Font;
import java.awt.Color;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.wallet.DefaultCoinSelector;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import org.coinspark.core.CSUtils;
import org.multibit.viewsystem.swing.CSBitcoinAssetTableData;
import java.util.concurrent.Executors;
import javax.swing.SwingUtilities;
import org.coinspark.protocol.CoinSparkAssetRef;
import org.coinspark.protocol.CoinSparkGenesis;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.*;
import static org.multibit.model.bitcoin.WalletAssetComboBoxModel.NUMBER_OF_CONFIRMATIONS_TO_SEND_ASSET_THRESHOLD;
import org.multibit.model.core.StatusEnum;
import org.multibit.utils.CSMiscUtils;
import org.multibit.viewsystem.ViewSystem;
import org.multibit.viewsystem.swing.view.components.FontSizer;
import java.util.Map;

/**
 *
 */
public class WalletAssetSummaryTableModel extends WalletAssetTableModel {

    private static final long serialVersionUID = -9886012854496208L;

    public static final String COLUMN_CONFIRMATION = "walletAssetSummaryTableColumn.confirmation";
    public static final String COLUMN_NAME = "walletAssetSummaryTableColumn.name";
    public static final String COLUMN_QUANTITY = "walletAssetSummaryTableColumn.quantity";
    public static final String COLUMN_SPENDABLE = "walletAssetSummaryTableColumn.spendable";
    public static final String COLUMN_REFRESH = "walletAssetSummaryTableColumn.refresh";
    public static final String COLUMN_DESCRIPTION = "walletAssetSummaryTableColumn.description";
    public static final String COLUMN_ISSUER = "walletAssetSummaryTableColumn.issuer";
    public static final String COLUMN_CONTRACT = "walletAssetTableColumn.contract";
    public static final String COLUMN_EXPIRY = "walletAssetSummaryTableColumn.expiry";

    public static final String[] COLUMN_HEADER_KEYS = new String[]{
	COLUMN_REFRESH,
//	COLUMN_DESCRIPTION,
//	COLUMN_ISSUER,
	COLUMN_CONTRACT,
	COLUMN_CONFIRMATION, // Tooltip: date=xyz, block number, number of confirmations,
	COLUMN_NAME, // try a panel with two buttons in grid or free flow.
	COLUMN_QUANTITY, // 4650 Units (350 to be confirm)
	COLUMN_SPENDABLE,
	// COLUMN_QUANTITY_DETAIL // (all confirmed) vs (3000 unconfirmed)
	COLUMN_EXPIRY
    };

    // Cache any blocks we have to get, so we don't repeat network requests. Don't expire.
    private ConcurrentHashMap<Integer, Block> blockCache = new ConcurrentHashMap<>();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    private MultiBitFrame multiBitFrame;

    public WalletAssetSummaryTableModel(BitcoinController bitcoinController, MultiBitFrame multiBitFrame) {
	super(bitcoinController);
	this.multiBitFrame = multiBitFrame;
	this.hideInvisible = true;
    }

    /*
     Helper to get the real column index
     */
    public static int getIndexOfColumn(String name) {
	return java.util.Arrays.asList(COLUMN_HEADER_KEYS).indexOf(name);
    }

    @Override
    public void createHeaders() {
	headers = new ArrayList<String>();
//	headers.add(controller.getLocaliser().getString("walletAssetTableColumn.assetRef"));
	for (String s : COLUMN_HEADER_KEYS) {
	    headers.add("");
	}
    }

    @Override
    protected void createAssetDataForActiveWallet() {
	super.createAssetDataForActiveWallet();

	// insert special data object for Bitcoin balance
	CSBitcoinAssetTableData btcObj = new CSBitcoinAssetTableData();
	walletAssetData.add(0, btcObj);
    }

    public CSBitcoinAssetTableData getBitcoinAssetTableData() {
	return (CSBitcoinAssetTableData) walletAssetData.get(0);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
	if (row == 0) {
	    return false; // BITCOIN ROW IS NEVER EDITABLE
	}
	if ((getIndexOfColumn(COLUMN_DESCRIPTION) == col)
		|| (getIndexOfColumn(COLUMN_EXPIRY) == col)
		|| (getIndexOfColumn(COLUMN_ISSUER) == col)
		|| (getIndexOfColumn(COLUMN_QUANTITY) == col)
		|| (getIndexOfColumn(COLUMN_SPENDABLE) == col)
		|| (getIndexOfColumn(COLUMN_CONFIRMATION) == col)) {
	    return false;
	}
	return true;
    }

    /**
     * Table model is read only, no need to throw exceptions, just do nothing...
     */
    @Override
    public void setValueAt(Object value, int row, int column) {
	//throw new UnsupportedOperationException();
    }

//    // Initially v
//    public Class getColumnClass(int c) {
//	if (c==0 || c==1) return Icon.class;
//	return String.class;
//	
////	Object o = getValueAt(0, c);
////	if (o == null) return String.class;
////        return getValueAt(0, c).getClass();
//    }
    @Override
    public Object getValueAt(int row, int column) {
	WalletAssetTableData rowObj = null;
	if (row >= 0 && row < walletAssetData.size()) {
	    rowObj = walletAssetData.get(row);
	}
	if (rowObj == null) {
	    return null;
	}

	CSBitcoinAssetTableData btcObj = null;
	boolean isBitcoin = (rowObj instanceof CSBitcoinAssetTableData);
	if (isBitcoin) {
	    btcObj = (CSBitcoinAssetTableData) rowObj;
	}

	CSAsset asset = rowObj.getAsset();
	CSAsset.CSAssetState assetState = CSAsset.CSAssetState.INVALID;
	if (asset != null) {
	    assetState = asset.getAssetState();
	}
	//boolean noDetailsFlag = assetState.
	// NOTE: If asset is null, this should be the Bitcoin object.
	
	/*
	Get the balance and spendable balance of the asset.
	*/
	boolean showAssetSpendableFlag = false;
	boolean showAssetUpdatingNowFlag = false;
	Wallet wallet = bitcoinController.getModel().getActiveWallet();
	Wallet.CoinSpark.AssetBalance assetBalance = null;
	if (asset != null) {
	    int id = asset.getAssetID();
	    assetBalance = wallet.CS.getAssetBalance(id);
	    showAssetSpendableFlag = (assetBalance.total.compareTo(assetBalance.spendable) != 0);
	    showAssetUpdatingNowFlag = assetBalance.updatingNow;
	    
	    // Fudge UI, if the send panel transaction is set - which means the send panel
	    // is visible - hide spendable numbers if transaction is not selectable.
	    if (showAssetSpendableFlag) {
		Transaction tx = multiBitFrame.sendPanelTransaction;
		if (tx!=null) {
		    showAssetSpendableFlag = DefaultCoinSelector.isSelectable(tx);
		    if (showAssetSpendableFlag) {
			// panel may still be visible but we are selectable so lets clear property
			multiBitFrame.sendPanelTransaction = null;
		    }
		}
	    }
	    
	}
	
	if (bitcoinController.getMultiBitService()==null) return null;
	PeerGroup pg = bitcoinController.getMultiBitService().getPeerGroup();
	if (pg == null) return null;
	int numConnectedPeers = pg.numConnectedPeers();
	int mostCommonChainHeight = pg.getMostCommonChainHeight();
	// mostCommonChainHeight can be 0 when launching.
	
	int threshold = NUMBER_OF_CONFIRMATIONS_TO_SEND_ASSET_THRESHOLD;
	String sendAssetWithJustOneConfirmation = controller.getModel().getUserPreference("sendAssetWithJustOneConfirmation");
	if (Boolean.TRUE.toString().equals(sendAssetWithJustOneConfirmation)) {
	    threshold = 1;
	}
	int lastHeight = 0;
	if (wallet != null) {
	    lastHeight = wallet.getLastBlockSeenHeight();
	    // can be -1 if new wallet with no previously seen block, so use common height for now
	    if (lastHeight == -1) lastHeight = mostCommonChainHeight;
	}
	int numConfirms = 0;
	CoinSparkAssetRef assetRef = null;
	if (asset != null) assetRef = asset.getAssetReference();
	if (assetRef != null) {
	    int blockIndex = (int) assetRef.getBlockNum();
	    numConfirms = mostCommonChainHeight - blockIndex + 1; // 0 means no confirmation, 1 is yes for sa
	}
	int numConfirmsStillRequired = threshold - numConfirms;
	boolean isWalletSyncing = (mostCommonChainHeight > lastHeight);

	// Switch on string, makes it easier for us to re-order columns without
	// having to reorganize the case statements.
	// http://stackoverflow.com/questions/338206/switch-statement-with-strings-in-java
	switch (COLUMN_HEADER_KEYS[column]) {
	    case COLUMN_DESCRIPTION: {
		HashMap<String, Object> map = new HashMap<>();
		if (isBitcoin) {
		    return map;
		}
		// Ƀ is the symbol industry group wants to use in 2014.
		// If there is no tooltip, no description, than dont show icon.
		String s = asset.getDescription();
		if (s != null && !"".equals(s)) {
		    String units = asset.getUnits();    // mandatory field but we are getting null sometimes
		    if (units != null) {
			s = s + "<br><br>1 unit = " + units;
		    }
		    ImageIcon icon = ImageLoader.fatCow16(ImageLoader.FATCOW.information);
		    map.put("icon", icon);
		    map.put("tooltip", s);
		};
		return map;
	    }
	    case COLUMN_ISSUER: {
		HashMap<String, Object> map = new HashMap<>();
		if (isBitcoin) {
		    return map;
		}
		String issuerName = asset.getIssuer();
		if (issuerName != null) {
		    ImageIcon icon = ImageLoader.fatCow16(ImageLoader.FATCOW.ceo);
		    map.put("icon", icon);
		    String tip = "Asset issued by " + asset.getIssuer();
		    map.put("tooltip", tip);
		}
		return map;
	    }
	    case COLUMN_SPENDABLE: {
		HashMap<String, Object> map = new HashMap<>();
		String s = "";
		if (isBitcoin) {
		    s = btcObj.syncText;
		    if (s == null) {
			s = btcObj.spendableText;
		    } else {
			if (btcObj.syncPercentText != null) {
			    s = s + " " + btcObj.syncPercentText;
			}
		    }
		    //if (btcObj.syncPercentToolTipText)
		    if (s == null) {
			s = "";
		    }
		} else if (asset != null && showAssetSpendableFlag) {
			s = "(" + CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, assetBalance.spendable) + " spendable)";
		}

		// Replace asset spendable flag if new asset needs more confirmaions
		// and this asset is almost ready to be sent i.e. has name, quantity.
		// Does not make sense to show this if asset files need to be uploaded.
		if (!isBitcoin && numConfirmsStillRequired>0 && mostCommonChainHeight>0 && asset!=null && asset.getName()!=null) {
		    int n = numConfirmsStillRequired;
		    // we dont want to show high number if wallet is resyncing from start.
		    if (n>NUMBER_OF_CONFIRMATIONS_TO_SEND_ASSET_THRESHOLD) {
			n = NUMBER_OF_CONFIRMATIONS_TO_SEND_ASSET_THRESHOLD;
		    }
		    
		    if (isWalletSyncing) {
			s = "(Synchronizing...)";
		    } else {
			s = "(Waiting for " + n + " confirmation" + ((n>1) ? "s)" : ")");
		    }
		}
		
		map.put("label", s);
		return map;
	    }
	    case COLUMN_EXPIRY: {
		HashMap<String, Object> map = new HashMap<>();
		if (isBitcoin) {
		    return map;
		}
		String tip = null;
		Date issueDate = asset.getIssueDate();
		if (issueDate != null) {
		    LocalDateTime issueDateTime = new DateTime(issueDate).toLocalDateTime();
		    tip = "Issued on " + issueDateTime.toString("d MMM y, HH:mm:ss") + ".";
		} else {
		    tip = "No issue date given.";
		}
		Date d = asset.getExpiryDate();
		ImageIcon icon = null;
		String s = null;
		if (d != null) {
		    LocalDateTime expiryDateTime = new DateTime(d).toLocalDateTime();

		    tip = tip + " Expires on " + expiryDateTime.toString("d MMM y, HH:mm:ss") + ".";

		    LocalDateTime now = LocalDateTime.now();

		    //For testing, plusDays and minusDays.
		    //expiryDateTime = now.minusDays(600);
		    Days daysDifference = Days.daysBetween(expiryDateTime, now); //, expiryDateTime);
		    int daysBetween = daysDifference.getDays();
		    int weeksBetween = daysDifference.toStandardWeeks().getWeeks();
		    daysBetween = Math.abs(daysBetween);
		    weeksBetween = Math.abs(weeksBetween);

		    String diff = null;
		    if (daysBetween <= 13) {
			diff = daysBetween + " days";
		    } else if (weeksBetween <= 12) {
			diff = weeksBetween + " weeks";
		    } else {
			diff = Math.abs(weeksBetween / 4) + " months";
		    }
		    if (now.compareTo(expiryDateTime) > 0) {
			s = "Expired " + diff + " ago";
			icon = ImageLoader.fatCow16(ImageLoader.FATCOW.skull_old);
			if (daysBetween <= 7) {
			    s = s + " (" + expiryDateTime.toString("d MMM y h:mm a z") + ")";
			} else {
			    s = s + " (" + expiryDateTime.toString("d MMM y") + ")";
			}
		    } else {
			s = "Expires on " + expiryDateTime.toString("d MMM y");
				//expiryDateTime.toString("d MMM y, HH:mm:ss") + ".";

			if (daysBetween <= 30) {
//			    s = "Expires in " + diff;
			    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.fire);
//			    s = s + " (" + expiryDateTime.toString("d MMM, h:mm a z") + ")";

			} else {
//			    s = "Expires in " + diff;
			    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.clock);
//			    s = s + " (" + expiryDateTime.toString("d MMM y") + ")";
			}
		    }

		} else {
		    // No expiry date info.
		    icon = null;
		    s = null;
		    tip = null;
		    
		    // If also no issue date info, that means we have not loaded the asset page yet
		    if (issueDate != null) {
			icon = ImageLoader.fatCow16(ImageLoader.FATCOW.thumb_up);
			s = "Does not expire";
		    }
		}
		// Expired!!! skull icon? thumbs down.
		map.put("icon", icon);
		map.put("text", s);
		map.put("tooltip", tip);
		return map;
	    }
	    case COLUMN_REFRESH: {
		HashMap<String, Object> map = new HashMap<>();
		if (isBitcoin) {
		    return map;
		}
		
		ImageIcon icon = null;
		String tip = null;

		if (assetState == CSAsset.CSAssetState.REFRESH) {
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.hourglass);
		    tip = "Checking...";
		} else {
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.arrow_refresh_small);
		    
		    Date validationDate = asset.getValidChecked();
		    tip = "Last checked: ";
		    if (tip==null) {
			tip = tip + "Never";
		    } else {
			tip = tip + CSMiscUtils.prettyFormatDate(validationDate);
		    }
		    tip = tip + "<br><br>Click to check the asset and balance now";
		}
		map.put("tooltip", tip);
		map.put("icon", icon);
		return map;
	    }
	    case COLUMN_QUANTITY: {
		HashMap<String, Object> map = new HashMap<>();
		if (isBitcoin) {
		    String s = btcObj.balanceText;
		    if (s == null) {
			s = "";
		    }
		    map.put("text",s);
		    return map;
//		    return s;
		}
		if (asset.getName() == null) {
		    return map;
//		    return ""; // TODO: Change to webPageJSON==null
		}
		
		String displayString = CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, assetBalance.total);

		if (showAssetUpdatingNowFlag) {
		    if (assetBalance.total.equals(BigInteger.ZERO)==true) {
			displayString = "...";
		    } else {
			displayString += " + ...";
		    }
		}
		// For debugging, round-trip to see if values match.
//		BigInteger y = CSMiscUtils.getRawUnitsFromDisplayString(asset, displayString);
//		System.out.println("DISPLAY: " + displayString + " ----> RAW: " + y);
		
		// tooltip
		String units = asset.getUnits();    // mandatory field but we are getting null sometimes
		if (units != null) {
		    String tip = "1 unit = " + units;
		    map.put("tooltip", tip);
		}
		
		map.put("text", displayString);
		return map;
//		return displayString;
	    }
	    case COLUMN_NAME: {
		HashMap map = new HashMap<>();
		if (isBitcoin) {
		    String s = btcObj.dataChangedText;
		    if (s != null) {
			// Show important warning that wallet was changed by another process
			map.put("text", s);
			StyleRange style = new StyleRange(0, -1, Font.BOLD, Color.RED);
			map.put("style", style);
			map.put("tooltip", btcObj.dataChangedToolTipText);
		    } else {
			map.put("text", "Bitcoin");
			//map.put("text2", "(bitcoin.org)");
			Font defaultFont = FontSizer.INSTANCE.getAdjustedDefaultFont();
			map.put("style", new StyleRange(0, -1, defaultFont.getStyle()));
		    }
		    map.put("noaction", true); // disable action
		    return map;
		}
		String name = asset.getNameShort(); // Short name is upto 16 chars, better for small table column.
		String domain = CSMiscUtils.getDomainHost(asset.getDomainURL());
		boolean uploadFiles = false;
		if (name != null) {
		    map.put("text", name);
		    map.put("text2", "(" + domain + ")");
		} else {
		    String s;
		    if (asset.getAssetSource() == CSAsset.CSAssetSource.GENESIS) {
			s = "Upload files to " + asset.getAssetWebPageURL();
			uploadFiles = true;
		    } else {
			if (domain == null) {
			    s = "Locating new asset..";
			    if (lastHeight < assetRef.getBlockNum()) {
				s = "Awaiting network synchronization...";
			    }
			    
			} else {
			    s = "Loading new asset from " + domain + "...";
			}
		    }
		    map.put("text", s);
		    StyleRange style = new StyleRange(0, -1, Font.PLAIN, Color.RED);
		    map.put("style", style);
		    map.put("noaction", true); // disable action
		}
		
		if (uploadFiles) {
		    map.put("tooltip", map.get("text"));
		} // for upload, it's long so show url.
		else if (asset.getName()!=null && domain!=null) {
		    String tip = asset.getName() + "<br>Issuer: " + asset.getIssuer() + "<br><br>Click to view the asset web page or issuer home page.";
		    map.put("tooltip", tip);
		}
		return map;
	    }
	    case COLUMN_CONFIRMATION: {
		HashMap<String, Object> map = new HashMap<>();
		if (isBitcoin) {
		    return map;
		}
		ImageIcon icon = null;
		String tip = null;

		int txHeight = 0;
		int numConfirmations = 0;
		String newAssetString = null;

		if (assetRef != null) {
		    txHeight = (int) assetRef.getBlockNum();
		    numConfirmations = lastHeight - txHeight + 1; // 0 means no confirmation, 1 is yes for same block.
		    String issueString = null;
		    Date issueDate = asset.getIssueDate();
		    String sinceString = null;
		    if (issueDate != null) {
			LocalDateTime issueDateTime = new DateTime(issueDate).toLocalDateTime();
			issueString = "Date of issue was " + issueDateTime.toString("d MMM y, HH:mm:ss z");
			// Compute the 'minutes/hours/days/weeks ago' string.
			LocalDateTime now = LocalDateTime.now();
			Minutes theDiff = Minutes.minutesBetween(issueDateTime, now);
			//Days daysDifference = Days.daysBetween(issueDateTime, now);
			int minutesBetween = theDiff.getMinutes();
			int hoursBetween = theDiff.toStandardHours().getHours();
			int daysBetween = theDiff.toStandardDays().getDays();
			int weeksBetween = theDiff.toStandardWeeks().getWeeks();
			minutesBetween = Math.abs(minutesBetween);
			hoursBetween = Math.abs(hoursBetween);
			daysBetween = Math.abs(daysBetween);
			weeksBetween = Math.abs(weeksBetween);
			if (minutesBetween <= 120) {
			    sinceString = minutesBetween + " minutes";
			} else if (hoursBetween <= 48) {
			    sinceString = hoursBetween + " hours";
			} else if (daysBetween <= 14) {
			    sinceString = daysBetween + " days";
			} else {
			    sinceString = weeksBetween + " weeks";
			}

		    } else {
			// TODO: Find a new issue date, or date of first insert into blockchain?
			// Transaction.getUpdateTime() first seen
			// tx.getUpdateTime()
			issueString = "No issue date given";
		    }
		    // issue date is null when tx is null, getUpdateTime returning junk.
		    tip = String.format("Issued %s ago, at block height %d, number of confirmations %d (%s)", sinceString, txHeight, numConfirmations, issueString);
		    newAssetString = String.format("Asset issued only %s ago, caution recommended.<br>Issued at block height %d, number of confirmations %d (%s)", sinceString, txHeight, numConfirmations, issueString);
		}

		if (assetRef == null) {
		    // Fix for... GDG Issue 8: If an asset does not yet have an asset ref, the reason can only be that it's a genesis that has not yet been confirmed. 
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.warning);
		    tip = "Awaiting new asset confirmation...";
		} else if (asset.getName() != null && numConfirmations < 0) {



// <=
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.warning);
		    
		    // If there are no connected peers, the network is probably down
		    if (numConnectedPeers==0) {
			tip = "Waiting to connect to Bitcoin network";
		    } else {
			int n = NUMBER_OF_CONFIRMATIONS_TO_SEND_ASSET_THRESHOLD;
			if (numConfirmsStillRequired<n) n = numConfirmsStillRequired;
			
			if (isWalletSyncing) {
			    tip = "Synchronizing...";
			} else {
			    tip = "Waiting for " + n + " confirmation" + ((n>1) ? "s" : "");
			}
		    }
		} else if (assetState==CSAsset.CSAssetState.BLOCK_NOT_FOUND ||
			assetState==CSAsset.CSAssetState.TX_NOT_FOUND) {
		    // Example: manually add an asset-ref which is bogus.
		    
		    if (txHeight <= lastHeight) {
			if (assetState==CSAsset.CSAssetState.TX_NOT_FOUND) {
			    tip = "Searching for asset's genesis transaction...";
			} else {
			    tip = "Searching for asset's genesis block...";
			}
		    } else {
			tip = "Awaiting network synchronization...";
		    }
		    
		    
		    // FIXME: See Gideon email about faucet test and genesis header not found.
		    
		    tip = "Searching for the asset's genesis transaction";
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.find);                   
//                    if  (asset.getAssetSource() == CSAsset.CSAssetSource.GENESIS) { 
//                    }
		} else if (assetState==CSAsset.CSAssetState.ASSET_SPECS_NOT_FOUND ||
			assetState==CSAsset.CSAssetState.ASSET_SPECS_NOT_PARSED ||
			assetState==CSAsset.CSAssetState.ASSET_WEB_PAGE_NOT_FOUND ||
			assetState==CSAsset.CSAssetState.REQUIRED_FIELD_MISSING)
		{
		    tip = "Waiting for asset files to be uploaded.";
                    if (asset.getAssetState()==CSAsset.CSAssetState.REQUIRED_FIELD_MISSING) {
                        tip = "Asset files are missing required data.";
                    }
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.server_error);
		}
		else if (assetState==CSAsset.CSAssetState.HASH_MISMATCH) {
		    tip = "Caution: contract does not match asset details encoded by issuer.<br><br>" + tip;
		    //tip = "Caution: asset details do not match what was encoded by issuer.<br><br>" + tip;
		    icon = ImageLoader.fugue(ImageLoader.FUGUE.traffic_cone__exclamation);
		} else if (assetState==CSAsset.CSAssetState.CONTRACT_INVALID) {
		    tip = "Caution: contract is missing or damaged.<br><br>" + tip;
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.warning);
		}
		else if (assetState==CSAsset.CSAssetState.REFRESH) {
		    tip = null;
		    icon = null;
//		    tip = "Refreshing...";
//		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.hourglass);
		}
		else if (asset.getName() != null && numConfirmations >= 1 && numConfirmations < 1008) {
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.new_new);
		    tip = newAssetString;
		} else if (numConfirmations >= 1008) {
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.tick_shield);
		}
		map.put("tooltip", tip);
		map.put("icon", icon);
		return map;
	    }
	    case COLUMN_CONTRACT: {
		HashMap<String, Object> map = new HashMap<>();
		if (isBitcoin) {
		    return map;
		}

		ImageIcon icon = null;
		//null; // TODO: Change to webPageJSON==null
		String tip = null;
		
		String s = asset.getDescription();
		if (s==null) {
		    String domain = asset.getDomainURL();
		    if (domain!=null) {
			String host = CSMiscUtils.getDomainHost(domain);
			s = "Unknown asset issued by " + host;
		    } else {
			// New asset without any files uploaded or genesis found
			s = "Unknown asset";
		    }
		}

		if (s != null && !"".equals(s)) {
		    tip = s + "<br><br>";
		}
		
		CSAsset.CSAssetContractState contractState = asset.getAssetContractState();
		if (contractState==CSAsset.CSAssetContractState.UNKNOWN) {
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.page_white_find);
		    tip = tip + "Contract not found on issuer's web-site";
		    
		    // This might happen if file on server is deleted
		    if (asset.getValidContractPath() != null) {
			tip = tip + "<br><br>Click to read the cached copy of the contract";	
		    }

		} else if (contractState==CSAsset.CSAssetContractState.CANNOT_PARSE) {
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.page_white_error);
		    tip = tip + "Contract is not a valid PDF document";
		    
		    // This might occur if file on server becomes corrupted
		    if (asset.getValidContractPath() != null) {
			tip = tip + "<br><br>Click to read the cached copy of the contract";	
		    }
		} else if (contractState==CSAsset.CSAssetContractState.EMBEDDED_URL) {
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.page_white_error);
		    tip = tip + "Contract is not allowed to contain an embedded URL";
		    
		    // This might occur if file on server is changed
		    if (asset.getValidContractPath() != null) {
			tip = tip + "<br><br>Click to read the cached copy of the contract";	
		    }
		} else if (contractState==CSAsset.CSAssetContractState.POSSIBLE_EMBEDDED_URL) {
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.page_white_error);
		    tip = tip + "Contract may contain an embedded URL which is not allowed";
		    
		    // This might occur if file on server is changed
		    if (asset.getValidContractPath()!= null) {
			tip = tip + "<br><br>Click to read the cached copy of the contract";	
		    }		    
		}
		// At this point, the contract state is VALID, so...
		// ...if the asset is invalid for any reason but the contract exists locally,
		//    show the cached contract.
		else if (assetState != CSAsset.CSAssetState.VALID && asset.getContractPath() != null)
		{
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.page_white_database);
		    tip = tip + "Click to read the cached copy of the contract";
		} else if (assetState == CSAsset.CSAssetState.VALID) {
		    icon = ImageLoader.fatCow16(ImageLoader.FATCOW.page_white_acrobat);
		    tip = tip + "Click to read the contract at the web site of the issuer";
		}

		if (tip != null && icon != null) {
		    map.put("tooltip", tip);
		    map.put("icon", icon);
		}
		return map;
	    }

//        case "walletAssetTableColumn.issuer":
//            return asset.getIssuer();
//	case "walletAssetTableColumn.issueDate":
//            return asset.getIssueDate();
//        case "walletAssetTableColumn.contractUrl":
//            return asset.getContractUrl();
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
    
}
