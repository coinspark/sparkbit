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
package org.multibit.viewsystem.swing.view.panels;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.common.eventbus.Subscribe;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.exchange.CurrencyConverter;
import org.multibit.exchange.CurrencyConverterResult;
import org.multibit.model.bitcoin.BitcoinModel;
import org.multibit.model.bitcoin.WalletAddressBookData;
import org.multibit.model.core.CoreModel;
import org.multibit.utils.ImageLoader;
import org.multibit.viewsystem.DisplayHint;
import org.multibit.viewsystem.View;
import org.multibit.viewsystem.Viewable;
import org.multibit.viewsystem.swing.ColorAndFontConstants;
import org.multibit.viewsystem.swing.MultiBitFrame;
import org.multibit.viewsystem.swing.action.*;
import org.multibit.viewsystem.swing.view.components.*;
import org.multibit.viewsystem.swing.view.models.AddressBookTableModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/*CoinSpark START*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.*;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import org.joda.money.Money;
import org.multibit.model.bitcoin.WalletAssetComboBoxModel;
import org.multibit.model.bitcoin.WalletAssetComboBoxItem;
import org.multibit.viewsystem.swing.view.panels.CSSendAssetPanel;
import org.multibit.viewsystem.swing.view.panels.CSMessageSendPanel;
import org.multibit.viewsystem.dataproviders.AssetFormDataProvider;
import org.coinspark.core.*;
import org.coinspark.wallet.*;
import org.coinspark.protocol.*;
import org.multibit.utils.CSMiscUtils;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
/*CoinSpark END*/

public class SendBitcoinPanel extends AbstractTradePanel implements Viewable, AssetFormDataProvider {

    static final Logger log = LoggerFactory.getLogger(SendBitcoinPanel.class);

    private static final long serialVersionUID = -2065108865497111662L;
    private static SendBitcoinConfirmAction sendBitcoinConfirmAction;
    private static boolean enableSendButton = false;
    private static MultiBitButton sendButton;
    private MultiBitButton pasteAddressButton;

    private static SendBitcoinPanel thisPanel;

    private static String regularTooltipText = "";
    private static String pleaseWaitTooltipText = "";

    /* CoinSpark START */
    private String sendAssetAmountText; // the amount typed in the textfield when send pressed
    private static SendAssetConfirmAction sendAssetConfirmAction;
    private JComboBox<WalletAssetComboBoxItem> assetComboBox;
    private JPanel bitcoinAmountPanel;
    private CSSendAssetPanel assetAmountPanel;
//    private GridBagConstraints amountPanelConstraints;
    private MultiBitLabel notificationLabel2;
    private int yGridAssetAmountPanel;
    private int yGridMessagePanel;
    private CSMessageSendPanel messageSendPanel;
    private MultiBitLabel messageLabel;
    private MultiBitLabel paymentRefTextLabel;
    /* CoinSpark END */

    public SendBitcoinPanel(BitcoinController bitcoinController, MultiBitFrame mainFrame) {
	super(mainFrame, bitcoinController);
	thisPanel = this;
	checkDeleteSendingEnabled();

	regularTooltipText = controller.getLocaliser().getString("sendBitcoinAction.tooltip");
	pleaseWaitTooltipText = controller.getLocaliser().getString("sendBitcoinAction.pleaseWait.tooltip");
	// Register ourselves as a listener the CSEventBus
	CSEventBus.INSTANCE.registerAsyncSubscriber(this);
	
//	sendButton.addFocusListener(this);
    }

    public static void setEnableSendButton(boolean enableSendButton) {
	SendBitcoinPanel.enableSendButton = enableSendButton;

	if (EventQueue.isDispatchThread()) {
	    enableSendButtonOnSwingThread();
	} else {
	    SwingUtilities.invokeLater(new Runnable() {
		@Override
		public void run() {
		    enableSendButtonOnSwingThread();
		}
	    });
	}
    }

    private static void enableSendButtonOnSwingThread() {
	if (sendBitcoinConfirmAction != null) {
	    sendBitcoinConfirmAction.setEnabled(SendBitcoinPanel.enableSendButton);
	}

	final String finalRegularTooltipText = regularTooltipText;
	final String finalPleaseWaitTooltipText = pleaseWaitTooltipText;

	if (sendButton != null) {
	    if (SendBitcoinPanel.enableSendButton) {
		sendButton.setEnabled(true);
		sendButton.setToolTipText(HelpContentsPanel.createTooltipText(finalRegularTooltipText));
	    } else {
		sendButton.setEnabled(false);
		sendButton.setToolTipText(HelpContentsPanel.createTooltipText(finalPleaseWaitTooltipText));
	    }
	}

	if (thisPanel != null) {
	    thisPanel.invalidate();
	    thisPanel.validate();
	    thisPanel.repaint();
	}
    }

    @Override
    protected boolean isReceiveBitcoin() {
	return false;
    }

    /*CoinSpark START*/
    
    @Override
    public boolean isTradingAsset() {
	return (getAssetId() != 0);
    }
    
    // Implement AssetFormDataProvider
    @Override
    public String getMessage() {
	return messageSendPanel.getMessageText();
    }
    
    @Override
    public int getAssetId() {
	Object o = assetComboBox.getSelectedItem();
	int id = 0;
	if (o != null) {
	    WalletAssetComboBoxItem w = (WalletAssetComboBoxItem) o;
	    id = w.getId();
	}
	return id;
    }

    @Override
    public String getAssetAmount() {
	String amount = assetAmountPanel.getAmount();
	return amount; //String.valueOf(amount);
    }
    
    @Override
    public String getAssetAmountText() {
	return sendAssetAmountText;
    }

    @Override
    public void setAssetAmount(String s) {
	assetAmountPanel.setAmount(s);
    }
    
    @Override
    public String getAssetReference() {
	String s = null;
	int id = getAssetId();
	if (id != 0) {
	    CSAsset asset = this.bitcoinController.getModel().getActiveWallet().CS.getAsset(id);
	    if (asset != null) {
		s = CSMiscUtils.getHumanReadableAssetRef(asset);
	    }
	}
	return s;
    }
    
    @Override
    public boolean selectAssetByReference(String assetRef) {
	//WalletAssetComboBoxModel
	boolean result = false;
	ComboBoxModel<WalletAssetComboBoxItem> model = assetComboBox.getModel();
	int size = model.getSize();
        for(int i=0;i<size;i++) {
            WalletAssetComboBoxItem item = model.getElementAt(i);
	    String s = item.getAssetRef();
	    if (s!=null) {
		if (s.equals(assetRef)) {
		    if (assetComboBox.getSelectedItem() != item) {
			assetComboBox.setSelectedItem(item);
			result = true;
			break;
		    }
		}
	    } else {
		// Select Bitcoin if applicable.
		if (i == 0 && s == null && assetRef == null) {
		    if (assetComboBox.getSelectedItem() != item) {
			assetComboBox.setSelectedItem(item);
			result = true;
			break;
		    }
		}
	    }
	}
	return result;
    }
    
//    @Override
    public boolean isSenderPays() {
	return assetAmountPanel.isSenderPays();
    }
    
