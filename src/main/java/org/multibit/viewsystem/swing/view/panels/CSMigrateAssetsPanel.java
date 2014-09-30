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

import com.google.bitcoin.core.Utils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.math.BigInteger;
import java.util.Arrays;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.bitcoinj.wallet.Protos.Wallet.EncryptionType;
import org.multibit.controller.Controller;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.model.bitcoin.WalletBusyListener;
import org.multibit.model.core.CoreModel;
import org.multibit.utils.CSMiscUtils;
import org.multibit.utils.ImageLoader;
import org.multibit.viewsystem.DisplayHint;
import org.multibit.viewsystem.View;
import org.multibit.viewsystem.Viewable;
import org.multibit.viewsystem.swing.ColorAndFontConstants;
import org.multibit.viewsystem.swing.MultiBitFrame;
import org.multibit.viewsystem.swing.action.HelpContextAction;
import org.multibit.viewsystem.swing.action.CSMigrateAssetsSubmitAction; //SignMessageSubmitAction;
import org.multibit.viewsystem.swing.view.components.HelpButton;
import org.multibit.viewsystem.swing.view.components.MultiBitButton;
import org.multibit.viewsystem.swing.view.components.MultiBitLabel;
import org.multibit.viewsystem.swing.view.components.MultiBitTextArea;
import org.multibit.viewsystem.swing.view.components.MultiBitTitledPanel;

import org.coinspark.protocol.CoinSparkAddress;
import com.google.bitcoin.core.Wallet;


/**
 * View for migrating assets and bitcoin to a new wallet
 */
public class CSMigrateAssetsPanel extends JPanel implements Viewable, WalletBusyListener {

    private static final long serialVersionUID = 11111194329957705L;

    private final Controller controller;
    private final BitcoinController bitcoinController;

    private MultiBitFrame mainFrame;

    private MultiBitLabel messageLabel1;
    private MultiBitLabel messageLabel2;

    private MultiBitLabel walletTextLabel;
    private JPasswordField walletPasswordField;
    private MultiBitLabel walletPasswordPromptLabel;

    private MultiBitTextArea addressTextArea;
    private MultiBitLabel addressLabel;
    private MultiBitTextArea addressTextArea2;
    private MultiBitLabel addressLabel2;
    
    
    private MultiBitTextArea messageTextArea;
    private MultiBitLabel messageLabel;
    
    private MultiBitTextArea signatureTextArea;
    private MultiBitLabel signatureLabel;
    
//    private SignMessageSubmitAction signMessageSubmitAction;
    private CSMigrateAssetsSubmitAction migrateAssetsSubmitAction;
    private MultiBitButton clearAllButton;
    
    private JLabel tickLabel;
    private JLabel tickLabel2;
    private MultiBitButton submitButton;
    private MultiBitLabel feeTextLabel;
    
    private static final int FIELD_WIDTH = 360;
    private static final int FIELD_HEIGHT = 30;

