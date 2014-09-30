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
import java.awt.Font;
import java.util.HashMap;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import org.multibit.viewsystem.swing.ColorAndFontConstants;
import java.awt.Insets;
import java.awt.FontMetrics;
import javax.swing.JLabel;

/**
 *
 */
public class CSCustomFontDefaultTableCellRenderer extends DefaultTableCellRenderer {

    private Font customFont = null;

    public final void setCustomFont(Font customFont) {
	this.customFont = customFont;
    }

    public CSCustomFontDefaultTableCellRenderer(Font font) {
	super();
	setCustomFont(font);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
	    boolean hasFocus,
	    int row, int column) {
        if (value==null) {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);    
        }
	HashMap<String, Object> map = (HashMap<String, Object>) value;
	String label = null;
	if (map!=null) {
	    label = (String) map.get("label");
	}
	Component comp = super.getTableCellRendererComponent(table, label, isSelected, hasFocus, row, column);

	Font f = (Font) map.get("font");
	if (f != null) {
	    comp.setFont(f);
	} else if (customFont != null) {
	    comp.setFont(customFont);
	}
	comp.setForeground(ColorAndFontConstants.TEXT_COLOR);
	
	String tip = null;
	Boolean truncatedTooltip = (Boolean) map.get("truncatedTooltip");
	if (label != null && truncatedTooltip != null && truncatedTooltip) {
	    int availableWidth = table.getColumnModel().getColumn(column).getWidth();
	    availableWidth -= table.getIntercellSpacing().getWidth();
	    Insets borderInsets = getBorder().getBorderInsets(comp);
	    availableWidth -= (borderInsets.left + borderInsets.right);
	    FontMetrics fm = getFontMetrics(comp.getFont());
	    if (fm.stringWidth(label) > availableWidth) {
		tip = label;
	    }
	}
	((JLabel) comp).setToolTipText(tip);

	return comp;
    }
}