    /*CoinSpark END */

    @Override
    public Action getCreateNewAddressAction() {
	return new CreateNewSendingAddressAction(super.bitcoinController, this);
    }

    @Override
    protected Action getDeleteAddressAction() {
	if (deleteAddressAction == null) {
	    return new DeleteSendingAddressAction(this.bitcoinController, mainFrame, this);
	} else {
	    return deleteAddressAction;
	}
    }

    @Override
    public void checkDeleteSendingEnabled() {
	AddressBookTableModel addressesTableModel = getAddressesTableModel();
	if (deleteAddressAction != null) {
	    deleteAddressAction.setEnabled(addressesTableModel != null && addressesTableModel.getRowCount() > 0);
	}
    }

    @Override
    public String getAddressConstant() {
	return BitcoinModel.SEND_ADDRESS;
    }

    @Override
    public String getLabelConstant() {
	return BitcoinModel.SEND_LABEL;
    }

    @Override
    public String getAmountConstant() {
	return BitcoinModel.SEND_AMOUNT;
    }

    /**
     * method for concrete impls to populate the localisation map
     */
    @Override
    protected void populateLocalisationMap() {
	localisationKeyConstantToKeyMap.put(ADDRESSES_TITLE, "sendBitcoinPanel.sendingAddressesTitle");
	localisationKeyConstantToKeyMap.put(CREATE_NEW_TOOLTIP, "createOrEditAddressAction.createSending.tooltip");
	localisationKeyConstantToKeyMap.put(DELETE_TOOLTIP, "deleteSendingAddressSubmitAction.tooltip");
    }

    @Override
    protected JPanel createFormPanel(JPanel formPanel, GridBagConstraints constraints) {
	formPanel.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);

	JPanel buttonPanel = new JPanel();
	FlowLayout flowLayout = new FlowLayout();
	flowLayout.setAlignment(FlowLayout.LEADING);
	buttonPanel.setLayout(flowLayout);

	formPanel.setLayout(new GridBagLayout());

	// create stents and forcers
	createFormPanelStentsAndForcers(formPanel, constraints);
	
	// Y grid position
	int yGridPosition = 0;
		
	
	yGridPosition++;

	MultiBitLabel addressLabel = new MultiBitLabel(controller.getLocaliser().getString("sendBitcoinPanel.addressLabel"));
	addressLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendBitcoinPanel.addressLabel.tooltip")));
	addressLabel.setHorizontalAlignment(JLabel.TRAILING);
	constraints.fill = GridBagConstraints.HORIZONTAL;
	constraints.gridx = 0;
	constraints.gridy = yGridPosition;
	constraints.weightx = 1.0;
	constraints.weighty = 0.2;
	constraints.gridwidth = 1;
	constraints.gridheight = 1;
	constraints.anchor = GridBagConstraints.LINE_END;
	formPanel.add(addressLabel, constraints);
	/* CoinSpark START */
//    String receiveAddressText = controller.getLocaliser().getString("receiveBitcoinPanel.addressLabel");
//    MultiBitLabel notUsedReceiveAddressLabel = new MultiBitLabel(receiveAddressText);
//    formPanel.add(MultiBitTitledPanel.createStent((int) notUsedReceiveAddressLabel.getPreferredSize().getWidth()), constraints);
    /* CoinSpark END */

	int coinsparkFieldWidth = fontMetrics.stringWidth(MultiBitFrame.EXAMPLE_COINSPARK_FIELD_TEXT);
	addressTextField = new MultiBitTextField("", MultiBitFrame.EXAMPLE_COINSPARK_FIELD_TEXT.length(), controller);
//	addressTextField = new MultiBitTextField("", 24, controller);
	addressTextField.setHorizontalAlignment(JTextField.LEADING);
//	int fontHeight = getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont()).getHeight() + TEXTFIELD_VERTICAL_DELTA;
	Dimension addressSize = new Dimension(new Dimension(coinsparkFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont()).getHeight() + TEXTFIELD_VERTICAL_DELTA));
	addressTextField.setMinimumSize(addressSize);
	//addressTextField.setMaximumSize(addressSize);
	addressTextField.setPreferredSize(addressSize);
	
