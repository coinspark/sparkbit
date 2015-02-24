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

import org.multibit.model.bitcoin.BitcoinModel;
import com.google.bitcoin.core.Utils;
import java.awt.Color;
import java.text.DecimalFormat;
import javax.swing.ButtonGroup;
import javax.swing.JTextField;

import java.text.DecimalFormat;
import java.text.ParseException;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter; 
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import org.multibit.viewsystem.swing.view.components.FontSizer;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import org.multibit.viewsystem.swing.ColorAndFontConstants;
import static org.multibit.viewsystem.swing.view.panels.AbstractTradePanel.TEXTFIELD_VERTICAL_DELTA;
import java.text.NumberFormat;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.GridBagConstraints;
import java.math.RoundingMode;

/**
 *
 */
public class CSSendAssetPanel extends javax.swing.JPanel {

    private DecimalFormat amountFormat;
    private int numberOfDecimalPlaces;
    private GridBagConstraints fmtLeftLabelConstraints;
    private GridBagConstraints amountAssetTextfieldConstraints;
    
    public int getNumberOfDecimalPlaces() {
	return numberOfDecimalPlaces;
    }
    
    // Set the number formatter on the amount text field, based on number of decimal places
    // requested in multiple field of asset.
    public void setDecimalPlaces(int n) {
	numberOfDecimalPlaces = n;
	
	StringBuilder sb = new StringBuilder("####################");
	if (n > 0) {
	    sb.append(".");
	    for (int i = 0; i < n; i++) {
		sb.append("#");
	    }
	}
	String fmt = sb.toString();

	AbstractFormatter formatter = null;
	
	// Force the number of decimal places to be visible.
	// We use our own format string, rather than DecimalFormat() to avoid locale display
	// such as commas as part of number e.g. 2,000,000.1234
	amountFormat = new DecimalFormat(fmt);
	amountFormat.setMinimumFractionDigits(n);
	amountFormat.setMaximumFractionDigits(n);
	amountFormat.setRoundingMode(RoundingMode.DOWN);

	formatter = new NumberFormatter(amountFormat);
	
	//((NumberFormatter)formatter).setCommitsOnValidEdit(true);
	//((NumberFormatter)formatter).setAllowsInvalid(false); // if false, no period . allowed
	
        //((NumberFormatter)formatter).setValueClass(.getClass());
	JFormattedTextField.AbstractFormatterFactory tf = new DefaultFormatterFactory(formatter, formatter, formatter);
	amountAssetTextField.setFormatterFactory(tf);
	
	// FEE LABEL
	String miningFee = Utils.bitcoinValueToPlainString(BitcoinModel.SEND_MINIMUM_FEE);
	String transferAmount = Utils.bitcoinValueToPlainString(BitcoinModel.COINSPARK_SEND_MINIMUM_AMOUNT);
	//btcFeeLabel.setText("Transfer amount (" + transferAmount + " BTC) and mining fee (" + miningFee + " BTC) applies.");
	btcFeeLabel.setText("Bitcoin transfer amount (" + transferAmount + " BTC) and transaction fee (" + miningFee + " BTC) applies.");
    }
    

    // This resizes the panel correctly
    public void refreshPreferredLayoutSize() {
	Dimension d = getLayout().preferredLayoutSize(this);
// 	System.out.println("preferredLayoutSize = " + d);
	setPreferredSize(d);
   }
    
    public void formatAmountToDecimalPlaces() {
	if (amountFormat != null) {
	    try {
		amountAssetTextField.commitEdit();
		// commitEdit() doesn't seem to work, which is we the parse and set the value,
		// to force the formatter to do its work and update the value and text.
		
		String s = amountAssetTextField.getText();
		Number n = amountFormat.parse(s);
		amountAssetTextField.setValue(n);
	    } catch (ParseException pce) {
	    }
	}
    }

