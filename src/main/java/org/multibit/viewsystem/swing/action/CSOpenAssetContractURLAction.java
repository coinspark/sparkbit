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
package org.multibit.viewsystem.swing.action;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import org.multibit.model.bitcoin.WalletAssetTableData;
import org.multibit.viewsystem.swing.WalletAssetSummaryTableModel;
import org.coinspark.wallet.CSAsset;
	
/**
 *
 */
// ActionEvent object is a table, and string is the row
public class CSOpenAssetContractURLAction extends AbstractAction {

    public CSOpenAssetContractURLAction() {
	// could store bitcoin controller etc.
    }

    @Override
    public void actionPerformed(ActionEvent e) {
	JTable table = (JTable) e.getSource();
	int row = Integer.parseInt(e.getActionCommand());

	// Custom code for this type of action
	WalletAssetSummaryTableModel model = (WalletAssetSummaryTableModel) table.getModel();
	WalletAssetTableData data = model.getRow(row);
	CSAsset asset = data.getAsset();
//	System.out.println(">>> ASSET STATE: " + asset.getAssetState() + " , CONTRACT STATE: " + asset.getAssetContractState());
	boolean cachedFlag = false;
	String url = null;
	// TODO: Maybe just use the asset state of VALID or REFRESH?
	if (asset.getValidChecked() != null && asset.getValidFailures()==0) {
	    url = asset.getContractUrl();
	} else {
	    String path = data.getAsset().getContractPath();
	    File f = new File(path);
	    if (f.exists()) {
		url = f.toURI().toString();
	    }
	    cachedFlag = true;
	}
	
	if (url==null) return; // something went wrong, so do nothing.
	
//	String msg = null;
//	if (!cachedFlag) {
//	    msg = "Would you like to open the asset contract\nat the following website with your browser?\n\n" + url;
//	} else {
//	    msg = "The asset has not been validated.\n\nWould you like to open the local copy of the asset contract?\n\n" + url;
//	}
	
	try {
	    // TODO: Show dialog to open external URL?
	    if (url != null && Desktop.isDesktopSupported()) {
		//default icon, custom title
//		int n = JOptionPane.showConfirmDialog(
//			table,
//			msg,
//			"Open Asset Contract",
//			JOptionPane.YES_NO_OPTION);
//		if (JOptionPane.YES_OPTION == n) {
		    Desktop.getDesktop().browse(new URI(url));
//		}
	    }
	} catch (Exception use) {
	    use.printStackTrace();
	}

    }
}
