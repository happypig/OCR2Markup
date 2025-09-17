package DILA.brokenlinkschecker.impl;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Table model. 
 */
@SuppressWarnings("serial")
public class CheckerTableModel extends AbstractTableModel {

	/**
	 * The displayed name of the column containing the start pages.
	 */
	public static final String START_PAGE_COLUMN_NAME = "Start page (URL)";

	/**
	 * The displayed name of the column containing the statuses.
	 */
	public static final String STATUS_COLUMN_NAME = "Status";

	/**
	 * The names of the columns.
	 */
	private Object[] columnNames = { CheckerTableModel.START_PAGE_COLUMN_NAME,
			CheckerTableModel.STATUS_COLUMN_NAME };

	/**
	 * The data from inside the table.
	 */
	private List<Object[]> data;

	/**
	 * No arguments constructor.
	 */
	public CheckerTableModel() {
		data = new ArrayList<Object[]>();
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		return data.get(row)[col];
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		data.get(row)[col] = value;
		fireTableCellUpdated(row, col);
	}
	
	@Override
	public String getColumnName(int column) {
	    return (String) columnNames[column];
	}
	
	/**
	 * Add a row at the end of the table.
	 */
	public void addRowAtEnd() {
		data.add(new String[2]);
	}

	/**
	 * Remove a row from the end of the table.
	 */
	public void removeRowFromEnd() {
		data.remove(data.size() - 1);
	}
	
	/**
	 * Add row at the given index.
	 * @param index the index where to row is to be added.
	 */
	public void addRowAtIndex(int index) {
		data.add(index, new String[2]);
	}
	
	/**
	 * Remove row from the given index.
	 * @param index the index from where the row is to be deleted.
	 */
	public void removeRowFromIndex(int index) {
		data.remove(index);
		fireTableRowsDeleted(index, index);
	}
	
	/**
	 * Add a new value at the end of the table.
	 * @param value the value to be added.
	 */
	public void addValueAtEnd(Object value) {
		data.add(new Object[]{value, null});
		fireTableRowsInserted(data.size() - 1, data.size() - 1);
	}

	/**
	 * Check if a cell is editable.
	 * @param row the row where the cell is found
	 * @param col the column where the cell is found
	 */
	public boolean isCellEditable(int row, int col) {
		return false;
	}
	
	/**
	 * Get the URL corresponding to a given row.
	 * @param row The index of the row.
	 * @return the url from the given row.
	 */
	public String getUrlForRow(int row) {
		return (String) data.get(row)[0];
	}
}
