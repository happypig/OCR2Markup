package simple.documentation.framework.operations;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Properties;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;

/**
 * Query DB operation.
 */
public class QueryDatabaseOperation implements AuthorOperation{
  private static String ARG_JDBC_DRIVER ="jdbc_driver";
  private static String ARG_USER ="user";
  private static String ARG_PASSWORD ="password";
  private static String ARG_SQL ="sql";
  private static String ARG_CONNECTION ="connection";

  /**
   * @return The array of arguments the developer must specify when
   * configuring the action.
   */
  public ArgumentDescriptor[] getArguments() {
    ArgumentDescriptor args[] = new ArgumentDescriptor[] {
        new ArgumentDescriptor(
            ARG_JDBC_DRIVER,
            ArgumentDescriptor.TYPE_STRING,
        "The name of the Java class that is the JDBC driver."),
        new ArgumentDescriptor(
            ARG_CONNECTION,
            ArgumentDescriptor.TYPE_STRING,
        "The database URL connection string."),
        new ArgumentDescriptor(
            ARG_USER,
            ArgumentDescriptor.TYPE_STRING,
        "The name of the database user."),
        new ArgumentDescriptor(
            ARG_PASSWORD,
            ArgumentDescriptor.TYPE_STRING,
        "The database password."),
        new ArgumentDescriptor(
            ARG_SQL,
            ArgumentDescriptor.TYPE_STRING,
        "The SQL statement to be executed.")
    };
    return args;
  }

  /**
   * @return The operation description.
   */
  public String getDescription() {
    return "Executes a database query and puts the result in a table.";
  }

  public void doOperation(AuthorAccess authorAccess, ArgumentsMap map)
  throws IllegalArgumentException, AuthorOperationException {
    // Collects the arguments.
    String jdbcDriver =
      (String)map.getArgumentValue(ARG_JDBC_DRIVER);
    String connection =
      (String)map.getArgumentValue(ARG_CONNECTION);
    String user =
      (String)map.getArgumentValue(ARG_USER);
    String password =
      (String)map.getArgumentValue(ARG_PASSWORD);
    String sql =
      (String)map.getArgumentValue(ARG_SQL);
    int caretPosition = authorAccess.getEditorAccess().getCaretOffset();
    try {
      if (jdbcDriver == null) {
        throw new AuthorOperationException("No jdbc driver provided.");
      }
      if (connection == null) {
        throw new AuthorOperationException("No connection provided.");
      }
      if (user == null) {
        throw new AuthorOperationException("No user provided.");
      }
      if (sql == null) {
        throw new AuthorOperationException("No sql provided.");
      }
      authorAccess.getDocumentController().insertXMLFragment(
          getFragment(jdbcDriver, connection, user, password, sql),
          caretPosition);
    } catch (SQLException e) {
      throw new AuthorOperationException(
          "The operation failed due to the following database error: " +
          e.getMessage(), e);
    } catch (ClassNotFoundException e) {
      throw new AuthorOperationException(
          "The JDBC database driver was not found. Tried to load ' " +
          jdbcDriver + "'", e);
    }
  }

  /**
   * Creates a connection to the database, executes
   * the SQL statement and creates an XML fragment
   * containing a table element that wraps the data
   * from the result set.
   *
   *
   * @param jdbcDriver The class name of the JDBC driver.
   * @param connectionURL The connection URL.
   * @param user The database user.
   * @param password The password.
   * @param sql The SQL statement.
   * @return The string containing the XML fragment.
   *
   * @throws SQLException thrown when there is a
   * problem accessing the database or there are
   * erors in the SQL expression.
   * @throws ClassNotFoundException when the JDBC
   * driver class could not be loaded.
   */
  private String getFragment(
      String jdbcDriver,
      String connectionURL,
      String user,
      String password,
      String sql) throws
      SQLException,
      ClassNotFoundException {
    Properties pr = new Properties();
    pr.put("characterEncoding", "UTF8");
    pr.put("useUnicode", "TRUE");
    pr.put("user", user);
    pr.put("password", password);
    // Loads the database driver.
    Class.forName(jdbcDriver);
    // Opens the connection
    Connection connection =
      DriverManager.getConnection(connectionURL, pr);
    java.sql.Statement statement =
      connection.createStatement();
    ResultSet resultSet =
      statement.executeQuery(sql);
    StringBuffer fragmentBuffer = new StringBuffer();
    fragmentBuffer.append(
    "<table xmlns='http://www.oxygenxml.com/sample/documentation'>");
    //
    // Creates the table header.
    //
    fragmentBuffer.append("<header>");
    ResultSetMetaData metaData = resultSet.getMetaData();
    int columnCount = metaData.getColumnCount();
    for (int i = 1; i <= columnCount; i++) {
      fragmentBuffer.append("<td>");
      fragmentBuffer.append(
          xmlEscape(metaData.getColumnName(i)));
      fragmentBuffer.append("</td>");
    }
    fragmentBuffer.append("</header>");
    //
    // Creates the table content.
    //
    while (resultSet.next()) {
      fragmentBuffer.append("<tr>");
      for (int i = 1; i <= columnCount; i++) {
        fragmentBuffer.append("<td>");
        fragmentBuffer.append(
            xmlEscape(resultSet.getObject(i)));
        fragmentBuffer.append("</td>");
      }
      fragmentBuffer.append("</tr>");
    }
    fragmentBuffer.append("</table>");
    // Cleanup
    resultSet.close();
    statement.close();
    connection.close();
    return fragmentBuffer.toString();
  }

  /**
   * Some of the values from the database table
   * may contain characters that must be escaped
   * in XML, to ensure the fragment is well formed.
   *
   * @param object The object from the database.
   * @return The escaped string representation.
   */
  private String xmlEscape(Object object) {
    String str = String.valueOf(object);
    return str.
    replaceAll("&", "&amp;").
    replaceAll("<", "&lt;");
  }
}
