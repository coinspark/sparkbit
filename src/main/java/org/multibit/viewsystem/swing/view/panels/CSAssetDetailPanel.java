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
package org.multibit.viewsystem.swing.view.panels;

import com.google.bitcoin.core.Wallet;
import java.math.BigInteger;
import org.multibit.viewsystem.swing.view.components.SwingLink;

import org.multibit.controller.bitcoin.BitcoinController;
import org.coinspark.wallet.CSAsset;
import org.coinspark.core.CSUtils;

import java.util.Date;
import javax.swing.ImageIcon;
import org.coinspark.protocol.CoinSparkAssetRef;
import org.coinspark.protocol.CoinSparkGenesis;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.multibit.utils.CSMiscUtils;
import org.multibit.utils.ImageLoader;

import org.multibit.viewsystem.swing.view.components.FontSizer;

/**
 *
 */
public class CSAssetDetailPanel extends javax.swing.JPanel {

    private final BitcoinController bitcoinController;

    private CSAsset asset = null;
    
    /* CSASSET FIELD INFO
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("AssetID:        ").append(assetID).append("\n");
        sb.append("Date created:   ").append(dateCreation == null ? "" : dateCreation.toString()).append("\n");
        sb.append("State:          ").append(assetState == null ? "" : assetState).append("\n");
        sb.append("Source:         ").append(assetSource == null ? "" : assetSource).append("\n");
        sb.append("Genesis IxID:   ").append(genTxID == null ? "" : genTxID).append("\n");
        sb.append("Genesis:        ").append(genesis == null ? "" : genesis.toString()).append("\n");
        sb.append("AssetRef:       ").append(assetRef == null ? "" : assetRef.toString()).append("\n");
        sb.append("JSONString:     ").append(getJSONString()).append("\n");
        sb.append("Date validated: ").append(validChecked == null ? "" : validChecked.toString()).append("\n");
        sb.append("Failure count:  ").append(validFailures).append("\n");
        sb.append("Contract path:  ").append(contractPath == null ? "" : (contractPath + "(" + contractMimeType.getExtension() +")")).append("\n");
        sb.append("Image path:     ").append(imagePath == null ? "" : (imagePath + "(" + imageMimeType.getExtension() +")")).append("\n");
        sb.append("Icon path:      ").append(iconPath == null ? "" : (iconPath + "(" + iconMimeType.getExtension() +")")).append("\n");
        
        return sb.toString();
    */
    
    /**
     * Creates new form CSAssetDetailPanel
     */
    public CSAssetDetailPanel(BitcoinController bitcoinController) {
	this.bitcoinController = bitcoinController;
	
	initComponents();
    }

    String prettyFormatDate(Date d) {
	if (d == null) return "";
	LocalDateTime dt = new DateTime(d).toLocalDateTime();
	return dt.toString("d MMM y, HH:mm:ss z");
    }
    
    // Refresh the view with last known asset
    public void updateView() {
	if (asset != null) updateView(this.asset);
    }
    
