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

import java.awt.BorderLayout;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.multibit.controller.Controller;
import org.multibit.utils.ImageLoader;
import org.multibit.viewsystem.DisplayHint;
import org.multibit.viewsystem.View;
import org.multibit.viewsystem.Viewable;
import org.multibit.viewsystem.swing.ColorAndFontConstants;
import org.multibit.viewsystem.swing.MultiBitFrame;

import org.multibit.controller.bitcoin.BitcoinController;

import java.awt.event.*;
import javax.swing.JTextArea;
import java.util.Date;
import javax.swing.JScrollPane;
import java.awt.Font;


import org.coinspark.wallet.CSAsset;
import org.coinspark.wallet.CSAssetDatabase;
import org.coinspark.wallet.CSBalanceDatabase;
import org.coinspark.wallet.CSBalance;
import java.math.BigInteger;
import com.google.bitcoin.core.Wallet;
import org.coinspark.wallet.CSTransactionOutput;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;

// Apache license
import org.sparkbit.WrapLayout;

import org.sparkbit.jsonrpc.JSONRPCController;

/**
 * The debug view.
 */
public class CSDeveloperToolsPanel extends JPanel implements Viewable {
    private static final long serialVersionUID = 100352212345057705L;
    
    private Controller controller;
    private final BitcoinController bitcoinController;

    private final MultiBitFrame mainFrame;
    
    private final JTextArea logTextArea;
    
  /**
     * Creates a new {@link CSDebugPanel}.
     */
    public CSDeveloperToolsPanel(BitcoinController bitcoinController, MultiBitFrame mainFrame) {
	this.bitcoinController = bitcoinController;
	this.controller = this.bitcoinController;
	this.mainFrame = mainFrame;

	setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);

	setLayout(new BorderLayout());

	JPanel buttonPanel = new JPanel();
	WrapLayout wrapLayout = new WrapLayout(WrapLayout.LEFT);
	buttonPanel.setLayout(wrapLayout);
	