    /**
     * Creates a new {@link SignMessagePanel}.
     */
    public CSMigrateAssetsPanel(BitcoinController bitcoinController, MultiBitFrame mainFrame) {
        this.bitcoinController = bitcoinController;
        this.controller = this.bitcoinController;
        this.mainFrame = mainFrame;

        setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        initUI();
        
        walletBusyChange(this.bitcoinController.getModel().getActivePerWalletModelData().isBusy());
        this.bitcoinController.registerWalletBusyListener(this);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setMinimumSize(new Dimension(800, 480));
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setOpaque(false);
        mainPanel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        String[] keys = new String[] { "showExportPrivateKeysPanel.walletPasswordPrompt", "sendBitcoinPanel.addressLabel",
                "verifyMessagePanel.message.text", "verifyMessagePanel.signature.text" };

        int stentWidth = MultiBitTitledPanel.calculateStentWidthForKeys(controller.getLocaliser(), keys, this)
                + ExportPrivateKeysPanel.STENT_DELTA;

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        JPanel instructionsPanel = createInstructionsPanel(stentWidth);
        mainPanel.add(instructionsPanel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.1;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(MultiBitTitledPanel.createStent(12, 12), constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        JPanel walletPanel = createWalletPanel(stentWidth);
        mainPanel.add(walletPanel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.1;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(MultiBitTitledPanel.createStent(12, 12), constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        JPanel messagePanel = createAddressPanel(stentWidth);
        mainPanel.add(messagePanel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.1;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(MultiBitTitledPanel.createStent(12, 12), constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.weightx = 0.4;
        constraints.weighty = 0.06;
        constraints.anchor = GridBagConstraints.LINE_START;
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, constraints);

        messageLabel1 = new MultiBitLabel(" ");
        messageLabel1.setOpaque(false);
        messageLabel1.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));
        messageLabel1.setHorizontalAlignment(JLabel.LEADING);
        messageLabel1.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.weighty = 0.06;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(messageLabel1, constraints);

        messageLabel2 = new MultiBitLabel(" ");
        messageLabel2.setOpaque(false);
        messageLabel2.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));
        messageLabel2.setHorizontalAlignment(JLabel.LEADING);
        messageLabel2.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.weighty = 0.06;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(messageLabel2, constraints);

//        Action helpAction;
//        if (ComponentOrientation.LEFT_TO_RIGHT == ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())) {
//            helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_ICON_FILE,
//                    "multiBitFrame.helpMenuText", "multiBitFrame.helpMenuTooltip", "multiBitFrame.helpMenuText",
//                    HelpContentsPanel.HELP_SIGN_AND_VERIFY_MESSAGE_URL);
//        } else {
//            helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_RTL_ICON_FILE,
//                    "multiBitFrame.helpMenuText", "multiBitFrame.helpMenuTooltip", "multiBitFrame.helpMenuText",
//                    HelpContentsPanel.HELP_SIGN_AND_VERIFY_MESSAGE_URL);
//        }   
//               HelpButton helpButton = new HelpButton(helpAction, controller);
//        helpButton.setText("");
//
//        String tooltipText = HelpContentsPanel.createMultilineTooltipText(new String[] { controller.getLocaliser().getString(
//                "multiBitFrame.helpMenuTooltip") });
//        helpButton.setToolTipText(tooltipText);
//        helpButton.setHorizontalAlignment(SwingConstants.LEADING);
//        helpButton.setBorder(BorderFactory.createEmptyBorder(0, AbstractTradePanel.HELP_BUTTON_INDENT,
//                AbstractTradePanel.HELP_BUTTON_INDENT,  AbstractTradePanel.HELP_BUTTON_INDENT));
//        helpButton.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
//
//        constraints.fill = GridBagConstraints.NONE;
//        constraints.gridx = 0;
//        constraints.gridy = 9;
//        constraints.weightx = 1;
//        constraints.weighty = 0.1;
//        constraints.gridwidth = 1;
//        constraints.gridheight = 1;
//        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
//        mainPanel.add(helpButton, constraints);

        JLabel filler2 = new JLabel();
        filler2.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.gridwidth = 1;
        constraints.weightx = 1;
        constraints.weighty = 100;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(filler2, constraints);

        JScrollPane mainScrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainScrollPane.getViewport().setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        mainScrollPane.getViewport().setOpaque(true);
        mainScrollPane.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        mainScrollPane.getHorizontalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);

        add(mainScrollPane, BorderLayout.CENTER);
    }

