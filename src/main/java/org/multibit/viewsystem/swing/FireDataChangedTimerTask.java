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

import org.multibit.viewsystem.DisplayHint;

/**
 * Condense the many fire data change events into something more manageable for a UI to refresh.
 * @author jim
 *
 */
public class FireDataChangedTimerTask extends TimerTask {
    private MultiBitFrame mainFrame;

    private boolean fireDataChanged = false;
    private boolean isCurrentlyUpdating = false;

    public FireDataChangedTimerTask(MultiBitFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public void run() {
        // If still updating from the last time, skip this firing.
        // Timer thread.
        if (!isCurrentlyUpdating) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    boolean fireDataChangedThisTime = false;
                    if (fireDataChanged) {
                        fireDataChanged = false;
                        fireDataChangedThisTime = true;
                    }

                    if (fireDataChangedThisTime) {
                        // Swing thread.
                        isCurrentlyUpdating = true;
                        try {
                            mainFrame.fireDataChangedUpdateNow(DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED);
                        } finally {
                            // Swing thread.
                            isCurrentlyUpdating = false;
                        }
                    }

                }
            });
        }
    }

    public boolean isFireDataChanged() {
        return fireDataChanged;
    }

    public void setFireDataChanged (boolean fireDataChanged) {
        this.fireDataChanged = fireDataChanged;
    }
}