//	addressTextField.addKeyListener(new AddressFieldKeyListener());
	addressTextField.addKeyListener(new QRCodeKeyListener());
	constraints.fill = GridBagConstraints.HORIZONTAL;
	constraints.gridx = 2;
	constraints.gridy = yGridPosition;
	constraints.weightx = 1000; //1.0;
	constraints.weighty = 0.2;
	constraints.gridwidth = 3;
	constraints.anchor = GridBagConstraints.LINE_START;
	formPanel.add(addressTextField, constraints);
	
	
	addressTextField.getDocument().addDocumentListener(new DocumentListener() {
	    public void changedUpdate(DocumentEvent e) {
		validateAddressUpdatePanel();
	    }

	    public void removeUpdate(DocumentEvent e) {
		validateAddressUpdatePanel();
	    }

	    public void insertUpdate(DocumentEvent e) {
		validateAddressUpdatePanel();
	    }
	});


	ImageIcon copyIcon = ImageLoader.createImageIcon(ImageLoader.COPY_ICON_FILE);
	CopySendAddressAction copyAddressAction = new CopySendAddressAction(controller, this, copyIcon);
	MultiBitButton copyAddressButton = new MultiBitButton(copyAddressAction, controller);
	constraints.fill = GridBagConstraints.NONE;
	constraints.gridx = 6;
	constraints.gridy = yGridPosition;
	constraints.weightx = 1;
	constraints.gridwidth = 1;
	constraints.anchor = GridBagConstraints.LINE_START;
	formPanel.add(copyAddressButton, constraints);

	ImageIcon pasteIcon = ImageLoader.createImageIcon(ImageLoader.PASTE_ICON_FILE);
	PasteAddressAction pasteAddressAction = new PasteAddressAction(super.bitcoinController, this, pasteIcon);
	pasteAddressButton = new MultiBitButton(pasteAddressAction, controller);
	constraints.fill = GridBagConstraints.NONE;
	constraints.gridx = 8;
	constraints.gridy = yGridPosition;
	constraints.weightx = 1.0; //10.0; //1; //0.0;
	constraints.weighty = 0.2;
	constraints.gridwidth = 1;
	constraints.anchor = GridBagConstraints.LINE_START;
	formPanel.add(pasteAddressButton, constraints);


	// Move the paste button stent to gridx 10 to act as a buffer area on the right hand side.
	JPanel pasteButtonStent = MultiBitTitledPanel.createStent((int)copyAddressButton.getPreferredSize().getWidth(), (int)copyAddressButton.getPreferredSize().getHeight());
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 10;
        constraints.gridy = yGridPosition;
        constraints.weightx = 1.0; //10.0;
        constraints.weighty = 0.2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        formPanel.add(pasteButtonStent, constraints);
	
	
	
	yGridPosition++;
	
	paymentRefTextLabel = new MultiBitLabel("");
	paymentRefTextLabel.setHorizontalAlignment(JLabel.LEADING);
	constraints.fill = GridBagConstraints.HORIZONTAL;
	constraints.gridx = 2;
	constraints.gridy = yGridPosition;
	constraints.weightx = 1.0;
	constraints.weighty = 0.2;
	constraints.gridwidth = 1;
	constraints.gridheight = 1;
	constraints.anchor = GridBagConstraints.LINE_START;
	formPanel.add(paymentRefTextLabel, constraints);
	
	
	
	yGridPosition++;
	
	
	
	MultiBitLabel labelLabel = new MultiBitLabel(controller.getLocaliser().getString("sendBitcoinPanel.labelLabel"));
	labelLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendBitcoinPanel.labelLabel.tooltip")));
	labelLabel.setHorizontalAlignment(JLabel.TRAILING);
	constraints.fill = GridBagConstraints.NONE;
	constraints.gridx = 0;
	constraints.gridy = yGridPosition;
	constraints.weightx = 1.0;
	constraints.weighty = 0.2; //1.0;
	constraints.gridwidth = 1;
	constraints.gridheight = 1;
	constraints.anchor = GridBagConstraints.LINE_END;
	formPanel.add(labelLabel, constraints);

	JTextField aTextField = new JTextField();
	labelTextArea = new MultiBitTextArea("", AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS, 20, controller);
	labelTextArea.setBorder(aTextField.getBorder());
	labelTextArea.addKeyListener(new QRCodeKeyListener());

	final JScrollPane labelScrollPane = new JScrollPane(labelTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
		JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	labelScrollPane.setOpaque(true);
	labelScrollPane.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
	labelScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
	labelScrollPane.getViewport().addChangeListener(new ChangeListener() {
	    @Override
	    public void stateChanged(ChangeEvent e) {
		if (labelScrollPane.getVerticalScrollBar().isVisible()) {
		    labelScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));
		} else {
		    labelScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		}
	    }
	});
//	labelScrollPane.setMinimumSize(new Dimension(longFieldWidth,40));
//	labelScrollPane.setPreferredSize(new Dimension(longFieldWidth,80));
	Dimension labelDimension = new Dimension(coinsparkFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont()).getHeight() * AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS + 6);// + TEXTFIELD_VERTICAL_DELTA + 6);
	labelScrollPane.setMinimumSize(labelDimension);
	//labelScrollPane.setMaximumSize(labelDimension);
	labelScrollPane.setPreferredSize(labelDimension);
	
