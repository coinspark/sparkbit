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
package org.multibit.viewsystem.swing.view.panels;

//import com.google.bitcoin.core.Transaction;
//import com.google.bitcoin.core.TransactionConfidence;
//import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
//import org.multibit.viewsystem.CoinSparks.CSAssetsDBDialog;
import com.google.common.eventbus.Subscribe;
import org.sparkbit.SparkBit;
import org.multibit.controller.Controller;
import org.multibit.controller.bitcoin.BitcoinController;
//import org.multibit.exchange.CurrencyConverter;
//import org.multibit.exchange.CurrencyConverterListener;
//import org.multibit.exchange.ExchangeRate;
//import org.multibit.model.bitcoin.WalletTableData;
import org.multibit.model.core.CoreModel;
import org.multibit.utils.DateUtils;
import org.multibit.utils.ImageLoader;
import org.multibit.viewsystem.DisplayHint;
import org.multibit.viewsystem.View;
import org.multibit.viewsystem.Viewable;
import org.multibit.viewsystem.swing.ColorAndFontConstants;
import org.multibit.viewsystem.swing.MultiBitFrame;
//import org.multibit.viewsystem.swing.UpdateTransactionsTimerTask;
//import org.multibit.viewsystem.swing.WalletTableModel;
import org.multibit.viewsystem.swing.action.ExportTransactionsSubmitAction;
import org.multibit.viewsystem.swing.action.HelpContextAction;
import org.multibit.viewsystem.swing.action.ShowTransactionDetailsAction;
import org.multibit.viewsystem.swing.action.CSAddAssetManuallyAction;
import org.multibit.viewsystem.swing.view.components.FontSizer;
import org.multibit.viewsystem.swing.view.components.HelpButton;
import org.multibit.viewsystem.swing.view.components.MultiBitButton;
import org.multibit.viewsystem.swing.view.components.MultiBitLabel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
//import java.util.List;
//import java.util.Timer;
/* CoinSpark START */
import org.coinspark.wallet.CSAsset;
//import org.coinspark.wallet.CSAssetDatabase;
import org.multibit.viewsystem.swing.WalletAssetTableModel;
import org.multibit.model.bitcoin.WalletAssetTableData;
//import org.multibit.viewsystem.swing.view.panels.CSAssetDetailPanel;
import javax.swing.border.Border;
import org.coinspark.wallet.CSEvent;
import org.coinspark.wallet.CSEventBus;
import org.coinspark.wallet.CSEventType;

import com.google.bitcoin.core.Wallet;
import org.coinspark.wallet.CSBalance;
import org.multibit.viewsystem.swing.action.CSRefreshAssetAction;
import org.multibit.viewsystem.swing.action.CSDeleteAssetAction;
import org.multibit.viewsystem.swing.view.components.CSCustomFontDefaultTableCellRenderer;
import org.multibit.viewsystem.swing.view.components.CSTruncatedTooltipDefaultTableCellRenderer; 

import java.awt.event.*;
import org.multibit.utils.CSMiscUtils;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

/* CoinSpark END */

// We don't need to implement CurrencyConverterListener
public class CSShowAssetsPanel extends JPanel implements Viewable {

    private static final long serialVersionUID = 1005108897887842662L;

    private final Controller controller;
    private final BitcoinController bitcoinController;
    
    private MultiBitFrame mainFrame;

    private JTable table;
    private WalletAssetTableModel assetTableModel;
    private Wallet previousWallet;

    private TableRowSorter<TableModel> rowSorter;

    private static final String SPACER = "   "; // 3 spaces
    
    private static final int STATUS_WIDTH_DELTA = 6;

    private static final int TABLE_BORDER = 3;

    private static final int MINIMUM_ICON_HEIGHT = 16;
    
    public static final int HEIGHT_DELTA = 3;

    public static final String PROGRESS_0_ICON_FILE = "/images/circleProgress0.png";
    private static final String PROGRESS_1_ICON_FILE = "/images/circleProgress1.png";
    private static final String PROGRESS_2_ICON_FILE = "/images/circleProgress2.png";
    private static final String PROGRESS_3_ICON_FILE = "/images/circleProgress3.png";
    private static final String PROGRESS_4_ICON_FILE = "/images/circleProgress4.png";
    private static final String PROGRESS_5_ICON_FILE = "/images/circleProgress5.png";
    private static final String RTL_PROGRESS_1_ICON_FILE = "/images/circleProgress1.png";
    private static final String RTL_PROGRESS_2_ICON_FILE = "/images/circleProgress2.png";
    private static final String RTL_PROGRESS_3_ICON_FILE = "/images/circleProgress3.png";
    private static final String RTL_PROGRESS_4_ICON_FILE = "/images/circleProgress4.png";
    private static final String RTL_PROGRESS_5_ICON_FILE = "/images/circleProgress5.png";
    private static final String TICK_ICON_FILE = "/images/tick.png";
    private static final String PICKAXE_ICON_FILE = "/images/pickaxe.png";
    private static final String SMALL_EXCLAMATION_MARK_ICON_FILE = "/images/smallExclamationMark.png";

    private ListSelectionModel listSelectionModel;
    private int selectedRow = -1;
    
    private Action addAssetManuallyAction;
    private MultiBitButton addAssetManuallyButton;
    private Action refreshAssetAction;
    private MultiBitButton refreshAssetButton;
    private Action deleteAssetAction;
    private MultiBitButton deleteAssetButton;
    
//    private Action showTransactionDetailsAction;
//    private MultiBitButton showTransactionsButton;
//    
//    private Action exportTransactionsSubmitAction;
//    private MultiBitButton exportTransactionsButton;

//    public static final int UPDATE_TRANSACTIONS_DELAY_TIME = 1000; // milliseconds
    
    private JScrollPane scrollPane;
    
    private CSAssetDetailPanel assetDetailPanel;
    
    private boolean viewIsVisible;
    
    /**
     * Timer used to condense multiple updates
     */
    //private static Timer updateTransactionsTimer;
    //private static UpdateTransactionsTimerTask updateTransactionsTimerTask;
    
