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
package org.multibit.viewsystem.swing.view.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * Adapted from Public domain code below, exact source unknown.
 * http://tips4java.wordpress.com/2009/07/12/table-button-column/
 * http://code.google.com/p/javasnoop/source/browse/trunk/src/com/aspect/snoop/ui/canary/ButtonColumn.java?r=33
 */
public class CSDualButtonColumnCellRenderer extends
	AbstractCellEditor
	//DefaultTableCellRenderer
	implements TableCellRenderer, TableCellEditor, ActionListener {

    private static final long serialVersionUID = 228888888888L;
    JPanel panel;
   // JPanel editPanel;
    JTable table;
    JButton editButton;
    JButton editButton2;
    JButton renderButton;
    JButton renderButton2;
    Action action;
    Object editorValue;
    boolean isEditing;

    public CSDualButtonColumnCellRenderer(JTable table, Action action) {
	this.table = table;
	this.action = action;

	renderButton = new JButton();
	renderButton.setContentAreaFilled(false);
	renderButton.setBorderPainted(false);
	renderButton.setFocusPainted(false);
	//renderButton.setBorder(null);   // this removes some padding
	renderButton.setMargin(new Insets(0, 0, 0, 0));	// this should also remove padding
	//renderButton.setVerticalAlignment(SwingConstants.CENTER);
	//renderButton.setVerticalTextPosition(SwingConstants.CENTER);

	renderButton2 = new JButton();
	renderButton2.setContentAreaFilled(false);
	renderButton2.setBorderPainted(false);
	renderButton2.setFocusPainted(false);
	//renderButton2.setBorder(null);   // this removes some padding
	renderButton2.setMargin(new Insets(0, 0, 0, 0));	// this should also remove padding
	//renderButton2.setVerticalAlignment(SwingConstants.CENTER);
	//renderButton2.setVerticalTextPosition(SwingConstants.CENTER);

	editButton = new JButton();
	editButton.addActionListener(this);
	editButton.setContentAreaFilled(false);
	editButton.setBorderPainted(false);
	editButton.setFocusPainted(true);//action!=null ); // we want to show focus when action is taking place
	//editButton.setBorder(null);   // this removes some padding
	editButton.setMargin(new Insets(0, 0, 0, 0));	// this should also remove padding

	editButton2 = new JButton();
	editButton2.addActionListener(this);
	editButton2.setContentAreaFilled(false);
	editButton2.setBorderPainted(false);
	editButton2.setFocusPainted(true); //action!=null ); // we want to show focus when action is taking place
	//editButton2.setBorder(null);   // this removes some padding
	editButton2.setMargin(new Insets(0, 0, 0, 0));	// this should also remove padding

	// TO FIX: Not sure why, but buttons in panel not getting visually updated on click.
	panel = new JPanel();
	//panel.setBorder(new EmptyBorder(0,0,0,0));
	panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0)); // FlowLayout.CENTER by default
	panel.setBackground(table.getBackground());
//	panel.setFocusable(true);

//	editPanel = new JPanel();
//	editPanel.setBackground(table.getBackground());
//	editPanel.add(editButton);
//	editPanel.add(editButton2);
    }

		    // Set position of text, relative to icon, so JButton.LEFT means put text to the left of icon.
    // and JButton.CENTE would center (even if behind text)
    // SetVerticalTextPosition(JButton.TOP); would put text on top of icon
//    public void setHorizontalTextPosition(int textPosition) {
//	renderButton.setHorizontalTextPosition(textPosition);
//	editButton.setHorizontalTextPosition(textPosition);
//    }
//
//		    // setHorizontalAlignment and setVerticalAlignment place button in its container
//    // We need to remove the padding between text and label otherwise it can look strange
//    public void setHorizontalAlignment(int alignment) {
//	renderButton.setHorizontalAlignment(alignment);
//	editButton.setHorizontalAlignment(alignment);
//    }
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	renderButton.setText("");
	renderButton.setIcon(null);
	renderButton2.setText("");
	renderButton2.setIcon(null);
	if (value == null) {
	} else {
	    HashMap<String, Object> map = (HashMap<String, Object>) value;
	    ImageIcon icon = (ImageIcon) (map.get("icon"));
	    String text = (String) (map.get("text"));
//		String tooltip = (String)(map.get("tooltip"));
	    if (text == null) {
		text = "";
	    }
//		if (tooltip==null) tooltip="";
	    if (icon != null) {
		renderButton.setIcon(icon);
	    }
	    if (text != null) {
		renderButton.setText(text);
	    }
	    
	    String text2 = (String) (map.get("text2"));
	    if (text2==null) text2 = "";
	    renderButton2.setText(text2);
	    //renderButton.setToolTipText(tooltip);
	}
	panel.removeAll();
	panel.add(renderButton);
	panel.add(renderButton2);
	//panel.repaint();
	return panel;
    }

    //@Override
    public Component getTableCellEditorComponent(
	    JTable table, Object value, boolean isSelected, int row, int column) {
	editButton.setText("");
	editButton.setIcon(null);
	editButton2.setText("");
	editButton2.setIcon(null);
	if (value == null) {
	} else {
	    HashMap<String, Object> map = (HashMap<String, Object>) value;
	    ImageIcon icon = (ImageIcon) (map.get("icon"));
	    String text = (String) (map.get("text"));
//		String tooltip = (String)(map.get("tooltip"));
	    if (text == null) {
		text = "";
	    }
//		if (tooltip==null) tooltip="";
	    if (icon != null) {
		editButton.setIcon(icon);
	    }
	    if (text != null) {
		editButton.setText(text);
	    }
	    
	    String text2 = (String) (map.get("text2"));
	    if (text2==null) text2 = "";
	    editButton2.setText(text2);
	    editButton2.setToolTipText("hello");
	    //renderButton.setToolTipText(tooltip);
	}
	panel.removeAll();
	panel.add(editButton);
	panel.add(editButton2);
	//panel.repaint();
	this.editorValue = value; // do we need this for anything? prob not.
//	editPanel.repaint();
	return panel;
    }

    @Override
    public Object getCellEditorValue() {
	return editorValue;
    }

    /*
     *	The button has been pressed. Stop editing and invoke the custom Action with useful data.
     */
    public void actionPerformed(ActionEvent e) {
	Object source = e.getSource();
	if (source == editButton) {
//	    System.out.println("EDIT BUTTON 1");
	} else if (source == editButton2) {
//	    System.out.println("EDIT BUTTON 2");
	}
	
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
