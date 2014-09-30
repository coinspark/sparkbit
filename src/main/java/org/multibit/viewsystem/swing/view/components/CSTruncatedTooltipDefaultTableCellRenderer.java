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

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Insets;
import java.awt.FontMetrics;
import javax.swing.JLabel;
import javax.swing.border.*;

/**
 * Set tooltip to the full text if it's truncated.
 * Ideal for a column where the font style is the same i.e. does not change between rows.
 */
public class CSTruncatedTooltipDefaultTableCellRenderer extends DefaultTableCellRenderer {

    private Border customBorder = null;

    public void setCustomBorder(Border customBorder) {
	this.customBorder = customBorder;
    }
    
    public CSTruncatedTooltipDefaultTableCellRenderer() {
	super();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
	    boolean hasFocus,
	    int row, int column) {
	Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	int availableWidth = table.getColumnModel().getColumn(column).getWidth();
	availableWidth -= table.getIntercellSpacing().getWidth();
	Insets borderInsets = getBorder().getBorderInsets(comp);
	availableWidth -= (borderInsets.left + borderInsets.right);
	FontMetrics fm = getFontMetrics(comp.getFont());

	String tip = null;
	if (value != null) {
	    String label = value.toString();
	    if (fm.stringWidth(label) > availableWidth) {
		tip = label;
	    }
	}
	((JLabel) comp).setToolTipText(tip);

	
	// set custom border if it exists
	if (customBorder!=null) {
	    setBorder(customBorder);
	}
	
	return comp;
    }
}