    public void updateView(CSAsset asset) {
	this.asset = asset;
	boolean visible = false;
	String s = null;
	
//	assetIDLabel.setText("" + asset.getAssetID());
	
	CoinSparkAssetRef assetRef = asset.getAssetReference();
	visible = (assetRef != null);
	assetRefTextField.setEditable(false);
	assetRefTextField.setVisible(visible);
	assetRefKeyLabel.setVisible(visible);
	if (visible) {
	    assetRefTextField.setText(CSMiscUtils.getHumanReadableAssetRef(asset));
	}
	
	s = asset.getAssetWebPageURL();
	visible = (s != null);
	assetWebPageURLLabel.setVisible(visible);
	assetWebPageURLKeyLabel.setVisible(visible);
	if (visible) {
	    assetWebPageURLLabel.setText(s);
	    ((SwingLink)assetWebPageURLLabel).setURL(s);    // hyperlink
	}

	s = CSMiscUtils.getHumanReadableAssetState(asset.getAssetState());
//	s = asset.getAssetState().name();
	visible = (s != null);
	stateLabel.setVisible(visible);
	stateKeyLabel.setVisible(visible);
	if (visible) {
	    stateLabel.setText(s);
	}

	s = asset.getGenTxID();
	visible = (s!=null);
	genesisTxidKeyLabel.setVisible(visible);
	genesisTxidTextField.setVisible(visible);
	genesisTxidTextField.setEditable(false);
	if (visible) {
	    genesisTxidTextField.setText(s);
	}

//	CoinSparkGenesis genesis = asset.getGenesis();
//	visible = (genesis != null);
	visible = false;
	genesisInfoKeyLabel.setVisible(visible);
	genesisInfoScrollPane.setVisible(visible);
//	if (visible) {
//	    genesisInfoTextArea.setText(genesis.toString());
//	}
	
	
	Date creationDate = asset.getDateCreation();
	visible = (creationDate != null);
	creationDateLabel.setVisible(visible);
	creationDateKeyLabel.setVisible(visible);
	if (visible) {
		creationDateLabel.setText(prettyFormatDate(creationDate));
	}
	
	Date validationDate = asset.getValidChecked();
	visible = (validationDate != null);
	dateValidatedKeyLabel.setVisible(visible);
	dateValidatedKeyLabel.setVisible(visible);
	if (visible) {
		dateValidatedLabel.setText(prettyFormatDate(validationDate));
	}
	
	int n = asset.getValidFailures();
	visible = (n > 0);
	validationFailureCountKeyLabel.setVisible(visible);
	validationFailureCountLabel.setVisible(visible);
	if (visible) {
		validationFailureCountLabel.setText(String.valueOf(n));
	}
	
	s = asset.getContractUrl();
	visible = (s != null);
	contractURLLabel.setVisible(visible);
	contractURLKeyLabel.setVisible(visible);
	if (visible) {
		contractURLLabel.setText(s);
		((SwingLink)contractURLLabel).setURL(s);    // hyperlink
//		ImageIcon icon = ImageLoader.createImageIcon(ImageLoader.CS_WWW_ICON_FILE);
//		contractURLLabel.setIcon(icon);
	}

	s = asset.getIconUrl();
	visible = (s != null && s.length()>0);
	iconURLLabel.setVisible(visible);
	iconURLKeyLabel.setVisible(visible);
	if (visible) {
		iconURLLabel.setText(s);
	}
	
	s = asset.getImageUrl();
	visible = (s != null  && s.length()>0);
	imageURLKeyLabel.setVisible(visible);
	imageURLLabel.setVisible(visible);
	if (visible) {
		imageURLLabel.setText(s);
	}
	

//	CSUtils.CSMimeType mimeType = asset.getContractMimeType();
//	contractMIMETypeLabel.setText(mimeType==null ? "" : mimeType.getType());
//	String extension = null;
//	if (mimeType != null) extension = mimeType.getExtension();
//	ImageIcon extensionIcon = ImageLoader.createImageIconForFileExtension(extension);
//	contractMIMETypeLabel.setIcon(extensionIcon);
	
//	contractURLLabel.setIcon(extensionIcon);
//        if (imageIcon != null) {
//            setIconImage(imageIcon.getImage());
//        }
	
	s = asset.getName();
	visible = (s!=null);
	nameKeyLabel.setVisible(visible);
	nameLabel.setVisible(visible);
	if (visible) {
	    nameLabel.setText(s);
	}
	
	s = asset.getIssuer();
	visible = (s!=null);
	issuerKeyLabel.setVisible(visible);
	issuerLabel.setVisible(visible);
	if (visible) {
	    issuerLabel.setText(s);
	}
	
	s = asset.getDescription();
	visible = (s!=null);
	descriptionKeyLabel.setVisible(visible);
	descriptionScrollPane.setVisible(visible);
	if (visible) {
	    descriptionTextArea.setText(s);
	}
	
	s = asset.getUnits();
	visible = (s!=null);
	valuePerUnitKeyLabel.setVisible(visible);
	valuePerUnitLabel.setVisible(visible);
	if (visible) {
	    valuePerUnitLabel.setText(s);
	}
	
	Date d = asset.getIssueDate(); // CSUtils.date2iso8601(d) ... a bit boring for display!
	visible = (d!=null);
	issueDateKeyLabel.setVisible(visible);
	issueDateLabel.setVisible(visible);
	if (visible) {
	    issueDateLabel.setText(prettyFormatDate(d));
	}
	
	d = asset.getExpiryDate();
	visible = (d!=null);
	expiryDateKeyLabel.setVisible(visible);
	expiryDateLabel.setVisible(visible);
	if (visible) {
	    expiryDateLabel.setText(prettyFormatDate(d));
	}
	

	visible = (asset.getName() != null); // if null, asset spec not found, we show "-"
	quantityLabel.setVisible(visible);
	quantityKeyLabel.setVisible(visible);
	if (visible) {
	    Wallet wallet = this.bitcoinController.getModel().getActiveWallet();
	    int assetID = asset.getAssetID();
	    Wallet.CoinSpark.AssetBalance assetBalance = wallet.CS.getAssetBalance(assetID);
	    BigInteger x =  assetBalance.total;
	    String display = CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, x);
	    if (assetBalance.updatingNow) {
		if (x.intValue()==0) {
		    display = "...";
		} else {
		    display += " + ...";
		}
	    }
	    quantityLabel.setText(display);
	}

	
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        creationDateKeyLabel = new javax.swing.JLabel();
        stateKeyLabel = new javax.swing.JLabel();
        genesisTxidKeyLabel = new javax.swing.JLabel();
        genesisInfoKeyLabel = new javax.swing.JLabel();
        assetRefKeyLabel = new javax.swing.JLabel();
        dateValidatedKeyLabel = new javax.swing.JLabel();
        validationFailureCountKeyLabel = new javax.swing.JLabel();
        contractURLKeyLabel = new javax.swing.JLabel();
        imageURLKeyLabel = new javax.swing.JLabel();
        iconURLKeyLabel = new javax.swing.JLabel();
        creationDateLabel = new javax.swing.JLabel();
        stateLabel = new javax.swing.JLabel();
        genesisInfoScrollPane = new javax.swing.JScrollPane();
        genesisInfoTextArea = new javax.swing.JTextArea();
        imageURLLabel = new javax.swing.JLabel();
        iconURLLabel = new javax.swing.JLabel();
        dateValidatedLabel = new javax.swing.JLabel();
        validationFailureCountLabel = new javax.swing.JLabel();
        contractURLLabel = new SwingLink();
        issueDateKeyLabel = new javax.swing.JLabel();
        issueDateLabel = new javax.swing.JLabel();
        expiryDateKeyLabel = new javax.swing.JLabel();
        expiryDateLabel = new javax.swing.JLabel();
        nameKeyLabel = new javax.swing.JLabel();
        nameLabel = new javax.swing.JLabel();
        issuerKeyLabel = new javax.swing.JLabel();
        issuerLabel = new javax.swing.JLabel();
        descriptionKeyLabel = new javax.swing.JLabel();
        descriptionScrollPane = new javax.swing.JScrollPane();
        descriptionTextArea = new javax.swing.JTextArea();
        quantityKeyLabel = new javax.swing.JLabel();
        quantityLabel = new javax.swing.JLabel();
        assetWebPageURLKeyLabel = new javax.swing.JLabel();
        valuePerUnitKeyLabel = new javax.swing.JLabel();
        valuePerUnitLabel = new javax.swing.JLabel();
        genesisTxidTextField = new javax.swing.JTextField();
        assetRefTextField = new javax.swing.JTextField();
        assetWebPageURLLabel = new SwingLink();

