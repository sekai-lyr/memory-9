import java.sql.*;

public class ReadH2Schema3 {
    public static void main(String[] args) {
        String[][] urls = {
            {"NO ENCRYPT", "jdbc:h2:file:D:/Sekai_two/memory-9/SekaiForm/data/sekai_friend;MODE=MySQL"},
            {"CIPHER=AES", "jdbc:h2:file:D:/Sekai_two/memory-9/SekaiForm/data/sekai_friend;MODE=MySQL;CIPHER=AES"},
        };
        
        for (String[] entry : urls) {
            String label = entry[0];
            String url = entry[1];
            try {
                Class.forName("org.h2.Driver");
                Connection conn = DriverManager.getConnection(url, "sekai", "123456520baba");
                System.out.println("SUCCESS: " + label);
                
                ResultSet tables = conn.getMetaData().getTables(null, "PUBLIC", "%", new String[]{"TABLE"});
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    System.out.println("\n--- TABLE: " + tableName + " ---");
                    ResultSet cols = conn.getMetaData().getColumns(null, "PUBLIC", tableName, "%");
                    while (cols.next()) {
                        String colName = cols.getString("COLUMN_NAME");
                        String colType = cols.getString("TYPE_NAME");
                        int colSize = cols.getInt("COLUMN_SIZE");
                        System.out.println("  " + colName + " " + colType + "(" + colSize + ")");
                    }
                    // Also show row count
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName);
                    rs.next();
                    System.out.println("  ROW COUNT: " + rs.getInt(1));
                }
                conn.close();
                return;
            } catch (Exception e) {
                System.out.println("FAILED: " + label + " - " + e.toString().substring(0, Math.min(120, e.toString().length())));
            }
        }
    }
}
