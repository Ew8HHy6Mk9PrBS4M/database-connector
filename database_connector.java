import java.util.List;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;

public class database_connector extends JFrame {

  private Connection connection;
  private JComboBox < String > databaseComboBox = new JComboBox < > ();
  private JComboBox < String > tableComboBox = new JComboBox < > ();
  private JTable table = new JTable();
  private DefaultTableModel tableModel = new DefaultTableModel();

  private final String DEFAULT_SERVER = "db4free.net";
  private final String DEFAULT_PORT = "3306";
  private final String DEFAULT_DATABASE = "placide_kallias";
  private final String DEFAULT_USER = "placide_kallias";
  private final String DEFAULT_PASSWORD = "deas2000";
  private final String DEFAULT_TABLE = "books";

  public database_connector() {
    // Set up the UI elements for the login screen
    JLabel serverLabel = new JLabel("Server:");
    JTextField serverField = new JTextField(DEFAULT_SERVER);
    JLabel portLabel = new JLabel("Port:");
    JTextField portField = new JTextField(DEFAULT_PORT);
    JButton connectButton = new JButton("Connect");
    JLabel databaseLabel = new JLabel("Database:");
    JLabel userLabel = new JLabel("Username:");
    JTextField userField = new JTextField(DEFAULT_USER);
    JLabel passwordLabel = new JLabel("Password:");
    JPasswordField passwordField = new JPasswordField();
    passwordField.setText(DEFAULT_PASSWORD);

    JPanel loginPanel = new JPanel();
    loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.PAGE_AXIS));
    loginPanel.add(serverLabel);
    loginPanel.add(serverField);
    loginPanel.add(portLabel);
    loginPanel.add(portField);
    loginPanel.add(userLabel);
    loginPanel.add(userField);
    loginPanel.add(passwordLabel);
    loginPanel.add(passwordField);
    loginPanel.add(connectButton);

    // Set default values
    serverField.setText(DEFAULT_SERVER);
    portField.setText(DEFAULT_PORT);
    userField.setText(DEFAULT_USER);

    // Set up the event handler for the Connect button
    connectButton.addActionListener(evt -> {
      try {
        String server = serverField.getText().trim();
        String port = portField.getText().trim();
        String username = userField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (server.isEmpty() || port.isEmpty() || username.isEmpty() || password.isEmpty()) {
          JOptionPane.showMessageDialog(null, "Please fill out all fields.", "Missing Information", JOptionPane.WARNING_MESSAGE);
          return;
        }

        // Load database names from the server and populate the dropdown box

        String url = "jdbc:mysql://" + server + ":" + port;
        connection = DriverManager.getConnection(url, username, password);

        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet resultSetDB = metaData.getCatalogs();

        while (resultSetDB.next()) {
          String dbName = resultSetDB.getString("TABLE_CAT");
          databaseComboBox.addItem(dbName);
        }

        databaseComboBox.setSelectedIndex(0);

        // Set up the UI elements for the main screen

        JLabel databaseLabel2 = new JLabel("Database:");
        tableComboBox.setEnabled(false);

        JButton disconnectButton = new JButton("Disconnect");

        databaseComboBox.addActionListener(evt2 -> {
          try {
            tableComboBox.removeAllItems();

            Statement statement = connection.createStatement();
            statement.execute("USE " + databaseComboBox.getSelectedItem());

            DatabaseMetaData metaData2 = connection.getMetaData();
            ResultSet resultSetTables = metaData2.getTables((String) databaseComboBox.getSelectedItem(), null, "%", null);

            while (resultSetTables.next()) {
              String tableName = resultSetTables.getString(3);
              tableComboBox.addItem(tableName);
            }

            tableComboBox.setEnabled(true);

          } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error occurred while populating tables dropdown: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
          }
        });

        tableComboBox.addActionListener(evt2 -> {
          try {
            // Clear out the old table contents and column names
            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);

            if (tableComboBox.getSelectedItem() != null) {
              Statement statement = connection.createStatement();
              PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + tableComboBox.getSelectedItem() + " LIMIT 100");
              ResultSet resultSetTableContents = preparedStatement.executeQuery();
              ResultSetMetaData rsmd = resultSetTableContents.getMetaData();
              int numOfCols = rsmd.getColumnCount();

              // Add the new column names to the table model
              for (int i = 1; i <= numOfCols; i++) {
                String colName = rsmd.getColumnName(i);
                tableModel.addColumn(colName);
              }

              while (resultSetTableContents.next()) {
                Object[] row = new Object[numOfCols];

                for (int i = 1; i <= numOfCols; i++) {
                  String value = resultSetTableContents.getString(i);
                  if (value == null) {
                    value = "";
                  }
                  row[i - 1] = value;
                }

                tableModel.addRow(row);
              }

              table.setModel(tableModel);
            }

          } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error occurred while populating table contents: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
          }
        });

        disconnectButton.addActionListener(evt2 -> {
          try {
            connection.close();
            setVisible(false);
            dispose();
            new database_connector().setVisible(true);
          } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error occurred while disconnecting: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
          }
        });

        JButton deleteButton = new JButton("Delete Row");
        deleteButton.addActionListener(evt2 -> {
          int selectedRow = table.getSelectedRow();
          if (selectedRow != -1) {
            Object primaryKeyValue = table.getValueAt(selectedRow, 0);

            // Check if the primary key value is null or empty
            if (primaryKeyValue == null || primaryKeyValue.toString().isEmpty()) {
              JOptionPane.showMessageDialog(null, "Cannot delete a row without a primary key value.", "Error", JOptionPane.WARNING_MESSAGE);
            } else {
              int option = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this row?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
              if (option == JOptionPane.YES_OPTION) {
                try {
                  String tableName = (String) tableComboBox.getSelectedItem();
                  Statement statement = connection.createStatement();
                  int id = Integer.parseInt(primaryKeyValue.toString());
                  PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM " + tableName + " WHERE id = ?");
                  preparedStatement.setInt(1, Integer.parseInt(primaryKeyValue.toString()));
                  preparedStatement.executeUpdate();
                  tableModel.removeRow(selectedRow);
                } catch (SQLException e) {
                  JOptionPane.showMessageDialog(null, "Error occurred while deleting row: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
              }
            }
          } else {
            JOptionPane.showMessageDialog(null, "Please select a row to delete.", "No Row Selected", JOptionPane.WARNING_MESSAGE);
          }
        });

        JButton updateButton = new JButton("Update Row");
        updateButton.addActionListener(evt2 -> {
          int selectedRow = table.getSelectedRow();
          int selectedColumn = table.getSelectedColumn();

          if (selectedRow != -1 && selectedColumn != -1) {
            String columnName = table.getColumnName(selectedColumn);

            // Check if the selected column is the primary key column
            boolean isPrimaryKey = isPrimaryKeyColumn(columnName);

            if (!isPrimaryKey) {
              try {
                String tableName = (String) tableComboBox.getSelectedItem();
                Statement statement = connection.createStatement();
                Object primaryKeyValue = table.getValueAt(selectedRow, 0);
                int id = Integer.parseInt(primaryKeyValue.toString());
                String columnValue = JOptionPane.showInputDialog(this, columnName + ": ", table.getValueAt(selectedRow, selectedColumn));
                PreparedStatement preparedStatement = connection.prepareStatement("UPDATE " + tableName + " SET " + columnName + "=? WHERE id=?");
                preparedStatement.setString(1, columnValue);
                preparedStatement.setInt(2, Integer.parseInt(primaryKeyValue.toString()));
                preparedStatement.executeUpdate();
                table.setValueAt(columnValue, selectedRow, selectedColumn);
              } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error occurred while updating row: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
              }
            } else {
              JOptionPane.showMessageDialog(null, "Cannot update a primary key value.", "Error", JOptionPane.WARNING_MESSAGE);
            }
          } else {
            JOptionPane.showMessageDialog(null, "Please select a cell to update.", "No Cell Selected", JOptionPane.WARNING_MESSAGE);
          }
        });

        JButton addButton = new JButton("Add Row");

        addButton.addActionListener(evt2 -> {
          String tableName = (String) tableComboBox.getSelectedItem();
          int numOfCols = tableModel.getColumnCount();

          // Create a list to hold the values entered by the user
          List < String > values = new ArrayList < > ();

          // Prompt the user to enter a value for each column
          for (int i = 0; i < numOfCols; i++) {
            String columnName = tableModel.getColumnName(i);
            String value = JOptionPane.showInputDialog(this, columnName + ": ");
            if (value == null) {
              // User clicked cancel
              return;
            }
            values.add(value);
          }

          try {
            // Build the INSERT statement using PreparedStatement
            String sql = "INSERT INTO " + tableName + " VALUES (";
            for (int i = 0; i < numOfCols; i++) {
              if (i > 0) {
                sql += ", ";
              }
              sql += "?";
            }
            sql += ")";
            PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            // Set the parameter values from the user input
            for (int i = 0; i < numOfCols; i++) {
              String value = values.get(i);
              preparedStatement.setString(i + 1, value);
            }

            // Execute the INSERT statement
            preparedStatement.executeUpdate();

            // Retrieve the auto-generated primary key value
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
              int primaryKeyValue = generatedKeys.getInt(1);
              Object[] row = new Object[numOfCols];
              row[0] = primaryKeyValue;
              // Assign values for other columns

            } else {
              throw new SQLException("Creating row failed, no ID obtained.");
            }

            // Add the new row to the table model
            Object[] row = new Object[numOfCols];
            for (int i = 0; i < numOfCols; i++) {
              String value = values.get(i);
              row[i] = value;
            }
            tableModel.addRow(row);

          } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error occurred while inserting row: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
          }
        });

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.add(databaseLabel2);
        mainPanel.add(databaseComboBox);
        mainPanel.add(tableComboBox);
        mainPanel.add(updateButton);
        mainPanel.add(deleteButton);
        mainPanel.add(addButton);
        mainPanel.add(disconnectButton);
        mainPanel.add(new JScrollPane(table));

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);

      } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error occurred while connecting to the database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    });

    setContentPane(loginPanel);
    pack();
    setLocationRelativeTo(null);
    setVisible(true);
  }

  private boolean isPrimaryKeyColumn(String columnName) {
    // Determine if the specified column is the primary key column for the selected table
    try {
      String tableName = (String) tableComboBox.getSelectedItem();
      DatabaseMetaData metaData = connection.getMetaData();
      ResultSet resultSet = metaData.getPrimaryKeys(null, null, tableName);
      while (resultSet.next()) {
        String pkColumnName = resultSet.getString("COLUMN_NAME");
        if (columnName.equals(pkColumnName)) {
          return true;
        }
      }
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error occurred while checking primary key: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    return false;
  }

  public static void main(String[] args) {
    new database_connector();
  }
}