    class JComponentTableCellRenderer implements TableCellRenderer {
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
		boolean hasFocus, int row, int column) {
	    return (JComponent) value;
	}
    }

    public CSShowAssetsPanel(BitcoinController bitcoinController, MultiBitFrame mainFrame) {
        this.bitcoinController = bitcoinController;
        this.controller = this.bitcoinController;
        this.mainFrame = mainFrame;

//        updateTransactionsTimerTask = new UpdateTransactionsTimerTask(controller, this, mainFrame);
//        updateTransactionsTimer = new Timer();
//        updateTransactionsTimer.scheduleAtFixedRate(updateTransactionsTimerTask, UPDATE_TRANSACTIONS_DELAY_TIME, UPDATE_TRANSACTIONS_DELAY_TIME);
               
        initUI();

        applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        
//        CurrencyConverter.INSTANCE.addCurrencyConverterListener(this);
	
	// Register ourselves as a listener the CSEventBus
	CSEventBus.INSTANCE.registerAsyncSubscriber(this);
    }

    @Subscribe
    public void listen(CSEvent event) throws Exception {
	// Ignore, don't update table etc. unless view is selected tab pane.
	if (!this.viewIsVisible) return;
	
	// We want to preserve selection
	final int selectedRow = table.getSelectedRow(); // display, not model index
	final int selectedColumn = table.getSelectedColumn(); // display, not model index
	final boolean selectionExists = (selectedColumn!=-1 && selectedRow!=-1);
	final int selectedAssetID = (selectionExists) ? assetTableModel.getAssetIDAtRow(rowSorter.convertRowIndexToModel(selectedRow)) : -1;
//	System.out.println("selected cell row " + selectedRow + ", col " + selectedColumn + "  where asset ID = " + selectedAssetID);
	//log.debug("Received CSEvent: Type=" + event.getType() + " , info=" + event.getInfo());
	Object o = event.getInfo();
	
	CSEventType t = event.getType();
	
	if (t == CSEventType.BALANCE_VALID) {
	    CSBalance bal = (CSBalance) o;
	    if (bal != null) {
		o = (Integer) bal.getAssetID();  // the integer is used later
	    }
	}
	
	if (t == CSEventType.ASSET_VALIDATION_STARTED
		|| t == CSEventType.ASSET_VALIDATION_COMPLETED
		|| t == CSEventType.ASSET_UPDATED
		|| t == CSEventType.ASSET_VISIBILITY_CHANGED
		|| t == CSEventType.BALANCE_VALID) {
	    
	    
	    
	    // We know o should be an Integer, but if BALANCE_VALID, we have already computed the value.
	    final int changedAssetID = (int)o;

	    SwingUtilities.invokeLater(new Runnable() {
		@Override
		public void run() {
//		    assetTableModel.fireTableDataChanged();

		    // row is the model row index, not display row indexdue to sorting
		    int changedRow = assetTableModel.getRowForAssetID(changedAssetID);
		    if (changedRow != -1) {
			// Note: if changedRow is -1, the header column becomes corrupted, with visibility icon becoming text label.
			assetTableModel.fireTableRowsUpdated(changedRow, changedRow);
		    }
		    assetDetailPanel.updateView();
		    assetDetailPanel.revalidate();
		}
	    });
	} else if (t == CSEventType.ASSET_DELETED
		|| t == CSEventType.ASSET_INSERTED) {
	    SwingUtilities.invokeLater(new Runnable() {
		@Override
		public void run() {
		    assetTableModel.recreateWalletData(); // firetabledatachanged() will get invoked in recreatewalletdata()		    
		    if (selectionExists == true && selectedAssetID!=-1) {
			int newRow = assetTableModel.getRowForAssetID(selectedAssetID);
			if (newRow != -1) {
			    int newDisplayRow = rowSorter.convertRowIndexToView(newRow);
			    table.setColumnSelectionInterval(selectedColumn, selectedColumn);
			    table.setRowSelectionInterval(newDisplayRow, newDisplayRow);
			}
		    }
		}
	    });
	}
	// SwingUtilities.invokeLater , EventQueue.invokeLater(...);
    }

    private void initUI() {
        setMinimumSize(new Dimension(550, 220));
        setLayout(new GridBagLayout());
        setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 0.5;
        constraints.anchor = GridBagConstraints.LINE_START;
//        add(createUpperPanel(), constraints);;
//
//        constraints.fill = GridBagConstraints.BOTH;
//        constraints.gridx = 0;
//        constraints.gridy = 1;
//        constraints.gridwidth = 1;
//        constraints.gridheight = 1;
//        constraints.weightx = 1.0;
//        constraints.weighty = 0.5;
//        constraints.anchor = GridBagConstraints.LINE_START;
//        add(createLowerPanel(), constraints);
        
        setLayout(new BorderLayout());
	assetDetailPanel = (CSAssetDetailPanel)createLowerPanel();
	// Hide it first
	assetDetailPanel.setVisible(false);
	JScrollPane detail = new JScrollPane(assetDetailPanel);
	
	        detail.setBorder(BorderFactory.createEmptyBorder());
		detail.setMinimumSize(new Dimension(550, 100));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createUpperPanel(), detail);
        splitPane.setDividerLocation( 200 );
        splitPane.setOneTouchExpandable(false);
	
	        splitPane.setBorder(BorderFactory.createEmptyBorder());

        add(splitPane, BorderLayout.CENTER); //, constraints);
    }
    
    private JPanel createUpperPanel() {
        JPanel p = new JPanel();
        //p.setOpaque(false);
        
        p.setLayout(new BorderLayout());
        JPanel buttonPanel = createButtonPanel();

        JPanel assetsPanel = createAssetsPanel();
        p.add(assetsPanel, BorderLayout.CENTER);
        
        buttonPanel.setMinimumSize(new Dimension(60, 60));
        p.add(buttonPanel, BorderLayout.SOUTH);
	
	p.setMinimumSize(new Dimension(550, 100));

        return p;
    }
    
    private JPanel createLowerPanel() {
	JPanel p = new CSAssetDetailPanel(this.bitcoinController);

	//JPanel p = new JPanel();
	
        //p.setLayout(new FlowLayout());
        //p.setBackground(Color.CYAN);
        //p.setMinimumSize(new Dimension(60, 300));
        
        //JLabel label = new JLabel("Detail for the selected row");
        //p.add(label);
        return p;
    }
    
//    public static void updateTransactions() {
//        if (updateTransactionsTimerTask != null) {
//                updateTransactionsTimerTask.setUpdateTransactions(true);                
//        }
//    }

//    private JPanel createTransactionsPanel() {
//	JPanel transactionsPanel = new JPanel();
//        transactionsPanel.setMinimumSize(new Dimension(550, 160));
//        transactionsPanel.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
//        transactionsPanel.setLayout(new GridBagLayout());
//        transactionsPanel.setOpaque(true);
//	return transactionsPanel;
//    }
    
    public void copyAssetRefToClipboard() {
	int row = table.getSelectedRow();
	if (row==-1) return;
		
	CSAsset asset = assetTableModel.getCSAssetAtRow(table.convertRowIndexToModel(row));
	if (asset==null) return;
	
	String assetRef = CSMiscUtils.getHumanReadableAssetRef(asset);
	try {
	    final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    final StringSelection selection = new StringSelection(assetRef);
	    clipboard.setContents(selection, selection);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }
    
    private JPanel createAssetsPanel() {
        JPanel p = new JPanel();
        p.setMinimumSize(new Dimension(550, 100));
        p.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        p.setLayout(new GridBagLayout());
        p.setOpaque(true);
        GridBagConstraints constraints = new GridBagConstraints();

        assetTableModel = new WalletAssetTableModel(bitcoinController);
        table = new JTable(assetTableModel);
        table.setOpaque(false);
        table.setBorder(BorderFactory.createEmptyBorder());
        table.setComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        table.setRowHeight(Math.max(MINIMUM_ICON_HEIGHT, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont()).getHeight()) + HEIGHT_DELTA);
	table.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());

	table.setRowSelectionAllowed(true);
	table.setColumnSelectionAllowed(false);
	//table.setCellSelectionEnabled(true); // single cell selection, default copy works ok, but no row highlighting.
	table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	
	// Useful to let user copy the asset-ref with ctrl-c, but still highlight row.
	ActionListener listener = new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent event) {
		copyAssetRefToClipboard();
	    }
	};
	final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false);
	table.registerKeyboardAction(listener, "Copy", stroke, JComponent.WHEN_FOCUSED);

        // Use status icons.
//        table.getColumnModel().getColumn(0).setCellRenderer(new ImageRenderer());
//        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
//        table.setRowSelectionAllowed(true);
//        table.setColumnSelectionAllowed(false);
//        table.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);

	// CoinSpark