    private JPanel createInstructionsPanel(int stentWidth) {
        MultiBitTitledPanel instructionsPanel = new MultiBitTitledPanel(controller.getLocaliser().getString(
                "migrateAssetsPanel.explainTitle"), ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        MultiBitTitledPanel.addLeftJustifiedTextAtIndent(
                controller.getLocaliser().getString("migrateAssetsPanel.instructions.text1"), 3, instructionsPanel);

        feeTextLabel = MultiBitTitledPanel.addLeftJustifiedTextAtIndent( " ", 4, instructionsPanel);
		 
        return instructionsPanel;
    }
    
    public void updateFeeTextLabel() {
	String fee = "0 BTC";
	Wallet w = this.bitcoinController.getModel().getActiveWallet();
	if (w!=null) {
	    BigInteger n = CSMiscUtils.calcMigrationFeeSatoshis(this.bitcoinController, w);
	    if (n != null) {
		String s = Utils.bitcoinValueToFriendlyString(n);
		fee = s + " BTC";
	    }
	}
	String feeText = controller.getLocaliser().getString("migrateAssetsPanel.instructions.text2", new Object[]{ fee } );
	feeTextLabel.setText(feeText);
    }
    
    private JPanel createWalletPanel(int stentWidth) {
        MultiBitTitledPanel inputWalletPanel = new MultiBitTitledPanel(controller.getLocaliser().getString(
                "showExportPrivateKeysPanel.walletPasswordPrompt"), ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        GridBagConstraints constraints = new GridBagConstraints();

        walletTextLabel = MultiBitTitledPanel.addLeftJustifiedTextAtIndent(
                controller.getLocaliser().getString("signMessagePanel.wallet.text"), 3, inputWalletPanel);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        inputWalletPanel.add(MultiBitTitledPanel.createStent(stentWidth, (int) (ExportPrivateKeysPanel.STENT_HEIGHT * 0.5)), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 5;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        inputWalletPanel.add(MultiBitTitledPanel.createStent(MultiBitTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS), constraints);
        
        JPanel filler3 = new JPanel();
        filler3.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 7;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        inputWalletPanel.add(filler3, constraints);

        walletPasswordPromptLabel = new MultiBitLabel(controller.getLocaliser().getString("showExportPrivateKeysPanel.walletPasswordPrompt"));
        walletPasswordPromptLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 8;
        constraints.weightx = 0.3;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        inputWalletPanel.add(walletPasswordPromptLabel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 8;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        inputWalletPanel.add(MultiBitTitledPanel.createStent(MultiBitTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS),
                constraints);

        walletPasswordField = new JPasswordField(24);
        walletPasswordField.setMinimumSize(new Dimension(200, 20));
        walletPasswordField.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 8;
        constraints.weightx = 0.3;
        constraints.weighty = 0.6;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        inputWalletPanel.add(walletPasswordField, constraints);

        JPanel filler4 = new JPanel();
        filler4.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 9;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        inputWalletPanel.add(filler4, constraints);

        return inputWalletPanel;
    }

    private JPanel createAddressPanel(int stentWidth) {
        MultiBitTitledPanel messagePanel = new MultiBitTitledPanel(controller.getLocaliser().getString(
                "migrateAssetsPanel.address.title"), ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        GridBagConstraints constraints = new GridBagConstraints();
        MultiBitTitledPanel.addLeftJustifiedTextAtIndent(
                controller.getLocaliser().getString("migrateAssetsPanel.instructions.text3"), 3, messagePanel);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(MultiBitTitledPanel.createStent(stentWidth, ExportPrivateKeysPanel.STENT_HEIGHT), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 6;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        messagePanel.add(MultiBitTitledPanel.createStent(MultiBitTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS), constraints);

        JPanel filler3 = new JPanel();
        filler3.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 8;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(filler3, constraints);

        addressLabel = new MultiBitLabel(controller.getLocaliser().getString("sendBitcoinPanel.addressLabel"));
        addressLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 9;
        constraints.weightx = 0.3;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        messagePanel.add(addressLabel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 9;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        messagePanel.add(MultiBitTitledPanel.createStent(MultiBitTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS),
                constraints);

        //addressTextArea = new MultiBitTextField("", 30, controller);
        JTextField aTextField = new JTextField();
        addressTextArea = new MultiBitTextArea("", 1, 30, controller);
        addressTextArea.setBorder(aTextField.getBorder());

        addressTextArea.setMinimumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        addressTextArea.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        addressTextArea.setMaximumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        addressTextArea.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
	
	addressTextArea.addKeyListener(new CoinSparkAddressListener());
	
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 3;
        constraints.gridy = 9;
        constraints.weightx = 0.3;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(addressTextArea, constraints);

	ImageIcon tickIcon = ImageLoader.createImageIcon(ImageLoader.TICK_ICON_FILE);
        tickLabel = new JLabel(tickIcon);
        tickLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("showExportPrivateKeysPanel.theTwoPasswordsMatch")));
        tickLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        tickLabel.setVisible(false);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 4;
        constraints.gridy = 9;
        constraints.weightx = 0.1;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(tickLabel, constraints);

	
	
        JPanel filler4 = new JPanel();
        filler4.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 10;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(filler4, constraints);

	addressLabel2 = new MultiBitLabel(controller.getLocaliser().getString("migrateAssetsPanel.confirmAddressLabel"));
        addressLabel2.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 11;
        constraints.weightx = 0.3;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        messagePanel.add(addressLabel2, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 9;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        messagePanel.add(MultiBitTitledPanel.createStent(MultiBitTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS),
                constraints);

        //addressTextArea = new MultiBitTextField("", 30, controller);
        JTextField aTextField2 = new JTextField();
        addressTextArea2 = new MultiBitTextArea("", 1, 30, controller);
        addressTextArea2.setBorder(aTextField2.getBorder());

        addressTextArea2.setMinimumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        addressTextArea2.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        addressTextArea2.setMaximumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        addressTextArea2.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
	
	addressTextArea2.addKeyListener(new CoinSparkAddressListener());

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 3;
        constraints.gridy = 11;
        constraints.weightx = 0.3;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(addressTextArea2, constraints);

	
	tickLabel2 = new JLabel(tickIcon);
        tickLabel2.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("showExportPrivateKeysPanel.theTwoPasswordsMatch")));
        tickLabel2.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        tickLabel2.setVisible(false);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 4;
        constraints.gridy = 11;
        constraints.weightx = 0.1;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(tickLabel2, constraints);
	
	/*
        JTextField secondTextField = new JTextField();
        messageTextArea = new MultiBitTextArea("", AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS + 1, 20, controller);
        messageTextArea.setBorder(secondTextField.getBorder());
        
        messageTextArea.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        
        final JScrollPane messageScrollPane = new JScrollPane(messageTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        messageScrollPane.setOpaque(true);
        messageScrollPane.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        messageScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1)); 
        messageScrollPane.getViewport().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (messageScrollPane.getVerticalScrollBar().isVisible()) {
                    messageScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));          
                } else {
                    messageScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));              
                }
            }
        });
        messageScrollPane.setMinimumSize(new Dimension(FIELD_WIDTH, (AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS + 1) * FIELD_HEIGHT + 6));
        messageScrollPane.setPreferredSize(new Dimension(FIELD_WIDTH, (AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS + 1) * FIELD_HEIGHT + 6));
        messageScrollPane.getHorizontalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);
        messageScrollPane.getVerticalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);
        if (messageScrollPane.getVerticalScrollBar().isVisible()) {
            messageScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));          
        } else {
            messageScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));              
        }

        messageLabel = new MultiBitLabel(controller.getLocaliser().getString("verifyMessagePanel.message.text"));
        messageLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 11;
        constraints.weightx = 0.3;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        messagePanel.add(messageLabel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 11;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        messagePanel.add(MultiBitTitledPanel.createStent(MultiBitTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS),
                constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 3;
        constraints.gridy = 11;
        constraints.weightx = 0.3;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(messageScrollPane, constraints);

        JPanel filler5 = new JPanel();
        filler5.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 12;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(filler5, constraints);

        signatureLabel = new MultiBitLabel(controller.getLocaliser().getString("verifyMessagePanel.signature.text"));
        signatureLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 13;
        constraints.weightx = 0.3;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        messagePanel.add(signatureLabel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 13;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        messagePanel.add(MultiBitTitledPanel.createStent(MultiBitTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS),
                constraints);

        JTextField anotherTextField = new JTextField();
        signatureTextArea = new MultiBitTextArea("", 2, 30, controller);
        signatureTextArea.setBorder(anotherTextField.getBorder());
        signatureTextArea.setMinimumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT * 2));
        signatureTextArea.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT * 2));
        signatureTextArea.setMaximumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT * 2));
        signatureTextArea.setLineWrap(true);
        signatureTextArea.setEditable(false);
        signatureTextArea.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 3;
        constraints.gridy = 13;
        constraints.weightx = 0.3;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(signatureTextArea, constraints);
*/
        return messagePanel;
    }
 

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEADING);
        buttonPanel.setLayout(flowLayout);
        buttonPanel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        migrateAssetsSubmitAction = new CSMigrateAssetsSubmitAction(this.bitcoinController, mainFrame, this, ImageLoader.fatCow16(ImageLoader.FATCOW.baggage_cart_box));
        submitButton = new MultiBitButton(migrateAssetsSubmitAction, controller);
        submitButton.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        buttonPanel.add(submitButton);

	submitButton.setEnabled(false);
        
        clearAllButton = new MultiBitButton(controller.getLocaliser().getString("CSMigrateAssetsPanel.clearAll.text"));
        clearAllButton.setToolTipText(controller.getLocaliser().getString("CSMigrateAssetsPanel.clearAll.tooltip"));
        clearAllButton.setIcon(ImageLoader.createImageIcon(ImageLoader.DELETE_ICON_FILE));
        clearAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                walletPasswordField.setText("");
                addressTextArea.setText("");
		addressTextArea2.setText("");
		tickLabel.setVisible(false);
		tickLabel2.setVisible(false);
//                messageTextArea.setText("");
//                signatureTextArea.setText("");
                messageLabel1.setText(" ");
                messageLabel2.setText(" ");
		
		submitButton.setEnabled(false); // wouldn't have to do this if textarea triggered update.
            }
        }); 
        clearAllButton.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        buttonPanel.add(clearAllButton);