//	labelScrollPane.setMinimumSize(new Dimension(longFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont())
//		.getHeight() * AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS + TEXTFIELD_VERTICAL_DELTA + 6));
//	labelScrollPane.setPreferredSize(new Dimension(longFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont())
//		.getHeight() * AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS + TEXTFIELD_VERTICAL_DELTA + 6));
	labelScrollPane.getHorizontalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);
	labelScrollPane.getVerticalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);

	constraints.fill = GridBagConstraints.BOTH; //HORIZONTAL;
	constraints.gridx = 2;
	constraints.gridy = yGridPosition;
	constraints.weightx = 1; //0.6;
	constraints.weighty = 0.2; //0; //1; //0.2; //1
	constraints.gridwidth = 3;
	constraints.gridheight = 1;
	constraints.anchor = GridBagConstraints.LINE_START;
	constraints.insets = new Insets(0, 0, 3, 0);	// when scrollers show to expand height, maintain gap to next grid
	formPanel.add(labelScrollPane, constraints);

	
	yGridPosition++;
	
	// Move the paste button stent to gridx 10 to act as a buffer area on the right hand side.
	JPanel myStent1 = MultiBitTitledPanel.createStent((int)labelScrollPane.getPreferredSize().getWidth(), 16 );
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = yGridPosition;
        constraints.weightx = 1.0; //10.0;
        constraints.weighty = 0.2;
        constraints.gridwidth = 4;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        formPanel.add(myStent1, constraints);
	

	yGridPosition++;

	
	
	MultiBitLabel assetTypeLabel = new MultiBitLabel(controller.getLocaliser().getString("sendBitcoinPanel.assetTypeLabel"));
	assetTypeLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendBitcoinPanel.sparkAddressLabel.tooltip")));
	assetTypeLabel.setBorder(BorderFactory.createMatteBorder((int) (TEXTFIELD_VERTICAL_DELTA * 0.5), 0, (int) (TEXTFIELD_VERTICAL_DELTA * 0.5), 0, ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR));
	assetTypeLabel.setHorizontalAlignment(JLabel.TRAILING);
	constraints.fill = GridBagConstraints.BOTH;
	constraints.gridx = 0;
	constraints.gridy = yGridPosition;
	constraints.weightx = 1.0;
	constraints.weighty = 0.2;
	constraints.gridwidth = 1;
	constraints.gridheight = 1;
	constraints.anchor = GridBagConstraints.LINE_END;
	formPanel.add(assetTypeLabel, constraints);

	WalletAssetComboBoxModel model = new WalletAssetComboBoxModel(this.bitcoinController);
	assetComboBox = new CSDisabledComboBox(model);
	assetComboBox.setSelectedIndex(0); // select Bitcoin
	assetComboBox.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
	assetComboBox.setBorder(BorderFactory.createEmptyBorder());
	assetComboBox.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
	assetComboBox.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
	assetComboBox.setRenderer(new CSDisabledItemsRenderer());
	assetComboBox.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		Object o = assetComboBox.getSelectedItem();
		if (o == null) {
		    return;
		}
		WalletAssetComboBoxItem w = (WalletAssetComboBoxItem) o;
		int id = w.getId();
		if (id == 0) {
		  // Bitcoin selected
		    removeAmountPanel(theFormPanel, assetAmountPanel);
		    setAmountPanel(theFormPanel, bitcoinAmountPanel);
		    notificationLabel.setText("");
		    sendButton.setAction(sendBitcoinConfirmAction);
		} else if (id == -1) {
		    // Nothing was selected
		} else {
		    // Asset was selected
		    removeAmountPanel(theFormPanel, bitcoinAmountPanel);
		    setAmountPanel(theFormPanel, assetAmountPanel);
		    notificationLabel.setText("");
		    sendButton.setAction(sendAssetConfirmAction);
		    //sendButton = new MultiBitButton(sendBitcoinConfirmAction, controller);
		    if (enableSendButton) {
			sendButton.setEnabled(true);
			sendButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendAssetAction.tooltip")));
		    } else {
			sendButton.setEnabled(false);
			sendButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendBitcoinAction.pleaseWait.tooltip")));
		    }
		    Wallet wallet = bitcoinController.getModel().getActiveWallet();		    
		    int n = CSMiscUtils.getNumberOfDisplayDecimalPlaces(wallet, id);
		    assetAmountPanel.setDecimalPlaces(n);
		    assetAmountPanel.updateForInvalidNumber();
		    
		    CSAsset asset = wallet.CS.getAsset(id);
		    String left = CSMiscUtils.getFmtLeftPortion(asset);
		    String right = CSMiscUtils.getFmtRightPortion(asset);
		    if (right==null) right="Units";
		    assetAmountPanel.setAmountFmtLeftLabel(left);
		    assetAmountPanel.setAmountFmtRightLabel(right);
		    
		    displaySenderRecipientCharges();
		}
		theFormPanel.revalidate();
	    }
	});

	constraints.fill = GridBagConstraints.HORIZONTAL;
	constraints.gridx = 2;
	constraints.gridy = yGridPosition;
	constraints.weightx = 1.0;
	constraints.weighty = 1.0;
	constraints.gridwidth = 3;
	constraints.gridheight = 1;
	constraints.anchor = GridBagConstraints.LINE_START;
	formPanel.add(assetComboBox, constraints);


	sendBitcoinConfirmAction = new SendBitcoinConfirmAction(super.bitcoinController, mainFrame, this);
	sendBitcoinConfirmAction.setEnabled(enableSendButton);
	sendButton = new MultiBitButton(sendBitcoinConfirmAction, controller);
	if (enableSendButton) {
	    sendButton.setEnabled(true);
	    sendButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendBitcoinAction.tooltip")));
	} else {
	    sendButton.setEnabled(false);
	    sendButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendBitcoinAction.pleaseWait.tooltip")));
	}
	
	// Mouse press event occurs before focus is lost, so we can capture what was typed before
	// losing focus leads to the textfield being updated/validated and the formatter applied.
	// We want to catch the user typing in too many decimals.
	sendButton.addMouseListener(new MouseAdapter() {
	    @Override
	    public void mousePressed(MouseEvent e) {
		sendAssetAmountText = assetAmountPanel.getAssetTextField().getText();
	    }
	});
		
	/* CoinSpark START */
	sendAssetConfirmAction = new SendAssetConfirmAction(super.bitcoinController, mainFrame, this);
	sendAssetConfirmAction.setEnabled(enableSendButton);
	/* CoinSpark END */

	
	constraints.fill = GridBagConstraints.HORIZONTAL;
	constraints.gridx = 6;
	constraints.gridy = yGridPosition; //yGridSendButton;
	constraints.weightx = 1; //0.1;
	constraints.weighty = 0.1;
	constraints.gridwidth = 3;
	constraints.gridheight = 1;
	constraints.anchor = GridBagConstraints.LINE_START;
	formPanel.add(sendButton, constraints);	
	
	
	
	yGridPosition++;
	
	
	
		this.messageLabel = new MultiBitLabel(controller.getLocaliser().getString("sendBitcoinPanel.messageLabel"));
	labelLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendBitcoinPanel.messageLabel.tooltip")));
	labelLabel.setHorizontalAlignment(JLabel.TRAILING);
	constraints.fill = GridBagConstraints.NONE;
	constraints.gridx = 0;
	constraints.gridy = yGridPosition;
	constraints.weightx = 1.0;
	constraints.weighty = 0.2; //1.0;
	constraints.gridwidth = 1;
	constraints.gridheight = 1;
	constraints.anchor = GridBagConstraints.LINE_END;
	formPanel.add(messageLabel, constraints);
	
	
	messageSendPanel = new CSMessageSendPanel();
	messageSendPanel.getMessageTextArea().addKeyListener(new MessageKeyListener());
	
	constraints.fill = GridBagConstraints.BOTH; //HORIZONTAL;
	constraints.gridx = 2;
	constraints.gridy = yGridPosition;
	constraints.weightx = 1; //0.6;
	constraints.weighty = 0.2; //0; //1; //0.2; //1
	constraints.gridwidth = 3;
	constraints.gridheight = 1;
	constraints.anchor = GridBagConstraints.LINE_START;
	formPanel.add(messageSendPanel, constraints);
	
	yGridPosition++;
	
	
	
	// Asset amount row
	// Because height of the asset amount panel can vary, it should currently be the
	// last row of widgets to show until we find a way to compute size dynamically
	// instead of setting to 1,000.
	yGridAssetAmountPanel = yGridPosition;
	
	
	MultiBitLabel amountLabel = new MultiBitLabel(controller.getLocaliser().getString("sendBitcoinPanel.amountLabel"));
	amountLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendBitcoinPanel.amountLabel.tooltip")));
	amountLabel.setHorizontalAlignment(JLabel.TRAILING);
	constraints.fill = GridBagConstraints.NONE;
	constraints.gridx = 0;
	constraints.gridy = yGridPosition;
	constraints.gridwidth = 1;
	constraints.gridheight = 1;
	constraints.weightx = 1.0; //0.1;  // FUDGE: Try to force labels to take more room and be visible.
	constraints.weighty = 0.2; //1;
	constraints.anchor = GridBagConstraints.LINE_END;
	formPanel.add(amountLabel, constraints);

	
	JPanel amountPanel = createAmountPanel();
	assetAmountPanel = new CSSendAssetPanel();
	assetAmountPanel.setDesiredWidth(addressTextField.getWidth());
	bitcoinAmountPanel = amountPanel;