//        table.setCellSelectionEnabled(true);
//        table.setTableHeader(new JTableHeader(table.getColumnModel()) {
//             @Override public Dimension getPreferredSize() {
//                 Dimension d = super.getPreferredSize();
//                 d.height = table.getRowHeight()*2;
//                 return d;
//             }
//         });

//        hider = new TableColumnHider(table);

//        table.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
//                int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
//                if (col == 9 || col == 10)
//                {
//                    WalletTableData wtd =  getSelectedRowData();
//
//                    String url = col == 9 ? wtd.getAssetPage() : wtd.getAssetContract();
//                    try {
//                    if(Desktop.isDesktopSupported())
//                    {
//                        Desktop.getDesktop().browse(new URI(url));
//                    }
//                    }
//                    catch (Exception use)
//                    {
//                        use.printStackTrace();;
//                    }
//                }
//                // DO here what you want to do with your url
//            }
//
//            @Override
//            public void mouseEntered(MouseEvent e) {
//                int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
//                if (col == 9 || col == 10) {
//                    table.setCursor(new Cursor(Cursor.HAND_CURSOR));
//                }
//            }
//
//            @Override
//            public void mouseExited(MouseEvent e) {
//                int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
//                if (col == 9 || col == 10) {
//                    table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
//                }
//            }
//        });
        

        // No row is currently selected.
        selectedRow = -1;

        // Listener for row selections.
        listSelectionModel = table.getSelectionModel();
        listSelectionModel.addListSelectionListener(new SharedListSelectionHandler());
	//showTransactionDetailsAction));
        
        // Date right justified.
//        table.getColumnModel().getColumn(1).setCellRenderer(new TrailingJustifiedDateRenderer());

        // Justify column headers.
        justifyColumnHeaders();

        TableColumn tableColumn = null; //table.getColumnModel().getColumn(0);
//        tableColumn.setPreferredWidth(100);
	
	// Default font
	Font defaultFont = FontSizer.INSTANCE.getAdjustedDefaultFont();

	// Visibility checkbox column uses a custom cell renderer for the header icon.
	int colIndex = assetTableModel.getColumnIndex("visibility");
	tableColumn = table.getColumnModel().getColumn( colIndex );
	TableCellRenderer checkboxRenderer = table.getDefaultRenderer(Boolean.class);
	TableCellEditor checkboxEditor = table.getDefaultEditor(Boolean.class);
	tableColumn.setCellEditor(checkboxEditor);
	tableColumn.setCellRenderer(checkboxRenderer);
	TableCellRenderer renderer = new JComponentTableCellRenderer();
	ImageIcon visibilityHeaderIcon = ImageLoader.fatCow16(ImageLoader.FATCOW.eye);
	JLabel visibilityHeaderLabel = new JLabel(visibilityHeaderIcon);
	Border headerBorder = UIManager.getBorder("TableHeader.cellBorder");
	visibilityHeaderLabel.setBorder(headerBorder);
	visibilityHeaderLabel.setToolTipText("Visibility");
	tableColumn.setHeaderRenderer(renderer);
	tableColumn.setHeaderValue(visibilityHeaderLabel);
	int visibilityColumnWidth = visibilityHeaderIcon.getIconWidth() + 6;
	tableColumn.setPreferredWidth(visibilityColumnWidth);
	tableColumn.setMinWidth(visibilityColumnWidth);
	tableColumn.setMaxWidth(visibilityColumnWidth);
    
	colIndex = assetTableModel.getColumnIndex("assetRef");
        tableColumn = table.getColumnModel().getColumn( colIndex);
	CSTruncatedTooltipDefaultTableCellRenderer assetRefRenderer = new CSTruncatedTooltipDefaultTableCellRenderer();
	assetRefRenderer.setCustomBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
	tableColumn.setCellRenderer(assetRefRenderer);
	FontMetrics fontMetrics = getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont());
	int minimumWidth = fontMetrics.stringWidth("#####-#####-#####");
	//System.out.println("assetRef column minimumWidth: " + minimumWidth);
	tableColumn.setMinWidth(minimumWidth);
	tableColumn.setPreferredWidth(minimumWidth);
	
	colIndex = assetTableModel.getColumnIndex("state");
        tableColumn = table.getColumnModel().getColumn( colIndex);
	tableColumn.setCellRenderer(new CSTruncatedTooltipDefaultTableCellRenderer());
	
	colIndex = assetTableModel.getColumnIndex("quantity");
        tableColumn = table.getColumnModel().getColumn( colIndex);
	CSTruncatedTooltipDefaultTableCellRenderer qtyRenderer = new CSTruncatedTooltipDefaultTableCellRenderer();
	qtyRenderer.setCustomBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
	tableColumn.setCellRenderer(qtyRenderer);
	qtyRenderer.setHorizontalAlignment( SwingConstants.RIGHT );

	minimumWidth = fontMetrics.stringWidth("12345678.1234");
	//System.out.println("assetRef column minimumWidth: " + minimumWidth);
	tableColumn.setMinWidth(minimumWidth);
	tableColumn.setPreferredWidth(minimumWidth);	
	
	colIndex = assetTableModel.getColumnIndex("name");
        tableColumn = table.getColumnModel().getColumn( colIndex);
	tableColumn.setCellRenderer(new CSCustomFontDefaultTableCellRenderer(defaultFont));
	
	colIndex = assetTableModel.getColumnIndex("issuer");
        tableColumn = table.getColumnModel().getColumn( colIndex);
	tableColumn.setCellRenderer(new CSCustomFontDefaultTableCellRenderer(defaultFont));
//	int spendableWidth = fontMetrics.stringWidth("(Spendable 00000.0000000)");
//	column.setPreferredWidth(spendableWidth);
//	column.setMaxWidth(spendableWidth * 2);
//	column.setMinWidth(spendableWidth);	
	
        // Description leading justified (set explicitly as it does not seem to work otherwise).
//        if (ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()).isLeftToRight()) {
//            table.getColumnModel().getColumn(2).setCellRenderer(new LeadingJustifiedRenderer());
//        } else {
//            table.getColumnModel().getColumn(2).setCellRenderer(new TrailingJustifiedStringRenderer());
//        }
	
//
//        // Amount decimal aligned
//        DecimalAlignRenderer decimalAlignRenderer = new DecimalAlignRenderer();
//        table.getColumnModel().getColumn(3).setCellRenderer(decimalAlignRenderer);
// 
//
//        FontMetrics fontMetrics = getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont());
//        TableColumn tableColumn = table.getColumnModel().getColumn(0); // status
//        int statusWidth = fontMetrics.stringWidth(controller.getLocaliser().getString("walletData.statusText"));
//        tableColumn.setPreferredWidth(statusWidth + STATUS_WIDTH_DELTA);
//
//        tableColumn = table.getColumnModel().getColumn(1); // Date.
//        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy HH:mm", controller.getLocaliser().getLocale());
//
//        int dateWidth = Math.max(fontMetrics.stringWidth(controller.getLocaliser().getString("walletData.dateText")),
//                fontMetrics.stringWidth(dateFormatter.format(new Date(DateUtils.nowUtc().getMillis()))));
//        tableColumn.setPreferredWidth(dateWidth);
//
//        tableColumn = table.getColumnModel().getColumn(2); // Description.
//        tableColumn.setPreferredWidth(250);
//
//        tableColumn = table.getColumnModel().getColumn(3); // Amount (BTC).
//        int amountBTCWidth = Math.max(fontMetrics.stringWidth(controller.getLocaliser().getString("sendBitcoinPanel.amountLabel") + " (BTC)"),
//                fontMetrics.stringWidth("00000.000000000"));
//        tableColumn.setPreferredWidth(amountBTCWidth);
//        tableColumn.setMinWidth(amountBTCWidth);

