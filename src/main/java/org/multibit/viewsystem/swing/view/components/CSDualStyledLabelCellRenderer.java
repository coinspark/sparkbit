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

import com.jidesoft.swing.StyleRange;
import com.jidesoft.swing.StyledLabel;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseListener;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import javax.swing.AbstractCellEditor;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.Action;

import org.multibit.model.bitcoin.WalletAssetTableData;
import org.multibit.viewsystem.swing.WalletAssetSummaryTableModel;
import java.awt.Graphics;

/**
 *
 */
public class CSDualStyledLabelCellRenderer extends AbstractCellEditor
	//DefaultTableCellRenderer
	implements MouseListener, TableCellRenderer, TableCellEditor {

    private static final long serialVersionUID = 8889990003L;

    JTable table;
    JPanel panel;
    StyledActionLabel label;
    StyledActionLabel label2;
    StyleRange defaultStyle;
    StyleRange defaultStyle2;
    Font defaultFont;
    Object editorValue;
    Action defaultAction;
    Action defaultAction2;

    // Inner class
    class StyledActionLabel extends StyledLabel {
	    public Action action = null;
    }
    
    public CSDualStyledLabelCellRenderer(JTable table, Font defaultFont, StyleRange defaultStyle, StyleRange defaultStyle2, Action defaultAction, Action defaultAction2) {
	this.table = table;
	this.defaultFont = defaultFont;
	this.defaultStyle = defaultStyle;
	this.defaultStyle2 = defaultStyle2;
	this.defaultAction = defaultAction;
	this.defaultAction2 = defaultAction2;

//	label = new StyledActionLabel() ;
//	label2 = new StyledActionLabel();
//	
//	panel = new JPanel();
//	panel.setLayout(new FlowLayout(FlowLayout.LEFT)); // FlowLayout.CENTER by default
//	panel.setBackground(table.getBackground());
//
//	label.addMouseListener(this);
//	label2.addMouseListener(this);
    }

    @Override
    public Object getCellEditorValue() {
	return editorValue;
    }

    //@Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	// Create the components
	label = new StyledActionLabel() ;
	label2 = new StyledActionLabel();
	panel = new JPanel();
	panel.setLayout(new FlowLayout(FlowLayout.LEFT)); // FlowLayout.CENTER by default
	panel.setBackground(table.getBackground());
	
	HashMap<String, Object> map = (HashMap<String, Object>) value;
	String text = (String) map.get("text");
	String text2 = (String) map.get("text2");
	StyleRange style = (StyleRange) map.get("style");
	if (style == null) {
	    style = this.defaultStyle;
	}
	StyleRange style2 = (StyleRange) map.get("style2");
	if (style2 == null) {
	    style2 = this.defaultStyle2;
	}
	// TODO: TOOLTIPS
	if (map.get("noaction") != null) {
	    label.action = null;
	} else {
	    label.action = defaultAction;
	}
	if (map.get("noaction2") != null) {
	    label2.action = null;
	} else {
	    label2.action = defaultAction2;
	}
	
	// Set action listener only if there is action
	if (label.action != null) label.addMouseListener(this);
	if (label2.action != null) label2.addMouseListener(this);
	
	
	label.clearStyleRanges();
	label2.clearStyleRanges();

	label.setText(null);
	label2.setText(null);
//	label.setEnabled(true);
//	label2.setEnabled(true);

	label.setFont(defaultFont);
	label2.setFont(defaultFont);

	if (text != null) {
	    label.setText(text);
	    label.addStyleRange(style);
//	    label.setEnabled(true);
	}

	if (text2 != null) {
	    label2.setText(text2);
	    label2.addStyleRange(style2);
//	    label2.setEnabled(true);
	}

//	panel.removeAll();
	
	if (text != null) {
	    panel.add(label);
	    Graphics g = table.getGraphics(); //label.getGraphics();
	    FontMetrics fontMetrics = g.getFontMetrics(label.getFont());
	    int x = fontMetrics.stringWidth(text);
	    label.setPreferredWidth(x);	    
	}
	if (text2 != null) {
	    panel.add(label2);
	    Graphics g = table.getGraphics(); //label.getGraphics();
	    FontMetrics fontMetrics = g.getFontMetrics(label2.getFont());
	    int x = fontMetrics.stringWidth(text);
	    label2.setPreferredWidth(x);	 
	}
	return panel;
    }

    public Component getTableCellEditorComponent(
	    JTable table, Object value, boolean isSelected, int row, int column) {
	this.editorValue = value;
	boolean hasFocus = true; // we make this up for next call
	return getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    // Mouse listener
    @Override
    public void mousePressed(MouseEvent e) {
	// Stop editing immediately, don't show underline, if there is no action on label pressed.
	StyledActionLabel obj = (StyledActionLabel) e.getSource();
	if (obj.action == null) {
	    fireEditingStopped();
	    return;
	}
	
	int style = obj.getFont().getStyle();
	Color color = obj.getForeground();
	StyleRange oldStyle = obj.getStyleRanges()[0];
	if (oldStyle != null) {
	    style = oldStyle.getFontStyle();
	    color = oldStyle.getFontColor();
	}
	boolean isLabel2 = (obj == label2);
	StyleRange s = new StyleRange(
		style, color,
		Color.YELLOW,
		StyleRange.STYLE_UNDERLINED, Color.BLUE);
	obj.clearStyleRanges();
	obj.addStyleRange(s);
	// TODO: In the future, have two click styles as properties on cell renderer

	    //obj.repaint();
	//panel.repaint();
    }

    // If fireEditingStopped() was invoked in mousePressed(), this method does not get invoked.
    @Override
    public void mouseReleased(MouseEvent e) {
	int row = table.convertRowIndexToModel(table.getEditingRow());
	fireEditingStopped();	// triggers getting a new cell and underline style range is thus removed
	//table.getCellEditor().stopCellEditing();
//	System.out.println("do action!");
//	
	StyledActionLabel obj = (StyledActionLabel)e.getSource();
	Action action = obj.action;
	if (action == null) {
	    return;
	}
	ActionEvent event = new ActionEvent(
		table,
		ActionEvent.ACTION_PERFORMED,
		"" + row);
	action.actionPerformed(event);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
