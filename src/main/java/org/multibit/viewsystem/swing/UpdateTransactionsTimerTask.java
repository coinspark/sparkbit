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

import java.util.TimerTask;
import javax.swing.SwingUtilities;

import org.multibit.controller.Controller;
import org.multibit.viewsystem.DisplayHint;
import org.multibit.viewsystem.View;
import org.multibit.viewsystem.swing.view.panels.ShowTransactionsPanel;

public class UpdateTransactionsTimerTask extends TimerTask {
    private Controller controller;
    private ShowTransactionsPanel transactionsPanel;
    private MultiBitFrame mainFrame;

    private boolean updateTransactions = false;
    private boolean isCurrentlyUpdating = false;


    public UpdateTransactionsTimerTask(Controller controller, final ShowTransactionsPanel transactionsPanel,
            MultiBitFrame mainFrame) {
        this.controller = controller;
        this.transactionsPanel = transactionsPanel;
        this.mainFrame = mainFrame;
    }

    @Override
    public void run() {
        // If still updating from the last time, skip this firing.
        // Timer thread.
        if (!isCurrentlyUpdating) {
            // If viewing transactions, refresh the screen so that transaction
            // confidence icons can update.
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    boolean updateThisTime = false;
                    if (updateTransactions) {
                        updateTransactions = false;
                        updateThisTime = true;
                    }

                    if (updateThisTime) {
                        mainFrame.updateHeader();
                        if (controller.getCurrentView() == View.TRANSACTIONS_VIEW) {
                            // Swing thread.
                            isCurrentlyUpdating = true;
                            try {
                                transactionsPanel.displayView(DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED);
                            } finally {
                                // Swing thread.
                                isCurrentlyUpdating = false;
                            }
                        }
                    }
                }
            });
        }
    }

    public boolean isUpdateTransactions() {
        // Clone before return.
        return updateTransactions ? true : false;
    }

    public void setUpdateTransactions(boolean updateTransactions) {
        this.updateTransactions = updateTransactions;
    }
}