//        if (CurrencyConverter.INSTANCE.isShowingFiat()) {
//            tableColumn = table.getColumnModel().getColumn(4); // Amount (fiat).
//            int amountFiatWidth = Math.max(fontMetrics.stringWidth(controller.getLocaliser().getString("sendBitcoinPanel.amountLabel") + " (USD)"),
//                    fontMetrics.stringWidth("000.0000"));
//            tableColumn.setPreferredWidth(amountFiatWidth);
//           
//            table.getColumnModel().getColumn(4).setCellRenderer(new TrailingJustifiedNumericRenderer());
//        }
//
        // Row sorter.
        rowSorter = new TableRowSorter<TableModel>(table.getModel());
        table.setRowSorter(rowSorter);
//
//        // Sort by date descending.
//        List<TableRowSorter.SortKey> sortKeys = new ArrayList<TableRowSorter.SortKey>();
//        sortKeys.add(new TableRowSorter.SortKey(1, SortOrder.DESCENDING));
//        rowSorter.setSortKeys(sortKeys);
//        Comparator<Date> comparator = new Comparator<Date>() {
//            @Override
//            public int compare(Date o1, Date o2) {
//                if (o1 == null) {
//                    if (o2 == null) {
//                        return 0;
//                    } else {
//                        return 1;
//                    }
//                } else {
//                    if (o2 == null) {
//                        return -1;
//                    }
//                }
//                long n1 = o1.getTime();
//                long n2 = o2.getTime();
//                if (n1 == 0) {
//                    // Object 1 has missing date.
//                    return 1;
//                }
//                if (n2 == 0) {
//                    // Object 2 has missing date.
//                    return -1;
//                }
//                if (n1 < n2) {
//                    return -1;
//                } else if (n1 > n2) {
//                    return 1;
//                } else {
//                    return 0;
//                }
//            }
//        };
//        rowSorter.setComparator(1, comparator);
//
//        Comparator<String> comparatorNumber = new Comparator<String>() {
//            @Override
//            public int compare(String o1, String o2) {
//                try {
//                    if (o1 == null) {
//                        if (o2 == null) {
//                            return 0;
//                        } else {
//                            return 1;
//                        }
//                    } else {
//                        if (o2 == null) {
//                            return -1;
//                        }
//                    }
//                    DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(controller.getLocaliser().getLocale());
//                    formatter.setParseBigDecimal(true);
//
//                    // Convert spaces to non breakable space.
//                    o1 = o1.replaceAll(" ", "\u00A0");
//                    o2 = o2.replaceAll(" ", "\u00A0");
//
//                    BigDecimal parsedO1 = (BigDecimal) formatter.parse(o1);
//                    BigDecimal parsedO2 = (BigDecimal) formatter.parse(o2);
//                    return parsedO1.compareTo(parsedO2);
//                } catch (NumberFormatException nfe) {
//                    return o1.compareTo(o2);
//                } catch (ParseException e) {
//                    return o1.compareTo(o2);
//                }
//            }
//        };
//        rowSorter.setComparator(3, comparatorNumber);
//        if (CurrencyConverter.INSTANCE.isShowingFiat()) {
//            rowSorter.setComparator(4, comparatorNumber);
//        }
        
        scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPaneSetup();   
        
//        showTransactionDetailsAction.setEnabled(table.getSelectedRow() > -1);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;

        p.add(scrollPane, constraints);
        
        return p;
    }
    
    private void justifyColumnHeaders() {
        TableCellRenderer renderer = table.getTableHeader().getDefaultRenderer();
        JLabel label = (JLabel) renderer;
        label.setHorizontalAlignment(JLabel.CENTER);
        table.getTableHeader().setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());       
    }
    
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SystemColor.windowBorder));
        buttonPanel.setOpaque(true);
        buttonPanel.setBackground(ColorAndFontConstants.MID_BACKGROUND_COLOR);
        buttonPanel.setComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

//        Action helpAction;
//        if (ComponentOrientation.LEFT_TO_RIGHT == ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())) {
//            helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_ICON_FILE,
//                    "multiBitFrame.helpMenuText", "multiBitFrame.helpMenuTooltip", "multiBitFrame.helpMenuText",
//                    HelpContentsPanel.HELP_ASSETS_URL);
//        } else {
//            helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_RTL_ICON_FILE,
//                    "multiBitFrame.helpMenuText", "multiBitFrame.helpMenuTooltip", "multiBitFrame.helpMenuText",
//                    HelpContentsPanel.HELP_ASSETS_URL);
//        }
//        HelpButton helpButton = new HelpButton(helpAction, controller);
//        helpButton.setText("");
//        helpButton.setToolTipText(controller.getLocaliser().getString("multiBitFrame.helpMenuTooltip"));
//        helpButton.setHorizontalAlignment(SwingConstants.LEADING);
//        constraints.fill = GridBagConstraints.HORIZONTAL;
//        constraints.gridx = 0;
//        constraints.gridy = 0;
//        constraints.weightx = 0.3;
//        constraints.weighty = 0.1;
//        constraints.gridwidth = 1;
//        constraints.gridheight = 1;
//        constraints.anchor = GridBagConstraints.LINE_START;
//        buttonPanel.add(helpButton, constraints);
        
	addAssetManuallyAction = new CSAddAssetManuallyAction(bitcoinController, mainFrame, this);
	addAssetManuallyButton = new MultiBitButton(addAssetManuallyAction, controller);
	addAssetManuallyButton.setEnabled(true);
//        showTransactionDetailsAction = new ShowTransactionDetailsAction(bitcoinController, mainFrame, this);
//        showTransactionsButton = new MultiBitButton(showTransactionDetailsAction, controller);
//        showTransactionsButton.setEnabled(false);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 0.1;
        constraints.weighty = 1.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
//        buttonPanel.add(showTransactionsButton, constraints);
	buttonPanel.add(addAssetManuallyButton, constraints);
	
	refreshAssetAction = new CSRefreshAssetAction(bitcoinController, buttonPanel, this);
	refreshAssetButton = new MultiBitButton(refreshAssetAction, controller);
	refreshAssetButton.setEnabled(false);
	constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.weightx = 0.1;
        constraints.weighty = 1.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
	buttonPanel.add(refreshAssetButton, constraints);

	deleteAssetAction = new CSDeleteAssetAction(bitcoinController, buttonPanel, this);
	deleteAssetButton = new MultiBitButton(deleteAssetAction, controller);
	deleteAssetButton.setEnabled(false);
	constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.weightx = 0.1;
        constraints.weighty = 1.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
	buttonPanel.add(deleteAssetButton, constraints);
	
//        exportTransactionsSubmitAction = new ExportTransactionsSubmitAction(bitcoinController, mainFrame);
//        exportTransactionsButton = new MultiBitButton(exportTransactionsSubmitAction, controller);
//        showTransactionsButton.setEnabled(false);
//        constraints.fill = GridBagConstraints.NONE;
//        constraints.gridx = 2;
//        constraints.gridy = 0;
//        constraints.weightx = 0.1;
//        constraints.weighty = 1.0;
//        constraints.gridwidth = 1;
//        constraints.gridheight = 1;
//        constraints.anchor = GridBagConstraints.LINE_START;
//        buttonPanel.add(exportTransactionsButton, constraints);

        /*CoinSpark*/
