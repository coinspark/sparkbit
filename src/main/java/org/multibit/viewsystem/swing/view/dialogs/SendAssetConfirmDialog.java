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

import org.multibit.viewsystem.swing.view.panels.SendBitcoinConfirmPanel;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FontMetrics;

import javax.swing.ImageIcon;

import org.multibit.controller.Controller;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.utils.ImageLoader;
import org.multibit.viewsystem.swing.MultiBitFrame;
import org.multibit.viewsystem.swing.view.components.FontSizer;
import org.multibit.viewsystem.swing.view.components.MultiBitDialog;

import com.google.bitcoin.core.Wallet.SendRequest;
import java.math.BigDecimal;

import java.math.BigInteger;
import org.multibit.viewsystem.swing.action.AssetValidator;
import org.coinspark.wallet.CSAsset;
import org.multibit.utils.CSMiscUtils;

/**
 * The send bitcoin confirm dialog.
 */
public class SendAssetConfirmDialog extends MultiBitDialog {

    private static final long serialVersionUID = 881435612345057705L;

    private static final int HEIGHT_DELTA = 150;
    private static final int WIDTH_DELTA = 400;
        
    private MultiBitFrame mainFrame;
    private SendBitcoinConfirmPanel sendBitcoinConfirmPanel;
    
    private final Controller controller;
    private final BitcoinController bitcoinController;
    
    private final SendRequest sendRequest;
    
    private final int assetId;
    private final BigInteger assetAmountRawUnits;
    private final AssetValidator validator;

    /**
     * Creates a new {@link SendBitcoinConfirmDialog}.
     */
    public SendAssetConfirmDialog(BitcoinController bitcoinController, MultiBitFrame mainFrame, SendRequest sendRequest, int assetId, BigInteger assetAmountRawUnits, AssetValidator validator) {
        super(mainFrame, bitcoinController.getLocaliser().getString("sendAssetConfirmView.title"));
        this.bitcoinController = bitcoinController;
        this.controller = this.bitcoinController;
        this.mainFrame = mainFrame;
        this.sendRequest = sendRequest;
	
	// Pass in asset related info
	// Would be better if we had a single data structure we could reference (not wallet prefs)
	this.assetId = assetId;
	this.assetAmountRawUnits = assetAmountRawUnits;
	this.validator = validator;

        ImageIcon imageIcon = ImageLoader.createImageIcon(ImageLoader.MULTIBIT_ICON_FILE);
        if (imageIcon != null) {
            setIconImage(imageIcon.getImage());
        }
        
        initUI();
        
        sendBitcoinConfirmPanel.getCancelButton().requestFocusInWindow();
        applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
    }

    /**
     * Initialise bitcoin confirm dialog.
     */
    public void initUI() {
        FontMetrics fontMetrics = getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont());
        
        if (mainFrame != null) {
            int minimumHeight = fontMetrics.getHeight() * 11 + HEIGHT_DELTA;
            int minimumWidth = Math.max(fontMetrics.stringWidth(MultiBitFrame.EXAMPLE_LONG_FIELD_TEXT), fontMetrics.stringWidth(controller.getLocaliser().getString("sendAssetConfirmView.message"))) + WIDTH_DELTA;
            setMinimumSize(new Dimension(minimumWidth, minimumHeight));
            positionDialogRelativeToParent(this, 0.5D, 0.47D);
        }
        
	// Set up the asset stuff, and then create a confirmation panel.
	CSAsset asset = this.bitcoinController.getModel().getActiveWallet().CS.getAssetDB().getAsset(this.assetId);
	String assetDomain = CSMiscUtils.getDomainHost(asset.getDomainURL());
	String assetDescription = String.format("%s (%s)", asset.getName(), (assetDomain == null) ? "???" : assetDomain);
	String assetAmountLabel = CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, this.assetAmountRawUnits);
	sendBitcoinConfirmPanel = new SendBitcoinConfirmPanel(this.bitcoinController, mainFrame, this, sendRequest, assetAmountLabel, assetDescription, validator);

        sendBitcoinConfirmPanel.setOpaque(false);
        
        setLayout(new BorderLayout());
        add(sendBitcoinConfirmPanel, BorderLayout.CENTER);
    }
}