        return buttonPanel;
    }

    @Override
    public void displayView(DisplayHint displayHint) {
        // If it is a wallet transaction change no need to update.
        if (DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED == displayHint) {
            return;
        }
       
        boolean walletPasswordRequired = false;
        if (this.bitcoinController.getModel().getActiveWallet() != null && this.bitcoinController.getModel().getActiveWallet().getEncryptionType() == EncryptionType.ENCRYPTED_SCRYPT_AES) {
            walletPasswordRequired = true;
        }
        enableWalletPassword(walletPasswordRequired);

        walletBusyChange(this.bitcoinController.getModel().getActivePerWalletModelData().isBusy());
        
        messageLabel1.setText(" ");
        messageLabel2.setText(" ");
	
	updateFeeTextLabel();
    }

    @Override
    public void navigateAwayFromView() {
    }

    public void setMessageText1(String message1) {
        if (messageLabel1 != null) {
            messageLabel1.setText(message1);
        }
    }

    public String getMessageText1() {
        if (messageLabel1 != null) {
            return messageLabel1.getText();
        } else {
            return "";
        }
    }

    public void setMessageText2(String message2) {
        if (messageLabel2 != null) {
            messageLabel2.setText(message2);
        }
    }

    public String getMessageText2() {
        if (messageLabel2 != null) {
            return messageLabel2.getText();
        } else {
            return "";
        }
    }

    @Override
    public Icon getViewIcon() {
        return ImageLoader.fatCow16(ImageLoader.FATCOW.baggage_cart_box);
    }

    @Override
    public String getViewTitle() {
        return controller.getLocaliser().getString("migrateAssetsAction.text");
    }

    @Override
    public String getViewTooltip() {
        return controller.getLocaliser().getString("migrateAssetsAction.tooltip");
    }

    @Override
    public View getViewId() {
        return View.COINSPARK_MIGRATE_ASSETS_VIEW;
    }
    