/*        showColorsButton = new JButton("Hide Colors");
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.weightx = 0.1;
        constraints.weighty = 1.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        buttonPanel.add(showColorsButton, constraints);
        //Add action listener to button
        showColorsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                if (showColorsButton.getText().contains("Hide"))
                {
                    for(int cn = 0; cn < WalletTableData.COLOR_COLUMN_HEADER_KEYS.length; cn++)
                    {
                        String colname = controller.getLocaliser().getString(WalletTableData.COLOR_COLUMN_HEADER_KEYS[cn]);
                        hider.hide(colname);
                    }
                    showColorsButton.setText("Show Colors");
                }
                else
                {
                    for(int cn = WalletTableData.COLOR_COLUMN_HEADER_KEYS.length-1; cn >=0 ; cn--)
                    {
                        String colname = controller.getLocaliser().getString(WalletTableData.COLOR_COLUMN_HEADER_KEYS[cn]);
                        hider.show(colname);
                    }
                    showColorsButton.setText("Hide Colors");
                }
            }
        });

        showColorsDBButton = new JButton("Show Assets Database");
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.weightx = 0.1;
        constraints.weighty = 1.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        buttonPanel.add(showColorsDBButton, constraints);
        //Add action listener to button
        showColorsDBButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                new CSAssetsDBDialog(mainFrame, bitcoinController.getModel().getAssetsDatabase());
            }
        });*/
        /*CoinSpark*/

        JPanel fill1 = new JPanel();
        fill1.setOpaque(false);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.weightx = 200;
        constraints.weighty = 1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        buttonPanel.add(fill1, constraints);

        return buttonPanel;
    }
    
    private void scrollPaneSetup() {
        scrollPane.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        scrollPane.setComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        scrollPane.getHorizontalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);
        scrollPane.getVerticalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);
        scrollPane.setOpaque(true);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, SystemColor.windowBorder));
    }

    @Override
    public void displayView(DisplayHint displayHint) {
	this.viewIsVisible = true;
        //System.out.println("ShowAssetsPanel#displayView called on panel " + System.identityHashCode(this) + " for wallet " + this.bitcoinController.getModel().getActiveWalletFilename());
        if (this.bitcoinController.getModel().getActiveWallet() == null) {
            return;
        }
        justifyColumnHeaders();
        scrollPaneSetup();
        
        // Amount decimal aligned
//        DecimalAlignRenderer decimalAlignRenderer = new DecimalAlignRenderer();
//        table.getColumnModel().getColumn(3).setCellRenderer(decimalAlignRenderer);

	// MultiBitFrame invokes displayView() a lot, even if changes do not require table to be recreated.
	// We only need this when displaying the wallet assets for the first time, or when an event is detected
	// due to a new asset being inserted or deleted.
	Wallet w = this.bitcoinController.getModel().getActiveWallet();
	if (previousWallet != w) {
	    previousWallet = w;
	    assetTableModel.recreateWalletData();
	}

        if (selectedRow > -1 && selectedRow < table.getRowCount()) {
            table.setRowSelectionInterval(selectedRow, selectedRow);
        }
    }

    @Override
    public void navigateAwayFromView() {
	this.viewIsVisible = false;
    }