//	cards = new JPanel(new CardLayout());
//	cards.add(amountPanel, "BITCOIN");
//	cards.add(assetAmountPanel, "COINSPARK");
//	cards.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
	assetAmountPanel.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
	
	// Add a listener on the unit textfield, so we can update ui with payment charges if any
	assetAmountPanel.getAssetTextField().addKeyListener(new AssetAmountKeyListener());

	setAmountPanel(formPanel, bitcoinAmountPanel);
	/*
	constraints.fill = GridBagConstraints.BOTH;
	constraints.gridx = 2;
	constraints.gridy = 4;
	constraints.gridwidth = 1;
	constraints.gridheight = 1;
	constraints.weightx = 0.1;
	constraints.weighty = 1.0; //.0; //1;
	constraints.anchor = GridBagConstraints.FIRST_LINE_START; //  LINE_START;
    formPanel.add(amountPanel, constraints);
//	formPanel.add(cards, constraints);
	*/
	
	
	yGridPosition++;
	
	
	

	
	

	notificationLabel = new MultiBitLabel("");
	notificationLabel.setForeground(Color.RED);
	//notificationLabel.setBorder(BorderFactory.createLineBorder(Color.GREEN));
	constraints.fill = GridBagConstraints.BOTH;
	constraints.gridx = 2;
	constraints.gridy = yGridPosition;

	constraints.gridwidth = 8;
	constraints.gridheight = 3;
	constraints.weightx = 0.1;
	constraints.weighty = 0.1;
	constraints.anchor = GridBagConstraints.ABOVE_BASELINE_LEADING;
	formPanel.add(notificationLabel, constraints);

	
	
	yGridPosition++;
	
	
	
	// Add another notification label
	notificationLabel2 = new MultiBitLabel("");
	notificationLabel2.setForeground(Color.MAGENTA); // not red, because it may be a suggestion or warning, rather than just an error.
	constraints.fill = GridBagConstraints.BOTH;
	constraints.gridx = 2;
	constraints.gridy = yGridPosition;

	constraints.gridwidth = 8;
	constraints.gridheight = 3;
	constraints.weightx = 0.1;
	constraints.weighty = 0.1;
	constraints.anchor = GridBagConstraints.ABOVE_BASELINE_LEADING;
	formPanel.add(notificationLabel2, constraints);	
	
	
	yGridPosition++;
	
	// Filler to push everything up if there is a lot of spare room..
	JPanel filler = new JPanel();
	filler.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
constraints.fill = GridBagConstraints.BOTH;
	constraints.gridx = 0;
	constraints.gridy = yGridPosition;
	constraints.gridwidth = 8;
	constraints.gridheight = 1;
	constraints.weightx = 0.1;
	constraints.weighty = 100.0;
	constraints.anchor = GridBagConstraints.ABOVE_BASELINE_LEADING;
	formPanel.add(filler, constraints);		
	