//    public SignMessageSubmitAction getSignMessageSubmitAction() {
//        return signMessageSubmitAction;
//    }

    @Override
    public void walletBusyChange(boolean newWalletIsBusy) {       
        // Update the enable status of the action to match the wallet busy status.
        if (this.bitcoinController.getModel().getActivePerWalletModelData().isBusy()) {
            // Wallet is busy with another operation that may change the private keys - Action is disabled.
            migrateAssetsSubmitAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("multiBitSubmitAction.walletIsBusy", 
                    new Object[]{controller.getLocaliser().getString(this.bitcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey())})));
            migrateAssetsSubmitAction.setEnabled(false);           
        } else {
            // Enable unless wallet has been modified by another process.
            if (!this.bitcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
                migrateAssetsSubmitAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("migrateAssetsAction.tooltip")));
                migrateAssetsSubmitAction.setEnabled(true);
            }
        }
    }
    
    private void enableWalletPassword(boolean enableWalletPassword) {
        // Enable/ disable the wallet password fields.
        walletPasswordField.setEnabled(enableWalletPassword);
        walletPasswordPromptLabel.setEnabled(enableWalletPassword);
        walletTextLabel.setEnabled(enableWalletPassword);
    }

    public MultiBitTextArea getMessageTextArea() {
        return messageTextArea;
    }

    public MultiBitTextArea getAddressTextArea() {
        return addressTextArea;    }

    public MultiBitTextArea getSignatureTextArea() {
        return signatureTextArea;
    }

    public JPasswordField getWalletPasswordField() {
        return walletPasswordField;
    }
    
    
    class CoinSparkAddressListener implements KeyListener {
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
//            char[] address1 = null;
//            char[] address2 = null;
	    String address1 = null;
	    String address2 = null;

            if (addressTextArea != null) {
                address1 = addressTextArea.getText();
            }
            if (addressTextArea2 != null) {
                address2 = addressTextArea2.getText();
            }

	    // Enable the migrate submit button only if both addresses are valid
	    boolean enableMigrateButton = false;
            boolean tickLabelVisible = false;
            if (address1 != null && address2 != null) {
		
		// TODO: Check the address is not a receiving address in the sender's wallet.
		// TODO: Check the bitcoin address is for this network.
		
		// Is the coinspark address valid?
		CoinSparkAddress csa = CSMiscUtils.decodeCoinSparkAddress(address1);
		if (csa != null) {
		    // Can the coinspark address receive assets?
		    int flags = csa.getAddressFlags();
		    if ((flags & CoinSparkAddress.COINSPARK_ADDRESS_FLAG_ASSETS) > 0) {
			String btcAddress = csa.getBitcoinAddress();
			if (btcAddress != null && CSMiscUtils.validateBitcoinAddress(btcAddress, bitcoinController))
			{
			    tickLabelVisible = true;
			}
		    }

		}
		tickLabel.setVisible(tickLabelVisible);
                if (tickLabelVisible && address1.equals(address2)) {
		    tickLabel2.setVisible(true);
		    enableMigrateButton = true;
                } else {
		    tickLabel2.setVisible(false);
		}
            }
	    
	    submitButton.setEnabled(enableMigrateButton);

        }
    }

}