        java.awt.GridBagLayout layout1 = new java.awt.GridBagLayout();
        layout1.columnWidths = new int[] {0, 10, 0, 10, 0, 10, 0, 10, 0};
        layout1.rowHeights = new int[] {0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0};
        setLayout(layout1);

        creationDateKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        creationDateKeyLabel.setText("Date added to wallet:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(creationDateKeyLabel, gridBagConstraints);

        stateKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        stateKeyLabel.setText("Status:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(stateKeyLabel, gridBagConstraints);

        genesisTxidKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        genesisTxidKeyLabel.setText("Genesis TxID:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(genesisTxidKeyLabel, gridBagConstraints);

        genesisInfoKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        genesisInfoKeyLabel.setText("Genesis Info:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 30;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(genesisInfoKeyLabel, gridBagConstraints);

        assetRefKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        assetRefKeyLabel.setText("Asset Ref:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(assetRefKeyLabel, gridBagConstraints);

        dateValidatedKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        dateValidatedKeyLabel.setText("Last validation attempt:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(dateValidatedKeyLabel, gridBagConstraints);

        validationFailureCountKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        validationFailureCountKeyLabel.setText("Failure count:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 54;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(validationFailureCountKeyLabel, gridBagConstraints);

        contractURLKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        contractURLKeyLabel.setText("Contract URL:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 58;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(contractURLKeyLabel, gridBagConstraints);

        imageURLKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        imageURLKeyLabel.setText("Image URL:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 42;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(imageURLKeyLabel, gridBagConstraints);

        iconURLKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        iconURLKeyLabel.setText("Icon URL:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 46;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(iconURLKeyLabel, gridBagConstraints);

        creationDateLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        creationDateLabel.setText("jLabel18");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(creationDateLabel, gridBagConstraints);

        stateLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        stateLabel.setText("jLabel19");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(stateLabel, gridBagConstraints);

        genesisInfoScrollPane.setMinimumSize(new java.awt.Dimension(400, 400));

        genesisInfoTextArea.setEditable(false);
        genesisInfoTextArea.setColumns(20);
        genesisInfoTextArea.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        genesisInfoTextArea.setLineWrap(true);
        genesisInfoTextArea.setRows(5);
        genesisInfoTextArea.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        genesisInfoTextArea.setPreferredSize(new java.awt.Dimension(300, 200));
        genesisInfoScrollPane.setViewportView(genesisInfoTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 30;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(genesisInfoScrollPane, gridBagConstraints);

        imageURLLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        imageURLLabel.setText("jLabel18");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 42;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(imageURLLabel, gridBagConstraints);

        iconURLLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        iconURLLabel.setText("jLabel19");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 46;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(iconURLLabel, gridBagConstraints);

        dateValidatedLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        dateValidatedLabel.setText("jLabel20");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(dateValidatedLabel, gridBagConstraints);

        validationFailureCountLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        validationFailureCountLabel.setText("jLabel21");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 54;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(validationFailureCountLabel, gridBagConstraints);

        contractURLLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        contractURLLabel.setText("SWINGLINK");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 58;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(contractURLLabel, gridBagConstraints);

        issueDateKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        issueDateKeyLabel.setText("Issue date:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 34;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(issueDateKeyLabel, gridBagConstraints);

        issueDateLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        issueDateLabel.setText("jLabel9");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 34;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(issueDateLabel, gridBagConstraints);

        expiryDateKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        expiryDateKeyLabel.setText("Expiry date:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 38;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(expiryDateKeyLabel, gridBagConstraints);

        expiryDateLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        expiryDateLabel.setText("jLabel17");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 38;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(expiryDateLabel, gridBagConstraints);

        nameKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        nameKeyLabel.setText("Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(nameKeyLabel, gridBagConstraints);

        nameLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        nameLabel.setText("jLabel18");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(nameLabel, gridBagConstraints);

        issuerKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        issuerKeyLabel.setText("Issuer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(issuerKeyLabel, gridBagConstraints);

        issuerLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        issuerLabel.setText("jLabel19");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(issuerLabel, gridBagConstraints);

        descriptionKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        descriptionKeyLabel.setText("Description:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(descriptionKeyLabel, gridBagConstraints);

        descriptionScrollPane.setMinimumSize(new java.awt.Dimension(400, 80));
        descriptionScrollPane.setPreferredSize(new java.awt.Dimension(400, 80));

        descriptionTextArea.setEditable(false);
        descriptionTextArea.setColumns(20);
        descriptionTextArea.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setRows(5);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionScrollPane.setViewportView(descriptionTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        add(descriptionScrollPane, gridBagConstraints);

        quantityKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        quantityKeyLabel.setText("Quantity:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(quantityKeyLabel, gridBagConstraints);

        quantityLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        quantityLabel.setText("jLabel20");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(quantityLabel, gridBagConstraints);

        assetWebPageURLKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        assetWebPageURLKeyLabel.setText("Asset Web Page URL:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(assetWebPageURLKeyLabel, gridBagConstraints);

        valuePerUnitKeyLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        valuePerUnitKeyLabel.setText("Value per Unit:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(valuePerUnitKeyLabel, gridBagConstraints);

        valuePerUnitLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        valuePerUnitLabel.setText("jLabel4");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(valuePerUnitLabel, gridBagConstraints);

        genesisTxidTextField.setBackground(null);
        genesisTxidTextField.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        genesisTxidTextField.setText("jTextField1");
        genesisTxidTextField.setBorder(null);
        genesisTxidTextField.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(genesisTxidTextField, gridBagConstraints);

        assetRefTextField.setBackground(null);
        assetRefTextField.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        assetRefTextField.setText("jTextField2");
        assetRefTextField.setBorder(null);
        assetRefTextField.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(assetRefTextField, gridBagConstraints);

        assetWebPageURLLabel.setText("SWINGLINK");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(assetWebPageURLLabel, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel assetRefKeyLabel;
    private javax.swing.JTextField assetRefTextField;
    private javax.swing.JLabel assetWebPageURLKeyLabel;
    private javax.swing.JLabel assetWebPageURLLabel;
    private javax.swing.JLabel contractURLKeyLabel;
    private javax.swing.JLabel contractURLLabel;
    private javax.swing.JLabel creationDateKeyLabel;
    private javax.swing.JLabel creationDateLabel;
    private javax.swing.JLabel dateValidatedKeyLabel;
    private javax.swing.JLabel dateValidatedLabel;
    private javax.swing.JLabel descriptionKeyLabel;
    private javax.swing.JScrollPane descriptionScrollPane;
    private javax.swing.JTextArea descriptionTextArea;
    private javax.swing.JLabel expiryDateKeyLabel;
    private javax.swing.JLabel expiryDateLabel;
    private javax.swing.JLabel genesisInfoKeyLabel;
    private javax.swing.JScrollPane genesisInfoScrollPane;
    private javax.swing.JTextArea genesisInfoTextArea;
    private javax.swing.JLabel genesisTxidKeyLabel;
    private javax.swing.JTextField genesisTxidTextField;
    private javax.swing.JLabel iconURLKeyLabel;
    private javax.swing.JLabel iconURLLabel;
    private javax.swing.JLabel imageURLKeyLabel;
    private javax.swing.JLabel imageURLLabel;
    private javax.swing.JLabel issueDateKeyLabel;
    private javax.swing.JLabel issueDateLabel;
    private javax.swing.JLabel issuerKeyLabel;
    private javax.swing.JLabel issuerLabel;
    private javax.swing.JLabel nameKeyLabel;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JLabel quantityKeyLabel;
    private javax.swing.JLabel quantityLabel;
    private javax.swing.JLabel stateKeyLabel;
    private javax.swing.JLabel stateLabel;
    private javax.swing.JLabel validationFailureCountKeyLabel;
    private javax.swing.JLabel validationFailureCountLabel;
    private javax.swing.JLabel valuePerUnitKeyLabel;
    private javax.swing.JLabel valuePerUnitLabel;
    // End of variables declaration//GEN-END:variables
}
