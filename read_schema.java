import java.sql.*;

public class ReadH2Schema {
    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection(
            "jdbc:h2:file:D:/Sekai_two/memory-9/SekaiForm/data/sekai_friend;MODE=MySQL",
            "sa", "");
        
        // List all tables
        ResultSet tables = conn.getMetaData().getTables(null, "PUBLIC", "%", new String[]{"TABLE"});
        System.out.println("=== Tables ===");
        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            System.out.println("\n--- " + tableName + " ---");
            
            // Get columns
            ResultSet cols = conn.getMetaData().getColumns(null, "PUBLIC", tableName, "%");
            while (cols.next()) {
                String colName = cols.getString("COLUMN_NAME");
                String colType = cols.getString("TYPE_NAME");
                int colSize = cols.getInt("COLUMN_SIZE");
                String nullable = cols.getInt("NULLABLE") == 1 ? "NULL" : "NOT NULL";
                String def = cols.getString("COLUMN_DEF");
                System.out.println("  " + colName + " " + colType + "(" + colSize + ") " + nullable + (def != null ? " DEFAULT " + def : ""));
            }
        }
        conn.close();
    }
}