//	Action helpAction;
//	if (ComponentOrientation.LEFT_TO_RIGHT == ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())) {
//	    helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_ICON_FILE,
//		    "multiBitFrame.helpMenuText", "multiBitFrame.helpMenuTooltip", "multiBitFrame.helpMenuText",
//		    HelpContentsPanel.HELP_SENDING_URL);
//	} else {
//	    helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_RTL_ICON_FILE,
//		    "multiBitFrame.helpMenuText", "multiBitFrame.helpMenuTooltip", "multiBitFrame.helpMenuText",
//		    HelpContentsPanel.HELP_SENDING_URL);
//	}
//	HelpButton helpButton = new HelpButton(helpAction, controller);
//	helpButton.setText("");
//
//	String tooltipText = HelpContentsPanel.createMultilineTooltipText(new String[]{
//	    controller.getLocaliser().getString("sendBitcoinPanel.helpLabel1.message"),
//	    controller.getLocaliser().getString("sendBitcoinPanel.helpLabel2.message"),
//	    controller.getLocaliser().getString("sendBitcoinPanel.helpLabel3.message"), "\n",
//	    controller.getLocaliser().getString("multiBitFrame.helpMenuTooltip")});
//	helpButton.setToolTipText(tooltipText);
//	helpButton.setHorizontalAlignment(SwingConstants.LEADING);
//	helpButton.setBorder(BorderFactory.createEmptyBorder(0, HELP_BUTTON_INDENT, HELP_BUTTON_INDENT, HELP_BUTTON_INDENT));
//	constraints.fill = GridBagConstraints.HORIZONTAL;
//	constraints.gridx = 0;
//	constraints.gridy = 8;
//	constraints.weightx = 1;
//	constraints.weighty = 0.3;
//	constraints.gridwidth = 1;
//	constraints.gridheight = 1;
//	constraints.anchor = GridBagConstraints.BELOW_BASELINE_LEADING;
//	formPanel.add(helpButton, constraints);


	
	/*
	Action sidePanelAction = new MoreOrLessAction(controller, this);
	sidePanelButton = new MultiBitButton(sidePanelAction, controller);
	sidePanelButton.setBorder(BorderFactory.createEmptyBorder());
	sidePanelButton.setBorderPainted(false);
	sidePanelButton.setFocusPainted(false);
	sidePanelButton.setContentAreaFilled(false);

	displaySidePanel();

	constraints.fill = GridBagConstraints.NONE;
	constraints.gridx = 10;  // 4, 7
	constraints.gridy = 3;
	constraints.weightx = 0.1;
	constraints.weighty = 0.3;
	constraints.gridwidth = 1;
	constraints.gridheight = 3;
	constraints.anchor = GridBagConstraints.BASELINE_TRAILING;
	formPanel.add(sidePanelButton, constraints);
*/
	return formPanel;
    }


    
    public void removeAmountPanel(JPanel formPanel, JPanel panel) {
	if (panel==null) return;
	formPanel.remove(panel);
    }
    
    public void setAmountPanel(JPanel formPanel, JPanel panel) {
	if (panel==null) return;
	GridBagConstraints constraints = new GridBagConstraints();
	constraints.fill = GridBagConstraints.BOTH;
	constraints.gridx = 2;
	constraints.gridy = yGridAssetAmountPanel;
	constraints.gridwidth = 3; //3
	constraints.gridheight = 1;
	constraints.weightx = 0.1;
	constraints.weighty = 1.0; //.0; //1;
	constraints.anchor = GridBagConstraints.FIRST_LINE_START; //  LINE_START;
	formPanel.add(panel, constraints);
    }
    
    
    @Override
    public String getAddress() {
	if (addressTextField != null) {
	    return addressTextField.getText();
	} else {
	    return "";
	}
    }

    /* CoinSpark START */
    public String getSparkAddress() {
	if (sparkAddressTextField != null) {
	    return sparkAddressTextField.getText();
	} else {
	    return "";
	}
    }
    /* CoinSpark END */

    @Override
    public void loadForm() {
	// get the current address, label and amount from the model
	String address = this.bitcoinController.getModel().getActiveWalletPreference(BitcoinModel.SEND_ADDRESS);
	String label = this.bitcoinController.getModel().getActiveWalletPreference(BitcoinModel.SEND_LABEL);
	String amountNotLocalised = this.bitcoinController.getModel().getActiveWalletPreference(BitcoinModel.SEND_AMOUNT);

	// Load encoded message and if decoded, set it in message area.
	String encoded = this.bitcoinController.getModel().getActiveWalletPreference(BitcoinModel.SEND_MESSAGE);
	String message = null;
	if (encoded != null) {
	    try {
		message = URLDecoder.decode(encoded, "UTF-8");
	    } catch (UnsupportedEncodingException ue) {
	    }
	}
	messageSendPanel.setMessageText(message);


	if (amountBTCTextField != null) {
	    CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.parseToBTCNotLocalised(amountNotLocalised);

	    if (converterResult.isBtcMoneyValid()) {
		parsedAmountBTC = converterResult.getBtcMoney();
		String amountLocalised = CurrencyConverter.INSTANCE.getBTCAsLocalisedString(converterResult.getBtcMoney());
		amountBTCTextField.setText(amountLocalised);
		if (notificationLabel != null) {
		    notificationLabel.setText("");
		}
	    } else {
		parsedAmountBTC = null;
		amountBTCTextField.setText("");
		if (notificationLabel != null) {
		    notificationLabel.setText(converterResult.getBtcMessage());
		}
	    }
	}

	if (address != null) {
	    addressTextField.setText(address);
	} else {
	    addressTextField.setText("");
	}
	if (label != null) {
	    labelTextArea.setText(label);
	} else {
	    labelTextArea.setText("");
	}

	/*CoinSpark START*/
    // Note: In future, sparkAddress will have priority over bitcoin address, so will convert from spark address
	//       to bitcoin address.  If user pastes a bitcoin address, action handler must clear the spark address.
//	String sparkAddress = this.bitcoinController.getModel().getActiveWalletPreference(BitcoinModel.SEND_SPARK_ADDRESS);
//	if (sparkAddress != null) {
//	    sparkAddressTextField.setText(sparkAddress);
//	} else {
//	    sparkAddressTextField.setText("");
//	}
	/*CoinSpark END*/

    // if there is a pending 'handleopenURI' that needs pasting into the
	// send form, do it
	String performPasteNow = this.bitcoinController.getModel().getActiveWalletPreference(BitcoinModel.SEND_PERFORM_PASTE_NOW);
	if (Boolean.TRUE.toString().equalsIgnoreCase(performPasteNow)) {
	    try {
		Address decodeAddress = new Address(this.bitcoinController.getModel().getNetworkParameters(), address);
		processDecodedString(com.google.bitcoin.uri.BitcoinURI.convertToBitcoinURI(decodeAddress, Utils.toNanoCoins(amountNotLocalised), label, null), null);
		this.bitcoinController.getModel().setActiveWalletPreference(BitcoinModel.SEND_PERFORM_PASTE_NOW, "false");
		sendButton.requestFocusInWindow();

		mainFrame.bringToFront();
	    } catch (AddressFormatException e) {
		throw new RuntimeException(e);
	    }
	}
    }

    public void setAddressBookDataByRow(WalletAddressBookData addressBookData) {
	addressTextField.setText(addressBookData.getAddress());
	addressesTableModel.setAddressBookDataByRow(addressBookData, selectedAddressRowModel, false);
    }

    @Override
    public void displayView(DisplayHint displayHint) {
	super.displayView(displayHint);

	if (DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED == displayHint) {
	    return;
	}

	JTextField aTextField = new JTextField();

	labelTextArea.setBorder(aTextField.getBorder());

	String bringToFront = controller.getModel().getUserPreference(BitcoinModel.BRING_TO_FRONT);
	if (Boolean.TRUE.toString().equals(bringToFront)) {
	    controller.getModel().setUserPreference(BitcoinModel.BRING_TO_FRONT, "false");
	    mainFrame.bringToFront();
	}

	((WalletAssetComboBoxModel) assetComboBox.getModel()).updateItems();

	// disable any new changes if another process has changed the wallet
	if (this.bitcoinController.getModel().getActivePerWalletModelData() != null
		&& this.bitcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
	    // files have been changed by another process - disallow edits
	    mainFrame.setUpdatesStoppedTooltip(addressTextField);
	    addressTextField.setEditable(false);
	    addressTextField.setEnabled(false);

	    if (sendButton != null) {
		sendButton.setEnabled(false);
		mainFrame.setUpdatesStoppedTooltip(sendButton);
	    }
	    if (pasteAddressButton != null) {
		pasteAddressButton.setEnabled(false);
		mainFrame.setUpdatesStoppedTooltip(pasteAddressButton);
	    }
	    titleLabel.setText(controller.getLocaliser().getString("sendBitcoinPanel.sendingAddressesTitle.mayBeOutOfDate"));
	    mainFrame.setUpdatesStoppedTooltip(titleLabel);
	} else {
	    addressTextField.setToolTipText(null);
	    addressTextField.setEditable(true);
	    addressTextField.setEnabled(true);

	    if (sendButton != null) {
		// CoinSpark: TODO: Update tooltip if the send button is for assets, and not bitcoin.
		if (SendBitcoinPanel.enableSendButton) {
		    sendButton.setEnabled(true);
		    sendButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendBitcoinAction.tooltip")));
		} else {
		    sendButton.setEnabled(false);
		    sendButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendBitcoinAction.pleaseWait.tooltip")));
		}
	    }
	    if (pasteAddressButton != null) {
		pasteAddressButton.setEnabled(true);
		pasteAddressButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("pasteAddressAction.tooltip")));
	    }
	    titleLabel.setText(controller.getLocaliser().getString("sendBitcoinPanel.sendingAddressesTitle"));
	    titleLabel.setToolTipText(null);
	}
	checkDeleteSendingEnabled();
	
	validateAddressUpdatePanel();
    }

    @Override
    public Icon getViewIcon() {
	return null;
//	return ImageLoader.createImageIcon(ImageLoader.SEND_BITCOIN_ICON_FILE);
    }

    @Override
    public String getViewTitle() {
	return controller.getLocaliser().getString("sendBitcoinConfirmAction.text");
    }

    @Override
    public String getViewTooltip() {
	return "Send bitcoin or assets to someone";
//	return controller.getLocaliser().getString("sendBitcoinConfirmAction.tooltip");
    }

    @Override
    public View getViewId() {
	return View.SEND_BITCOIN_VIEW;
    }

    public SendBitcoinConfirmAction getSendBitcoinConfirmAction() {
	return sendBitcoinConfirmAction;
    }
    
    /**
     * Save message to wallet info on each key press in message text area
     */
    protected class MessageKeyListener implements KeyListener {
        /** Handle the key typed event in the message field */
        @Override
        public void keyTyped(KeyEvent e) {
        }

        /** Handle the key-pressed event in the message field */
        @Override
        public void keyPressed(KeyEvent e) {
            // do nothing
        }

        /** Handle the key-released event in the message field */
	@Override
	public void keyReleased(KeyEvent e) {
	    String message = getMessage();
	    String encoded = null;
	    //Base64.encodeBase64String(binaryData)
	    if (message != null) {
		try {
		    encoded = URLEncoder.encode(message, "UTF-8");
		} catch (UnsupportedEncodingException ue) {
		    // do nothing
		    return;
		}
	    }

	    bitcoinController.getModel().setActiveWalletPreference(BitcoinModel.SEND_MESSAGE, encoded);
	    bitcoinController.getModel().getActivePerWalletModelData().setDirty(true);
	}
    }


    class AssetAmountKeyListener implements KeyListener {
        /** Handle the key typed event from the text field. */
        @Override
        public void keyTyped(KeyEvent e) {
        }

        /** Handle the key-pressed event from the text field. */
        @Override
        public void keyPressed(KeyEvent e) {
            // do nothing
        }

        /** Handle the key-released event from the text field. */
	@Override
	public void keyReleased(KeyEvent e) {
	    // ENTER: - keyReleased: java.awt.event.KeyEvent[KEY_RELEASED,keyCode=10,keyText=Enter,keyChar=Enter,keyLocation=KEY_LOCATION_STANDARD,rawCode=36,primaryLevelUnicode=10,scancode=0,extendedKeyCode=0xa]
	    // TODO: We have not found a way to manually force commit, as commitEdit() does not seem
	    // to work.  One solution is for the text field to lose focus, and perhaps for focus to
	    // move to the radio buttons.  It works here, but not in an ActionListener ActionPerformed method.
	    // The ActionPerformed also has another issue, if data has changed, the first
	    // enter is ignored and does not trigger an action, need a second enter to do so.
	    // if no changes, the first enter triggers action.
	    int key=e.getKeyCode();
	    if (key==KeyEvent.VK_ENTER) {
		assetAmountPanel.formatAmountToDecimalPlaces();
	    }
	    
	    displaySenderRecipientCharges();
	    displayQRCode(getAddress(), getAssetAmount(), getLabel());
	}
    }

    
    /*
    Display sender and recipient charges for an asset, based on amount entered in textfield
    */
    void displaySenderRecipientCharges() {

//	    System.out.println("key event source: " + e.getSource());
//	    System.out.println("text: " + assetAmountPanel.getAssetTextField().getText());
	String displayAmountString = assetAmountPanel.getDisplayAmount();
	if (displayAmountString != null) {

	    // If there are too many decimal places, more than specified by the asset,
	    // we consider the number to be invalid.
	    try {
		BigDecimal bd = new BigDecimal(displayAmountString);
		int a = CSMiscUtils.getNumberOfDecimalPlaces(bd);
		int b = assetAmountPanel.getNumberOfDecimalPlaces();
		if (a > b) {
		    displayAmountString = null; // too many decimal places
		}
	    } catch (NumberFormatException e) {
		displayAmountString = null; // should not happen but who knows?
	    }
	}
	if (displayAmountString == null) {
	    assetAmountPanel.updateForInvalidNumber();
	    displayQRCode(getAddress(), getAssetAmount(), getLabel());
	    return;
	}

	int assetID = getAssetId();
	Wallet wallet = bitcoinController.getModel().getActiveWallet();
	CSAsset asset = wallet.CS.getAsset(assetID);

	DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance();
	formatter.setParseBigDecimal(true);
	BigDecimal numDisplayUnitsToTransfer = null;
	try {
	    numDisplayUnitsToTransfer = (BigDecimal) formatter.parse(displayAmountString);
		//log.debug("For locale " + controller.getLocaliser().getLocale().toString() +  ", '" + btcString + "' parses to " + parsedBTC.toPlainString());
	    //System.out.println("Parsed valid number: " + numDisplayUnitsToTransfer);
	} catch (ParseException pe) {
	    log.debug("Parse exception: " + pe);
	}

	// Error parsing amount
	if (numDisplayUnitsToTransfer == null) {
	    assetAmountPanel.updateForInvalidNumber();
	    displayQRCode(getAddress(), getAssetAmount(), getLabel());
	    return;
	}
//	    System.out.println("display amount = " + displayAmountString);
//                address = addressTextField.getText();

	BigInteger numRawUnitsToTransfer = CSMiscUtils.getRawUnitsFromDisplayString(asset, numDisplayUnitsToTransfer.toPlainString());
	
	CoinSparkGenesis genesis = asset.getGenesis();
	long rawTransferAmount = numRawUnitsToTransfer.longValue();
//	long rawChargeAmount = genesis.calcCharge(rawTransferAmount);
	long senderGrossTransferAmount = genesis.calcGross(rawTransferAmount);
	long recipientNetTransferAmount = genesis.calcNet(rawTransferAmount);

	short chargeBasisPoints = genesis.getChargeBasisPoints();
	long rawFlatChargeAmount = genesis.getChargeFlat();
	
	assetAmountPanel.setChargeVisibility( rawFlatChargeAmount>0 || chargeBasisPoints>0 );

	//BigDecimal chargeDisplayUnits = CSMiscUtils.getDisplayUnitsForRawUnits(asset, new BigInteger(String.valueOf(rawChargeAmount)));
	BigDecimal senderChargeDisplayUnits = CSMiscUtils.getDisplayUnitsForRawUnits(asset, new BigInteger(String.valueOf( senderGrossTransferAmount - rawTransferAmount)));
	BigDecimal recipientChargeDisplayUnits = CSMiscUtils.getDisplayUnitsForRawUnits(asset, new BigInteger(String.valueOf(rawTransferAmount - recipientNetTransferAmount)));	
	BigDecimal recipientNetTransferDisplayUnits = CSMiscUtils.getDisplayUnitsForRawUnits(asset, new BigInteger(String.valueOf(recipientNetTransferAmount)));
	BigDecimal senderGrossTransferDisplayUnits = CSMiscUtils.getDisplayUnitsForRawUnits(asset, new BigInteger(String.valueOf(senderGrossTransferAmount)));

	String grossLabel = CSMiscUtils.getFormattedDisplayString(asset, senderGrossTransferDisplayUnits);
	String senderChargeLabel = CSMiscUtils.getFormattedDisplayString(asset, senderChargeDisplayUnits);
	assetAmountPanel.setSenderPaysAmount("fee of " + senderChargeLabel + ", sending total amount of " + grossLabel);
	String netLabel = CSMiscUtils.getFormattedDisplayString(asset, recipientNetTransferDisplayUnits);
	String recipientChargeLabel = CSMiscUtils.getFormattedDisplayString(asset, recipientChargeDisplayUnits);
	assetAmountPanel.setRecipientPaysAmount("fee of " + recipientChargeLabel + ", receiving net amount of " + netLabel);


	displayAssetChargesText();
    }

    /*
    Update the charges label based on selected asset
    */
    void displayAssetChargesText() {
	int assetID = getAssetId();
	Wallet wallet = bitcoinController.getModel().getActiveWallet();
	CSAsset asset = wallet.CS.getAsset(assetID);

	CoinSparkGenesis genesis = asset.getGenesis();
	int chargeBasisPoints = genesis.getChargeBasisPoints();
	long flatCharge = genesis.getChargeFlat();
	boolean flatChargeExists = flatCharge > 0;
	boolean interestChargeExists = chargeBasisPoints > 0;

	StringBuilder sb = new StringBuilder();
	if (!flatChargeExists && !interestChargeExists) {
	    sb.append("");
	    // Disable radio button etc.
	} else {
	    sb.append("Asset transaction fee of ");
	    if (chargeBasisPoints > 0) {
		double chargePercent = chargeBasisPoints / 100.0;
		sb.append(chargePercent + "%");
		if (flatChargeExists) {
		    sb.append(" and ");
		}
	    }
	    if (flatChargeExists) {
		String x = CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, new BigInteger(String.valueOf(flatCharge)));
		sb.append(x);
	    }
	    sb.append(" also applies.");
	}

	assetAmountPanel.setChargeLabelText(sb.toString());
    }
    
    
    /*
    Update asset combo box and other widgets, based on contents of the address text field.
    */
    public void validateAddressUpdatePanel() {
	boolean showingAssets = isTradingAsset();
	boolean showingBTC = !showingAssets;

	ComboBoxModel<WalletAssetComboBoxItem> model = assetComboBox.getModel();
	boolean assetsAvailable = model.getSize() > 1;

	boolean validCoinsparkAddress = false;
	boolean validBitcoinAddress = false;
	boolean cannotSendAssetsToValidCoinsparkAddress = false;
	boolean canSendMessage = false;
	boolean showPaymentRef = false;
	
	String address = addressTextField.getText();
	if (address.startsWith("s")) {
	    CoinSparkAddress csa = CSMiscUtils.decodeCoinSparkAddress(address);
	    if (csa != null) {
		String btcAddress = csa.getBitcoinAddress();
		validCoinsparkAddress = CSMiscUtils.validateBitcoinAddress(btcAddress, bitcoinController);
		    // a valid coinspark address cointains a valid btc address
		if (CSMiscUtils.canSendAssetsToCoinSparkAddress(csa)==false) {
		    cannotSendAssetsToValidCoinsparkAddress = true;
		}
	    }
	    int flags = csa.getAddressFlags();
	    canSendMessage = (flags & CoinSparkAddress.COINSPARK_ADDRESS_FLAG_TEXT_MESSAGES)>0;
	    
	    if ((flags & CoinSparkAddress.COINSPARK_ADDRESS_FLAG_PAYMENT_REFS) > 0) {
		CoinSparkPaymentRef paymentRef = csa.getPaymentRef();
		long ref = paymentRef.getRef();
		if (ref>0) {
		    showPaymentRef = true;
		    paymentRefTextLabel.setText("This address has a payment reference: " + ref);
		}
	    }
	    
	} else {
	    validBitcoinAddress = CSMiscUtils.validateBitcoinAddress(address, bitcoinController);
	}

	String message = null;
	if (!validBitcoinAddress && !validCoinsparkAddress) {
	    // Alert user that address is not valid, if something is in the address field.
	    if (address!=null && address.length()>0) { 
		message = "Please enter a valid Bitcoin or Coinspark address";
	    }
	    assetComboBox.setEnabled(false);
	} else if (validBitcoinAddress) {
	    assetComboBox.setEnabled(true);
	    if (assetsAvailable) message = "To send assets to the recipient, ask them for their Coinspark address";
	    if (showingAssets) {
		assetComboBox.getModel().setSelectedItem(assetComboBox.getModel().getElementAt(0));
	    }
	    ((WalletAssetComboBoxModel) assetComboBox.getModel()).disableAssets();
	} else if (validCoinsparkAddress && cannotSendAssetsToValidCoinsparkAddress) {
	    assetComboBox.setEnabled(false);
	    message = "The Coinspark address does not support sending assets";
	} else if (validCoinsparkAddress) {
	    assetComboBox.setEnabled(true);
	    ((WalletAssetComboBoxModel) assetComboBox.getModel()).enableAssets();
	}
	
	notificationLabel2.setText(message);
	
	// show/hide payment ref area
	
	// show/hide message text area
//	boolean oldVisibility = messageSendPanel.isVisible();
	messageSendPanel.setVisible(canSendMessage);
	messageLabel.setVisible(canSendMessage);
//	if (oldVisibility != canSendMessage) {
//	    upperPanel.validate();
//	}
	
	paymentRefTextLabel.setVisible(showPaymentRef);
    }
    
    
