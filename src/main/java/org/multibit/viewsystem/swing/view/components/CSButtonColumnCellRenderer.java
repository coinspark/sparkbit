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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import java.awt.Font;
import javax.swing.Icon;
import org.multibit.viewsystem.swing.ColorAndFontConstants;


/**
 * Adapted from Public domain code below, exact source unknown.
 * http://tips4java.wordpress.com/2009/07/12/table-button-column/
 * http://code.google.com/p/javasnoop/source/browse/trunk/src/com/aspect/snoop/ui/canary/ButtonColumn.java?r=33 
 */
public class CSButtonColumnCellRenderer extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {
    private static final long serialVersionUID = 8888888888L;
    JTable table;
    JButton editButton;
    JButton renderButton;
    Action action;
    Object editorValue;
    boolean isEditing;
    public boolean useAlternateRowColors;  // set this to true to stripe based on table colors.

    public CSButtonColumnCellRenderer(JTable table, Action action) {
	this.table = table;
	this.action = action;

	renderButton = new JButton();
	renderButton.setContentAreaFilled(false);
	renderButton.setBorderPainted(false);
	renderButton.setFocusPainted(false);
	renderButton.setBorder(null);   // this removes some padding
	renderButton.setMargin(new Insets(0, 0, 0, 0));	// this should also remove padding

	editButton = new JButton();
	if (action != null) editButton.addActionListener(this);
	editButton.setContentAreaFilled(false);
	editButton.setBorderPainted(false);
	editButton.setFocusPainted( action!=null ); // we want to show focus when action is taking place
	editButton.setBorder(null);   // this removes some padding
	editButton.setMargin(new Insets(0, 0, 0, 0));	// this should also remove padding
    }
    
    public void setFont(Font f) {
	renderButton.setFont(f);
	editButton.setFont(f);
    }

		    // Set position of text, relative to icon, so JButton.LEFT means put text to the left of icon.
    // and JButton.CENTE would center (even if behind text)
    // SetVerticalTextPosition(JButton.TOP); would put text on top of icon
    public void setHorizontalTextPosition(int textPosition) {
	renderButton.setHorizontalTextPosition(textPosition);
	editButton.setHorizontalTextPosition(textPosition);
    }

		    // setHorizontalAlignment and setVerticalAlignment place button in its container
    // We need to remove the padding between text and label otherwise it can look strange
    public void setHorizontalAlignment(int alignment) {
	renderButton.setHorizontalAlignment(alignment);
	editButton.setHorizontalAlignment(alignment);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	if (value == null) {
	    renderButton.setText(null);
	    renderButton.setIcon(null);
	    renderButton.setToolTipText(null);
	} else {
	    HashMap<String, Object> map = (HashMap<String, Object>) value;
	    Icon icon = (Icon) (map.get("icon"));
	    String text = (String) (map.get("text"));
	    String tooltip = (String) (map.get("tooltip"));
	    renderButton.setIcon(icon);
	    renderButton.setText(text);
	    renderButton.setToolTipText(tooltip);
	}
	
	if (useAlternateRowColors) {
	    if (isSelected) {
		renderButton.setBackground(table.getSelectionBackground());
		renderButton.setForeground(table.getSelectionForeground());
	    } else {
		renderButton.setForeground(table.getForeground());
		if (row % 2 == 1) {
		    renderButton.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
		} else {
		    renderButton.setBackground(ColorAndFontConstants.ALTERNATE_TABLE_COLOR);
		    renderButton.setOpaque(true);
		}
	    }
	}
	
	return renderButton;
    }

    //@Override
    public Component getTableCellEditorComponent(
	    JTable table, Object value, boolean isSelected, int row, int column) {
	if (value == null) {
	    editButton.setText("");
	    editButton.setIcon(null);
	} else {
	    HashMap<String, Object> map = (HashMap<String, Object>) value;
	    Icon icon = (Icon) (map.get("icon"));
	    String text = (String) (map.get("text"));
	    String tooltip = (String) (map.get("tooltip"));
	    renderButton.setIcon(icon);
	    renderButton.setText(text);
	    renderButton.setToolTipText(tooltip);
	}

	this.editorValue = value;
	return editButton;
    }

    @Override
    public Object getCellEditorValue() {
	return editorValue;
    }

    /*
     *	The button has been pressed. Stop editing and invoke the custom Action with useful data.
     */
    public void actionPerformed(ActionEvent e) {

	int row = table.convertRowIndexToModel(table.getEditingRow());
	fireEditingStopped();

	if (action == null) {
	    return;
	}

	ActionEvent event = new ActionEvent(
		table,
		ActionEvent.ACTION_PERFORMED,
		"" + row);
	action.actionPerformed(event);
    }
//
//  Implement MouseListener interface
//
	/*
     *  When the mouse is pressed the editor is invoked. If you then then drag
     *  the mouse to another cell before releasing it, the editor is still
     *  active. Make sure editing is stopped when the mouse is released.
     */

    public void mousePressed(MouseEvent e) {
	if (table.isEditing()
		&& table.getCellEditor() == this) {
	    isEditing = true;
	}
    }

    public void mouseReleased(MouseEvent e) {
	if (isEditing
		&& table.isEditing()) {
	    table.getCellEditor().stopCellEditing();
	}

	isEditing = false;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
}