    public void setAmountFmtLeftLabel(String s) {
	amountFmtLeftLabel.setText(s);
	
	if (s==null || s.equals("")) {
	    remove(amountFmtLeftLabel); 
	    
	    // Stretch to left
	    GridBagLayout layout = (GridBagLayout)getLayout();
	    GridBagConstraints constraints = (GridBagConstraints) amountAssetTextfieldConstraints.clone();
	    constraints.gridwidth += constraints.gridx;
	    constraints.gridx = 0;
	    layout.setConstraints(amountAssetTextField, constraints);
	} else {
	    // Restore left label, and restore constraints for amount field.
	    add(amountFmtLeftLabel, fmtLeftLabelConstraints);

	    GridBagLayout layout = (GridBagLayout)getLayout();
	    layout.setConstraints(amountAssetTextField, amountAssetTextfieldConstraints);
	}
    }
    
    public void setAmountFmtRightLabel(String s) {
	amountFmtRightLabel.setText(s);
    }

    public void clearAmountTextField() {
	amountAssetTextField.setValue(null);
    }
    
    
    /*
    We need to be able to populate when loading form.
    */
    public void setAmount(String s) {
	if (s==null) s="";
	amountAssetTextField.setText(s);
	try {
	    amountAssetTextField.commitEdit();
	} catch (ParseException e) {
//	    e.printStackTrace();
	}
    }
    
    public String getAmount() {
	try {
	    // e.g. 1.23456 entered and still visible bcomes 1.23 when formatter has two decimal places
	    amountAssetTextField.commitEdit();
	} catch (ParseException e) {
	    return "";
	}
	//System.out.println(" text = " + amountAssetTextField.getText());
	//System.out.println("value = " + amountAssetTextField.getValue());
	return String.valueOf( amountAssetTextField.getValue());
    }
    
    public String getDisplayAmount() {
	try {
	    amountAssetTextField.commitEdit();
	} catch (ParseException e) {
	    return null;
	}
	return amountAssetTextField.getValue().toString();
    }

    public ButtonGroup getPaymentChargeButtonGroup() {
	return paymentChargeButtonGroup;
    }
    
    public JTextField getAssetTextField() {
	return (JTextField) amountAssetTextField;
    }
    
    public boolean isSenderPays() {
	return senderPaysRadioButton.isSelected();
    }
    
    public boolean isRecipientPays() {
	return recipientPaysRadioButton.isSelected();
    }
    
    public void setChargeLabelText(String s) {
	chargeLabel.setText(s);
    }
    
    public void setSenderPaysAmount(String s) {
	senderPaysRadioButton.setText("Sender pays " + s);
    }
    
    public void setRecipientPaysAmount(String s) {
	recipientPaysRadioButton.setText("Recipient pays " + s);
    }
    
    public void updateForInvalidNumber() {
	setSenderPaysAmount("");
	setRecipientPaysAmount("");
	setChargeVisibility(false);
    }
    
    public void setChargeVisibility(boolean b) {
	senderPaysRadioButton.setVisible(b);
	recipientPaysRadioButton.setVisible(b);
	chargeLabel.setVisible(b);
	// Change size of panel based on visibility of charge widgets
	refreshPreferredLayoutSize();
    }
    
//    Dimension computeAssetTextFieldPreferredSize() {
//	int height = getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont()).getHeight() + TEXTFIELD_VERTICAL_DELTA;
//	return new Dimension(300,height);
//    }
    
    /**
     * Creates new form CSSendAssetPanel
     */
    public CSSendAssetPanel() {
	initComponents();
	senderPaysRadioButton.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
	recipientPaysRadioButton.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
	setChargeVisibility(false);
	
	//setKeyBindings();
	
	GridBagLayout layout = (GridBagLayout)getLayout();
	fmtLeftLabelConstraints = layout.getConstraints(amountFmtLeftLabel);
	amountAssetTextfieldConstraints = layout.getConstraints(amountAssetTextField);
		
    }
    