//    class AddressFieldKeyListener implements KeyListener {
//        /** Handle the key typed event from the text field. */
//        @Override
//        public void keyTyped(KeyEvent e) {
//        }
//
//        /** Handle the key-pressed event from the text field. */
//        @Override
//        public void keyPressed(KeyEvent e) {
//            // do nothing
//        }
//
//        /** Handle the key-released event from the text field. */
//	@Override
//	public void keyReleased(KeyEvent e) {
//	    validateAddressUpdatePanel();
//	}
//
//    }

    @Subscribe
    public void listen(CSEvent event) throws Exception {
	//log.debug("Received CSEvent: Type=" + event.getType() + " , info=" + event.getInfo());
	
	// Combo box should be updated whenever a new balanace has been calculated
	// for any asset.  However it could mean that the currently selected asset is no longer valid
	// for sending, so if the default BTC gets selected, the sudden change in UI will confuse users.
	// Better to keep the selected asset, even if now invalid, and then report an error on send.
	// TODO: Add refresh button so user can manually refresh menu? Otherwise user has new asset
	// inserted but it will never be valid at first so it never gets added to model.
	CSEventType t = event.getType();
	Object o = event.getInfo();
	
	if (t == CSEventType.BALANCE_VALID) {
	    SwingUtilities.invokeLater(new Runnable() {
		@Override
		public void run() {
		    //log.debug(">>>> updating combo box items");
		    WalletAssetComboBoxModel model = (WalletAssetComboBoxModel) assetComboBox.getModel();
		    model.updateItems();
		}
	    });
	}
    }

    // Experiment using focus listener to detech focus handover from text field to send button
//    public void focusGained(FocusEvent e) {
//        log.debug(">>>> Focus gained" + e);
//	log.debug("  >>>> opposite component = " + e.getOppositeComponent());
//	
//	if (e.getComponent()==sendButton && e.getOppositeComponent()==assetAmountPanel.getAssetTextField()) {
//	    log.debug("  >>>> OK DETECTED OK");
//	    log.debug("  >>>> " + assetAmountPanel.getAssetTextField().getText());
//	    log.debug("  >>>> " + getAssetAmountUncommitedTextValue());
//	}
//    }
//    
//    public void focusLost(FocusEvent e) {
//        log.debug(">>>> Focus lost" + e);
//	log.debug("  >>>> opposite component = " + e.getOppositeComponent());
//    }
  
}