        JButton clearLogButton = new JButton("Clear Log");
        clearLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logTextArea.setText("");
            }
        });
        buttonPanel.add(clearLogButton);
        
	JButton testButton = new JButton("wallet.test()");
	testButton.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		walletTestButtonPressed();
	    }
	});
	
	buttonPanel.add(testButton);
	
	testButton = new JButton("wallet.toString()");
	testButton.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		walletToStringButtonPressed();
	    }
	});
	
	buttonPanel.add(testButton);
	
	testButton = new JButton("Print AssetIDs");
	testButton.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent evt) {
		checkWalletAssets();
	    }
	});
	buttonPanel.add(testButton);


	
	testButton = new JButton("Toggle JSONRPC Server");
	testButton.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent evt) {
		jsonToggleButtonPressed();
	    }
	});
	buttonPanel.add(testButton);

	testButton = new JButton("Print JSONRPC Server");
	testButton.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent evt) {
		jsonPrintButtonPressed();
	    }
	});
	buttonPanel.add(testButton);
	
	
	logTextArea = new JTextArea();
	logTextArea.setEditable(false);	
	logTextArea.setFont(new Font("monospaced", Font.PLAIN, 12));
	JScrollPane scrollPane = new JScrollPane(logTextArea);
	
	add(buttonPanel, BorderLayout.NORTH);
	add(scrollPane, BorderLayout.CENTER);

	mainFrame.pack();
	
	setupJSONRPC();
    }

    public void setupJSONRPC() {
	// Create a new JSON-RPC 2.0 request dispatcher
	//Dispatcher
//	dispatcher =  new Dispatcher();
//	// Register the "echo", "getDate" and "getTime" handlers with it
//	dispatcher.register(new EchoHandler());
	
	JSONRPCController.INSTANCE.initialize(bitcoinController);
    }

    public void jsonPrintButtonPressed() {
	logTextArea.append("=============================================================\n");
	logTextArea.append("toggle jsonrpc server invoked at : " + new Date() + "\n");
	logTextArea.append("=============================================================\n");

	JSONRPCController jc = JSONRPCController.INSTANCE;
	logTextArea.append(jc.toString());
    }
    
    public void jsonToggleButtonPressed() {
	logTextArea.append("=============================================================\n");
	logTextArea.append("toggle jsonrpc server invoked at : " + new Date() + "\n");
	logTextArea.append("=============================================================\n");

	JSONRPCController jc = JSONRPCController.INSTANCE;
	if (!jc.shouldRunServer()) {
	    logTextArea.append("Not starting JSON server.\nConfig file jsonrpc.properties needs to have JSON RPC server enabled:\nrpcssl=true");
	logTextArea.append("\n\n");
	    return;
	}
	
	if (jc.canStartServer()) {
	    logTextArea.append("Attempting to start JSON server...\n");
	    boolean b = jc.startServer();
	    logTextArea.append(b ? "ok" : "failed");
	} else if (jc.canStopServer()) {
	    logTextArea.append("Attempting to stop JSON server...\n");
	    boolean b = jc.stopServer();
	    logTextArea.append(b ? "ok" : "failed");
	}
	
	logTextArea.append("\n\n");
    }

    public void walletTestButtonPressed() {
	logTextArea.append("=============================================================\n");
	logTextArea.append("wallet.test() invoked : " + new Date() + "\n");
	logTextArea.append("=============================================================\n");
	String s = this.bitcoinController.getModel().getActiveWallet().test();
        logTextArea.append(s);
	logTextArea.append("\n\n");
    }

    public void walletToStringButtonPressed() {
	logTextArea.append("=============================================================\n");
	logTextArea.append("wallet.toString() invoked : " + new Date() + "\n");
	logTextArea.append("=============================================================\n");

	String s = this.bitcoinController.getModel().getActiveWallet().toString();
	logTextArea.append(s);
	logTextArea.append("\n\n");
    }

    public void checkWalletAssets() {
	logTextArea.append("=============================================================\n");
	logTextArea.append("Checking wallet assets: " + new Date() + "\n");
	logTextArea.append("=============================================================\n");

	Wallet wallet = bitcoinController.getModel().getActiveWallet();
	if (wallet == null) {
	    logTextArea.append("Wallet is null\n\n");
	    return;
	}
	logTextArea.append( wallet.getDescription() + "\n\n");

	CSAssetDatabase assetDB = wallet.CS.getAssetDB();
	if (assetDB == null) {
	    logTextArea.append("assetDB is null\n\n");
	    return;
	}
	int[] array_ids = assetDB.getAssetIDs();
	if (array_ids == null) {
	    logTextArea.append("getAssetIDs() returned null");
	    logTextArea.append("\n\n");
	    return;
	}
	
	logTextArea.append("getAssetIDs() returned array of length " + array_ids.length + "\n\n");

	if (array_ids.length == 0) {
	    return;
	}
	
	for (int i : array_ids) {
	    logTextArea.append("\n***** Asset ID : " + i + " *****\n");
	    CSAsset asset = assetDB.getAsset(i);
	    if (asset != null) {
		String s = asset.toString();
		logTextArea.append("Asset string : " + s);

		CSBalanceDatabase balanceDB = wallet.CS.getBalanceDB();
			//public CSBalanceDatabase.CSBalanceIterator getAssetBalances(int AssetID)
		//8. Balance information can be obtained using CSBalanceDatabase.CSBalanceIterator class. You can get the list of balances for specific TxOut using getTxOutBalances call and list of balances for specific asset using getAssetBalances call. Please let me know if something is unclear in these classes. Please note the following:
		CSBalanceDatabase.CSBalanceIterator iter = balanceDB.getAssetBalances(i);
		CSBalance balance;
		BigInteger spendable = BigInteger.ZERO;
		BigInteger unconfirmedBalance = BigInteger.ZERO;
		while ((balance = iter.next()) != null) {
		    logTextArea.append("...CSBalance: " + balance + "\n");
		    BigInteger x = balance.getQty();
		    logTextArea.append("......getQty: " + x + "\n");
//		    if (x != null && balance.getState()==CSBalance.CSBalanceState.VALID) spendable = spendable.add(x);
		    logTextArea.append(".......state: " + balance.getState() + "\n");
		    CSTransactionOutput txOut = balance.getTxOut();
		    logTextArea.append("......tx out: " + txOut.toString() + "\n");
		    Transaction parent = txOut.getParentTransaction();
		    if (parent == null) {
			// we should have the hash so try this way
			parent = wallet.getTransaction( txOut.getTxID() );
		    }
		    if (parent != null) {
			TransactionOutput out = parent.getOutput(txOut.getIndex());
			
			// can also check isMature
			boolean isMature = parent.isMature();
		    logTextArea.append(".............isMature = " + isMature + "\n");
			
		    boolean isMine = out.isMine(wallet); // parent.isMine(wallet);
		    logTextArea.append(".............isMine = " + isMine + "\n");
		    boolean canSpend = out.isAvailableForSpending(); // parent.isEveryOwnedOutputSpent(wallet);
		    logTextArea.append(".............isAvailableForSpending = " + canSpend + "\n");
//		    boolean sent = out.is parent.sent(wallet);
//		    logTextArea.append(".............sent from this wallet = " + sent + "\n");
		    if (isMine && canSpend && x != null && balance.getState()==CSBalance.CSBalanceState.VALID) spendable = spendable.add(x);
		    }
		}
		logTextArea.append("...Sum of VALID CSBalance objects is: " + spendable + "\n");
	    } else {
		logTextArea.append("...getAsset() returned null\n");
	    }
	}

    }
		

    @Override
    public void navigateAwayFromView() {
    }

    @Override
    public void displayView(DisplayHint displayHint) {        
    }
       
    @Override
    public Icon getViewIcon() {
        return ImageLoader.fatCow16(ImageLoader.FATCOW.toolbox);
    }

    @Override
    public String getViewTitle() {
        return controller.getLocaliser().getString("developerToolsAction.text");
    }
    
    @Override
    public String getViewTooltip() {
        return controller.getLocaliser().getString("developerToolsAction.tooltip");
    }

    @Override
    public View getViewId() {
        return View.COINSPARK_DEVELOPER_TOOLS_VIEW;
    }
}