    /* The proper way to use key bindings to perform actions, rather than the low level
    interception of key presses via keylistener.
    However, actionPerformed() only gets invoked if data has changed, after the second
    press of Enter.  Reason is unknown.
    */
    /*
    private void setKeyBindings() {
	ActionMap actionMap = amountAssetTextField.getActionMap();
	int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
	InputMap inputMap = amountAssetTextField.getInputMap(condition);

	String vkEnter = "VK_ENTER";
	KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
	inputMap.put(enter, vkEnter);
	actionMap.put(vkEnter, new EnterKeyAction(vkEnter));
    }

    private class EnterKeyAction extends AbstractAction {

	public EnterKeyAction(String actionCommand) {
	    putValue(ACTION_COMMAND_KEY, actionCommand);
	}

	@Override
	public void actionPerformed(ActionEvent actionEvt) {
	    System.out.println(">>>>>>>>>>>>>" + actionEvt.getActionCommand() + " pressed ");
	    try {
		amountAssetTextField.commitEdit();
		Object o = amountAssetTextField.getValue();
		System.out.println("... o = " + o);
		amountAssetTextField.setValue(o);
	    } catch (ParseException pce) {
	    }

	    if (amountFormat != null) {
		try {
		    amountAssetTextField.commitEdit();
		    String s = amountAssetTextField.getText();
		    Number n = amountFormat.parse(s);

		    System.out.println("... n = " + n);
		    amountAssetTextField.setValue(n);
		} catch (ParseException pce) {
		}
	    }

	}
    }
    */
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        paymentChargeButtonGroup = new javax.swing.ButtonGroup();
        amountAssetTextField = new javax.swing.JFormattedTextField();
        senderPaysRadioButton = new javax.swing.JRadioButton();
        recipientPaysRadioButton = new javax.swing.JRadioButton();
        chargeLabel = new javax.swing.JLabel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        btcFeeLabel = new javax.swing.JLabel();
        amountFmtLeftLabel = new javax.swing.JLabel();
        amountFmtRightLabel = new javax.swing.JLabel();

        java.awt.GridBagLayout layout = new java.awt.GridBagLayout();
        layout.columnWidths = new int[] {0, 10, 0, 10, 0, 10, 0, 10, 0};
        layout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        setLayout(layout);

        amountAssetTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("##################.############"))));
        amountAssetTextField.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        amountAssetTextField.setMinimumSize(new java.awt.Dimension(100, 30));
        amountAssetTextField.setPreferredSize(new java.awt.Dimension(150, 30));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 10000.0;
        add(amountAssetTextField, gridBagConstraints);

        paymentChargeButtonGroup.add(senderPaysRadioButton);
        senderPaysRadioButton.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        senderPaysRadioButton.setSelected(true);
        senderPaysRadioButton.setText("Sender pays");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        add(senderPaysRadioButton, gridBagConstraints);

        paymentChargeButtonGroup.add(recipientPaysRadioButton);
        recipientPaysRadioButton.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        recipientPaysRadioButton.setText("Recipient pays");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        add(recipientPaysRadioButton, gridBagConstraints);

        chargeLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        chargeLabel.setText("Asset contract requires a transfer fee:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        add(chargeLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(filler1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        add(filler2, gridBagConstraints);

        btcFeeLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        btcFeeLabel.setText("FEE LABEL");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        add(btcFeeLabel, gridBagConstraints);

        amountFmtLeftLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        add(amountFmtLeftLabel, gridBagConstraints);

        amountFmtRightLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        amountFmtRightLabel.setText("Units");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        add(amountFmtRightLabel, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFormattedTextField amountAssetTextField;
    private javax.swing.JLabel amountFmtLeftLabel;
    private javax.swing.JLabel amountFmtRightLabel;
    private javax.swing.JLabel btcFeeLabel;
    private javax.swing.JLabel chargeLabel;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.ButtonGroup paymentChargeButtonGroup;
    private javax.swing.JRadioButton recipientPaysRadioButton;
    private javax.swing.JRadioButton senderPaysRadioButton;
    // End of variables declaration//GEN-END:variables
}
