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
package org.multibit.viewsystem.swing.view.components;

import java.awt.Color;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.JList;
import java.awt.Component;
import java.awt.Font;
import javax.swing.UIManager;

// TODO: Better to use an interface...
import org.multibit.model.bitcoin.WalletAssetComboBoxItem;

/**
 * Render disabled items differently
 */
public class CSDisabledItemsRenderer extends BasicComboBoxRenderer {

    @Override
    public Component getListCellRendererComponent(JList list,
	    Object value,
	    int index,
	    boolean isSelected,
	    boolean cellHasFocus)
    {
	Component c = super.getListCellRendererComponent (list, value, index, isSelected, cellHasFocus);
//	if (isSelected) {
//	    setBackground(list.getSelectionBackground());
//	    setForeground(list.getSelectionForeground());
//	} else {
//	    setBackground(list.getBackground());
//	    setForeground(list.getForeground());
//	}
	if (((WalletAssetComboBoxItem)value).isDisabled()) {
	    //setBackground(list.getBackground());
	    Font f = new Font(null, Font.ITALIC, 12);
	    c.setFont(f);
	    c.setForeground(UIManager.getColor("Label.disabledForeground"));
	    c.setBackground(list.getBackground());
	} else {
	    //c.setFo
//	    	Font f = new Font(null, Font.ITALIC, 12);
//		f = c.getFont();
//		c.setFont(f);
//	    c.set
	  //  c.setBackground(Color.red);
	   // c.setForeground(UIManager.getColor("Label.disabledForeground"));
	}
//	setFont(list.getFont());
//	setText((value == null) ? "" : value.toString());
	return c;
    }

}