/*
    class ImageRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 154545L;

        JLabel primaryLabel = new JLabel();

        // If the component is a doubleIcon the next fields are used.
        JLabel extraLabel = new JLabel();
        boolean doubleIcon = false;
        JPanel combinationPanel = new JPanel();

        ImageIcon shapeTriangleIcon = ImageLoader.createImageIcon(ImageLoader.SHAPE_TRIANGLE_ICON_FILE);
        ImageIcon shapeSquareIcon = ImageLoader.createImageIcon(ImageLoader.SHAPE_SQUARE_ICON_FILE);
        ImageIcon shapeHeptagonIcon = ImageLoader.createImageIcon(ImageLoader.SHAPE_PENTAGON_ICON_FILE);
        ImageIcon shapeHexagonIcon = ImageLoader.createImageIcon(ImageLoader.SHAPE_HEXAGON_ICON_FILE);
        ImageIcon pickaxeIcon = ImageLoader.createImageIcon(PICKAXE_ICON_FILE);
        ImageIcon smallExclamationMarkIcon = ImageLoader.createImageIcon(SMALL_EXCLAMATION_MARK_ICON_FILE);
        ImageIcon tickIcon = ImageLoader.createImageIcon(TICK_ICON_FILE);
        ImageIcon progress0Icon = ImageLoader.createImageIcon(PROGRESS_0_ICON_FILE);
        ImageIcon progress1Icon = ImageLoader.createImageIcon(PROGRESS_1_ICON_FILE);
        ImageIcon progress2Icon = ImageLoader.createImageIcon(PROGRESS_2_ICON_FILE);
        ImageIcon progress3Icon = ImageLoader.createImageIcon(PROGRESS_3_ICON_FILE);
        ImageIcon progress4Icon = ImageLoader.createImageIcon(PROGRESS_4_ICON_FILE);
        ImageIcon progress5Icon = ImageLoader.createImageIcon(PROGRESS_5_ICON_FILE);
        ImageIcon rtlProgress1Icon = ImageLoader.createImageIcon(RTL_PROGRESS_1_ICON_FILE);
        ImageIcon rtlProgress2Icon = ImageLoader.createImageIcon(RTL_PROGRESS_2_ICON_FILE);
        ImageIcon rtlProgress3Icon = ImageLoader.createImageIcon(RTL_PROGRESS_3_ICON_FILE);
        ImageIcon rtlProgress4Icon = ImageLoader.createImageIcon(RTL_PROGRESS_4_ICON_FILE);
        ImageIcon rtlProgress5Icon = ImageLoader.createImageIcon(RTL_PROGRESS_5_ICON_FILE);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            
            // Prepare the primary icon (used always), and an extra icon and containing panel for use as required.
            primaryLabel.setHorizontalAlignment(SwingConstants.CENTER);
            primaryLabel.setVerticalAlignment(SwingConstants.CENTER);
            primaryLabel.setOpaque(true);
            extraLabel.setHorizontalAlignment(SwingConstants.CENTER);
            extraLabel.setVerticalAlignment(SwingConstants.CENTER);
            extraLabel.setOpaque(true);
            combinationPanel.setOpaque(true);
            combinationPanel.setLayout(new GridBagLayout());

            GridBagConstraints constraints = new GridBagConstraints();

            // Prepare a double icon panel for use as required.
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.LINE_END;
            combinationPanel.add(primaryLabel, constraints);

            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.LINE_START;

            combinationPanel.add(extraLabel, constraints);           
            
            // Get the transaction and transaction confidence
            Transaction transaction = (Transaction)value;
     
            TransactionConfidence confidence = null;
            if (transaction != null) {
                confidence = transaction.getConfidence();
            }            
            ConfidenceType confidenceType = null;
            if (confidence != null) {
                confidenceType = confidence.getConfidenceType();
            }
            if (confidenceType == null) {
                confidenceType = ConfidenceType.UNKNOWN;
            }
   
            // Coinbase transactions have an extra pickaxe icon.
            if (transaction != null && transaction.isCoinBase()) {
                extraLabel.setIcon(pickaxeIcon);
                doubleIcon = true;
            } else {
                doubleIcon = false;
            }

            // Work out the primary icon.
            switch (confidenceType) {
            case UNKNOWN: {
                primaryLabel.setText("?");
                primaryLabel.setIcon(null);
                break;
            }
            case BUILDING: {
                if (bitcoinController.getMultiBitService().getChain() == null) {
                    primaryLabel.setText("?");
                    primaryLabel.setIcon(null);
                } else {
                    int numberOfBlocksEmbedded = bitcoinController.getMultiBitService().getChain().getBestChainHeight() - confidence.getAppearedAtChainHeight() + 1;
                    if (transaction != null && transaction.isCoinBase()) {
                        // Coinbase tx mature slower than regular blocks.
                        numberOfBlocksEmbedded = numberOfBlocksEmbedded / 20;
                    }
                    ImageIcon buildingIcon = getBuildingIcon(numberOfBlocksEmbedded, transaction);
                    primaryLabel.setIcon(buildingIcon);
                    primaryLabel.setText("");
                    if (numberOfBlocksEmbedded >= 6) {
                        primaryLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("multiBitFrame.status.isConfirmed")));
                    } else {
                        if (transaction != null && transaction.isCoinBase()) {
                            primaryLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("multiBitFrame.status.beingConfirmedAndCoinbase")));                            
                        } else {
                            primaryLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("multiBitFrame.status.beingConfirmed")));
                        }
                    }
                }
                break;
            }
            case PENDING: {
                primaryLabel.setIcon(getConfidenceIcon(confidence));
                primaryLabel.setText("");
                
                primaryLabel.setToolTipText(HelpContentsPanel.createTooltipText(getUnconfirmedConfidenceToolTip(transaction)));
                
                if (transaction != null) {
                    if (transaction.getLockTime() > 0) {
                        extraLabel.setIcon(smallExclamationMarkIcon);
                        doubleIcon = true;
                    } else {
                        doubleIcon = false;
                    }
                }
                break;
            }
            case DEAD: {
                primaryLabel.setIcon(smallExclamationMarkIcon);
                primaryLabel.setText(controller.getLocaliser().getString("multiBitFrame.status.dead"));
                break;
            }
            default: {
                primaryLabel.setIcon(null);
                primaryLabel.setText("?");
                break;
            }
            }
           
            // Propagate the tooltip text.
            extraLabel.setToolTipText(primaryLabel.getToolTipText());
            combinationPanel.setToolTipText(primaryLabel.getToolTipText());

            // Set foreground and background colors.
            if (isSelected) {
                selectedRow = row;
                primaryLabel.setBackground(table.getSelectionBackground());
                primaryLabel.setForeground(table.getSelectionForeground());
                extraLabel.setBackground(table.getSelectionBackground());
                extraLabel.setForeground(table.getSelectionForeground());
                combinationPanel.setBackground(table.getSelectionBackground());
            } else {
                primaryLabel.setForeground(table.getForeground());
                extraLabel.setForeground(table.getForeground());
                combinationPanel.setForeground(table.getForeground());
                if (row % 2 == 1) {
                    primaryLabel.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
                    extraLabel.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
                    combinationPanel.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
                } else {
                    primaryLabel.setBackground(ColorAndFontConstants.ALTERNATE_TABLE_COLOR);
                    extraLabel.setBackground(ColorAndFontConstants.ALTERNATE_TABLE_COLOR);
                    combinationPanel.setBackground(ColorAndFontConstants.ALTERNATE_TABLE_COLOR);
                    primaryLabel.setOpaque(true);
                    extraLabel.setOpaque(true);
                    combinationPanel.setOpaque(true);
                }
            }

            // Return either a single icon or a double icon panel.
            if (doubleIcon) {
                return combinationPanel;
            } else {
                return primaryLabel;
            }
        }

        private ImageIcon getBuildingIcon(int numberOfBlocksEmbedded, Transaction transaction) {
            TransactionConfidence confidence = null;
            if (transaction != null) {
                confidence = transaction.getConfidence();
            }
            
            if (numberOfBlocksEmbedded < 0) {
                numberOfBlocksEmbedded = 0;
            }
            if (numberOfBlocksEmbedded > 6) {
                numberOfBlocksEmbedded = 6;
            }

            boolean isLeftToRight = ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()).isLeftToRight();

            switch (numberOfBlocksEmbedded) {
            case 0: {
                return getConfidenceIcon(confidence);
            }
            case 1: {
                if (isLeftToRight) {
                    return progress1Icon;
                } else {
                    return rtlProgress1Icon;
                }
            }
            case 2: {
                if (isLeftToRight) {
                    return progress2Icon;
                } else {
                    return rtlProgress2Icon;
                }
            }
            case 3: {
                if (isLeftToRight) {
                    return progress3Icon;
                } else {
                    return rtlProgress3Icon;
                }
            }
            case 4: {
                if (isLeftToRight) {
                    return progress4Icon;
                } else {
                    return rtlProgress4Icon;
                }
            }
            case 5: {
                if (isLeftToRight) {
                    return progress5Icon;
                } else {
                    return rtlProgress5Icon;
                }
            }
            case 6: {
                return tickIcon;
            }
            default:
                return getConfidenceIcon(confidence);
            }
        }
        
        private String getUnconfirmedConfidenceToolTip(Transaction transaction) {
            TransactionConfidence confidence = null;
            if (transaction != null) {
                confidence = transaction.getConfidence();
            }

            // Work out the line describing the is the transaction is standard or not.
            String transactionTrustfulness = "";
            if (transaction != null) {
                if (transaction.getLockTime() > 0) {
                    // Non standard transaction.
                    transactionTrustfulness = MultiBit.getController().getLocaliser().getString("multiBitFrame.status.notConfirmedAndNotStandard") + ".";
                } else {
                    // Normal transaction.
                    if (transaction.isCoinBase()) {
                        transactionTrustfulness = MultiBit.getController().getLocaliser().getString("multiBitFrame.status.notConfirmedAndCoinbase") + ".";
                    } else {
                        transactionTrustfulness = MultiBit.getController().getLocaliser().getString("multiBitFrame.status.notConfirmed") + ".";
                    }
                }
            }
            
            // Work out the line describing the number of peers.
            int peers = 0;
            if (confidence != null) {
                peers = confidence.getBroadcastByCount();
            }
            StringBuilder builder = new StringBuilder();
            if (peers == 0) {
                builder.append(MultiBit.getController().getLocaliser()
                        .getString("transactionConfidence.seenByUnknownNumberOfPeers"));
            } else {
                builder
                    .append(MultiBit.getController().getLocaliser().getString("transactionConfidence.seenBy"))
                    .append(" ");
                builder.append(peers);
                if (peers > 1)
                    builder
                        .append(" ")
                        .append(MultiBit.getController().getLocaliser().getString("transactionConfidence.peers"))
                        .append(". ");
                else
                    builder
                        .append(" ")
                        .append(MultiBit.getController().getLocaliser().getString("transactionConfidence.peer"))
                        .append(". ");
            }

            return HelpContentsPanel.createMultilineTooltipText(new String[] {
                    transactionTrustfulness, builder.toString() });
        }
        
        private ImageIcon getConfidenceIcon(TransactionConfidence confidence) {
            // By default return a triangle which indicates the least known.
            ImageIcon iconToReturn = shapeTriangleIcon;
            
            if (confidence != null) {
                if (confidence.getConfidenceType() == ConfidenceType.BUILDING) {
                    return progress0Icon;
                }
            
                if (confidence.getBroadcastBy() != null) {
                    int numberOfPeers = confidence.getBroadcastByCount();
                    if (numberOfPeers >= 4) {
                        return progress0Icon;
                    } else {
                        switch (numberOfPeers) {
                        case 0 : iconToReturn = shapeTriangleIcon; break;
                        case 1 : iconToReturn = shapeSquareIcon; break;
                        case 2 : iconToReturn = shapeHeptagonIcon; break;
                        case 3 : iconToReturn = shapeHexagonIcon; break;
                        default:
                            iconToReturn = shapeTriangleIcon; 
                        }
                    }
                }
            }
            return iconToReturn;
        }    
    }
*/
    class TrailingJustifiedNumericRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1549545L;

        MultiBitLabel label;

        public TrailingJustifiedNumericRenderer() {
            label = new MultiBitLabel("");
        }
        
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            label.setHorizontalAlignment(SwingConstants.TRAILING);
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(new Insets(0, TABLE_BORDER, 1, TABLE_BORDER)));

            label.setText(value + SPACER);

            if ((value + "").contains("-")) {
                // Debit.
                if (isSelected) {
                    label.setForeground(table.getSelectionForeground());
                } else {
                    label.setForeground(ColorAndFontConstants.DEBIT_FOREGROUND_COLOR);                    
                }
            } else {
                // Credit.
                if (isSelected) {
                    label.setForeground(table.getSelectionForeground()); 
                } else {
                    label.setForeground(ColorAndFontConstants.CREDIT_FOREGROUND_COLOR);                     
                }
            }
            if (isSelected) {
                selectedRow = row;
                label.setBackground(table.getSelectionBackground());
            } else {
                if (row % 2 == 1) {
                    label.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
                } else {
                    label.setBackground(ColorAndFontConstants.ALTERNATE_TABLE_COLOR);
                    label.setOpaque(true);
                }
            }

            return label;
        }
    }

    class TrailingJustifiedStringRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1549545L;

        MultiBitLabel label;

        public TrailingJustifiedStringRenderer() {
            label = new MultiBitLabel("");
        }
        
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            label.setHorizontalAlignment(SwingConstants.TRAILING);
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(new Insets(0, TABLE_BORDER, 1, TABLE_BORDER)));

            label.setText(value + SPACER);

            if (isSelected) {
                label.setForeground(table.getSelectionForeground());
            } else {
                label.setForeground(Color.BLACK);
            }

            if (isSelected) {
                selectedRow = row;
                label.setBackground(table.getSelectionBackground());
            } else {
                if (row % 2 == 1) {
                    label.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
                } else {
                    label.setBackground(ColorAndFontConstants.ALTERNATE_TABLE_COLOR);
                    label.setOpaque(true);
                }
            }

            return label;
        }
    }

    class TrailingJustifiedDateRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1549545L;

        SimpleDateFormat dateFormatter;

        MultiBitLabel label;

        public TrailingJustifiedDateRenderer() {
            label = new MultiBitLabel("");
            dateFormatter = new SimpleDateFormat("dd MMM yyyy HH:mm", controller.getLocaliser().getLocale());
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            label.setHorizontalAlignment(SwingConstants.TRAILING);
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(new Insets(0, TABLE_BORDER, 1, TABLE_BORDER)));

            String formattedDate = "";
            if (value != null) {
                if (value instanceof Date) {
                    if (((Date) value).getTime() != 0) {
                        try {
                            formattedDate = dateFormatter.format(value);
                        } catch (IllegalArgumentException iae) {
                            // ok
                        }
                    }
                } else {
                    formattedDate = value.toString();
                }
            }

            label.setText(formattedDate + SPACER);

            if (isSelected) {
                selectedRow = row;
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
            } else {
                label.setForeground(table.getForeground());
                if (row % 2 == 1) {
                    label.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
                } else {
                    label.setBackground(ColorAndFontConstants.ALTERNATE_TABLE_COLOR);
                    label.setOpaque(true);
                }
            }

            return label;
        }
    }

    class LeadingJustifiedRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1549545L;

        MultiBitLabel label;

        public LeadingJustifiedRenderer() {
            label = new MultiBitLabel("");
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            label.setHorizontalAlignment(SwingConstants.LEADING);
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(new Insets(0, TABLE_BORDER, 1, TABLE_BORDER)));
            label.setText((String) value);

            if (isSelected) {
                selectedRow = row;
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
            } else {
                label.setForeground(table.getForeground());
                if (row % 2 == 1) {
                    label.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
                } else {
                    label.setBackground(ColorAndFontConstants.ALTERNATE_TABLE_COLOR);
                    label.setOpaque(true);
                }
            }

            return label;
        }
    }
    
    class DecimalAlignRenderer implements TableCellRenderer {
        private final TabStop tabStopRight = new TabStop(40, TabStop.ALIGN_RIGHT, TabStop.LEAD_NONE);
        private final TabStop tabStopLeft = new TabStop(41, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);

        private final TabSet tabSet = new TabSet(new TabStop[] { tabStopRight, tabStopLeft });

        private AttributeSet paragraphAttributeSet;
        private JTextPane pane;
        private Style style;
        
        public DecimalAlignRenderer() {
            pane = new JTextPane();

            StyleContext styleContext = StyleContext.getDefaultStyleContext();
            paragraphAttributeSet = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.TabSet, tabSet);
            pane.setParagraphAttributes(paragraphAttributeSet, true);
            
            style = pane.addStyle("number", null);

            pane.setOpaque(true);
            pane.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2,  ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR));
         }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            JPanel outerPanel = new JPanel(new BorderLayout());
            outerPanel.setOpaque(true);
            outerPanel.setBorder(BorderFactory.createEmptyBorder());

            JLabel filler = new JLabel();
            filler.setOpaque(true);

            if (value == null) {
                pane.setText("\t" + controller.getLocaliser().bitcoinValueToString(BigInteger.ZERO, false, false));
            } else {
                String contents = value.toString();
                String splitChar;
                String[] split;
                if (controller.getLocaliser().getDecimalFormatSymbols().getDecimalSeparator() == ',') {
                    // , as decimal point
                    splitChar = ",";
                    split = contents.split(",");
                } else {
                    // . as decimal point
                    splitChar = ".";
                    split = contents.split("\\.");
                }
                if (split == null) {
                    pane.setText("");
                } else if (split.length == 1) {
                    // Integer amount - no decimal point. Add a space to pad it
                    // left.
                    pane.setText("\t" + split[0] + " ");
                } else {
                    pane.setText("\t" + split[0] + splitChar + "\t" + split[1] + " ");
                }
                // log.debug("pane.getText = " + pane.getText());
            }

            if ((value.toString()).contains("-")) {
                // debit
                if (isSelected) {
                    pane.setForeground(table.getSelectionForeground());
                } else {
                    pane.setForeground(ColorAndFontConstants.DEBIT_FOREGROUND_COLOR);
                }
            } else {
                // credit
                if (isSelected) {
                    pane.setForeground(table.getSelectionForeground());
                } else {
                    pane.setForeground(ColorAndFontConstants.CREDIT_FOREGROUND_COLOR);
                }
            }

            if (isSelected) {
                selectedRow = row;
                pane.setBackground(table.getSelectionBackground());
                outerPanel.setBackground(table.getSelectionBackground());
                filler.setBackground(table.getSelectionBackground());
                pane.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, table.getSelectionBackground()));
            } else {
                if (row % 2 == 1) {
                    pane.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
                    pane.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR));
                    outerPanel.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
                    filler.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
                    outerPanel.setOpaque(true);
                } else {
                    pane.setBackground(ColorAndFontConstants.ALTERNATE_TABLE_COLOR);
                    pane.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, ColorAndFontConstants.ALTERNATE_TABLE_COLOR));
                    outerPanel.setBackground(ColorAndFontConstants.ALTERNATE_TABLE_COLOR);
                    filler.setBackground(ColorAndFontConstants.ALTERNATE_TABLE_COLOR);
                    pane.setOpaque(true);
                    outerPanel.setOpaque(true);
                    filler.setOpaque(true);
                }
            }

            StyleConstants.setForeground(style, pane.getForeground());
            if (row % 2 == 1 || isSelected) {
                StyleConstants.setBackground(style, pane.getBackground());
            } else {
                StyleConstants.setBackground(style, ColorAndFontConstants.ALTERNATE_TABLE_COLOR);
            }
            StyleConstants.setBold(style, FontSizer.INSTANCE.getAdjustedDefaultFont().isBold());
            StyleConstants.setItalic(style, FontSizer.INSTANCE.getAdjustedDefaultFont().isItalic());
            StyleConstants.setFontSize(style, FontSizer.INSTANCE.getAdjustedDefaultFont().getSize());
            StyleConstants.setFontFamily(style, FontSizer.INSTANCE.getAdjustedDefaultFont().getFontName());
            StyleConstants.setSpaceBelow(style, 10);

            pane.getStyledDocument().setCharacterAttributes(0, pane.getText().length(), pane.getStyle("number"), true);

            outerPanel.add(pane, BorderLayout.LINE_START);
            outerPanel.add(filler, BorderLayout.CENTER);
            
            // Avoid flicker by doing layout.
            outerPanel.doLayout();

            return outerPanel;
        }
    }
    
    class SharedListSelectionHandler implements ListSelectionListener {
//        private Action showTransactionDetailsAction;
//        
//        SharedListSelectionHandler (Action action) {
//            this.showTransactionDetailsAction = action;
//        }
	SharedListSelectionHandler() {
	}
        
	@Override
        public void valueChanged(ListSelectionEvent e) { 
            ListSelectionModel lsm = (ListSelectionModel)e.getSource();
            if (lsm.isSelectionEmpty()) {
//                showTransactionDetailsAction.setEnabled(false);
//                showTransactionsButton.invalidate();
//                showTransactionsButton.validate();
//                showTransactionsButton.repaint();
		assetDetailPanel.setVisible(false);
		refreshAssetButton.setEnabled(false);
		deleteAssetButton.setEnabled(false);

            } else {
		WalletAssetTableData data = getSelectedRowData();
		CSAsset asset = data.getAsset();
		assetDetailPanel.updateView(asset);
		assetDetailPanel.setVisible(true);
		assetDetailPanel.repaint();
		
		refreshAssetButton.setEnabled(true);

		// Only enable delete button if quantity is 0 (or - i.e. unknown asset due to missing spec etc)
		Wallet wallet = bitcoinController.getModel().getActiveWallet();
		int assetID = asset.getAssetID();
//		BigInteger x = wallet.CS.getUnspentAssetQuantity(assetID);
		BigInteger x = wallet.CS.getAssetBalance(assetID).total; 

		boolean canDelete = x.equals(BigInteger.ZERO);
		deleteAssetButton.setEnabled(canDelete);

                // Find out which indexes are selected.
//                int minIndex = lsm.getMinSelectionIndex();
//		System.out.println("minIndex = "+minIndex);
//		        int row = table.getSelectedRow();
//		System.out.println("getSelectedRow = "+row);
//		System.out.println("rowSorter.convertRowIndexToModel(row) = " + rowSorter.convertRowIndexToModel(row));
//                int maxIndex = lsm.getMaxSelectionIndex();
//                for (int i = minIndex; i <= maxIndex; i++) {
//                    if (lsm.isSelectedIndex(i)) {
////                        showTransactionDetailsAction.setEnabled(true);
////                        showTransactionsButton.invalidate();
////                        showTransactionsButton.validate();
////                        showTransactionsButton.repaint();
//                        break;
//                    }
//                }
            }
        }
    }

    public WalletAssetTableData getSelectedRowData() {
        int row = table.getSelectedRow();
        return assetTableModel.getRow(rowSorter.convertRowIndexToModel(row));
    }

    public JTable getTable() {
        return table;
    }

    @Override
    public Icon getViewIcon() {
	return null;
//        return ImageLoader.createImageIcon(ImageLoader.ASSETS_ICON_FILE);
    }

    @Override
    public String getViewTitle() {
        return controller.getLocaliser().getString("showAssetsAction.text");
    }

    @Override
    public String getViewTooltip() {
        return controller.getLocaliser().getString("showAssetsAction.tooltip");
    }

    @Override
    public View getViewId() {
        return View.COINSPARK_ASSETS_VIEW;
    }

//    @Override
//    public void lostExchangeRate(ExchangeRate exchangeRate) {  
//    }
//
//    @Override
//    public void foundExchangeRate(ExchangeRate exchangeRate) {
//        initUI();
//    }
//
//    @Override
//    public void updatedExchangeRate(ExchangeRate exchangeRate) {
//        CSShowAssetsPanel.updateTransactions();
//    }
/*CoinSpark*/
//    private class TableColumnHider {
//
//        private JTable table;
//        private TableColumnModel tcm;
//        private Map<String, IndexedColumn> hidden =
//                new HashMap<String, IndexedColumn>();
//
//        public TableColumnHider(JTable table) {
//            this.table = table;
//            this.tcm = table.getColumnModel();
//        }
//
//        public void hide(int index) {
//            String columnName = table.getColumnName(index)  ;
//            TableColumn column = tcm.getColumn(index);
//            IndexedColumn ic = new IndexedColumn(index, column);
//            if (hidden.put(columnName, ic) != null) {
//                throw new IllegalArgumentException("Duplicate column name.");
//            }
//            tcm.removeColumn(column);
//        }
//
//        public void hide(String columnName) {
//            int index = tcm.getColumnIndex(columnName);
//            TableColumn column = tcm.getColumn(index);
//            IndexedColumn ic = new IndexedColumn(index, column);
//            if (hidden.put(columnName, ic) != null) {
//                throw new IllegalArgumentException("Duplicate column name.");
//            }
//            tcm.removeColumn(column);
//        }
//
//        public void show(String columnName) {
//            IndexedColumn ic = hidden.remove(columnName);
//            if (ic != null) {
//                tcm.addColumn(ic.column);
//                int lastColumn = tcm.getColumnCount() - 1;
//                if (ic.index < lastColumn) {
//                    tcm.moveColumn(lastColumn, ic.index);
//                }
//            }
//        }
//
//        public void show(int index) {
//            String columnName = table.getColumnName(index);
//            IndexedColumn ic = hidden.remove(columnName);
//            if (ic != null) {
//                tcm.addColumn(ic.column);
//                int lastColumn = tcm.getColumnCount() - 1;
//                if (ic.index < lastColumn) {
//                    tcm.moveColumn(lastColumn, ic.index);
//                }
//            }
//        }
//        private class IndexedColumn {
//
//            private Integer index;
//            private TableColumn column;
//
//            public IndexedColumn(Integer index, TableColumn column) {
//                this.index = index;
//                this.column = column;
//            }
//        }
//    }
//
//    private TableColumnHider hider = null;
    
}