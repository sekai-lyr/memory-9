import java.sql.*;

public class ReadH2Schema4 {
    public static void main(String[] args) {
        String[][][] attempts = {
            {{"unencrypted sa/empty"}, {"jdbc:h2:file:D:/Sekai_two/memory-9/SekaiForm/data/sekai_friend;MODE=MySQL"}, {"sa", ""}},
            {{"unencrypted sekai/empty"}, {"jdbc:h2:file:D:/Sekai_two/memory-9/SekaiForm/data/sekai_friend;MODE=MySQL"}, {"sekai", ""}},
            {{"unencrypted sekai/123456520baba"}, {"jdbc:h2:file:D:/Sekai_two/memory-9/SekaiForm/data/sekai_friend;MODE=MySQL"}, {"sekai", "123456520baba"}},
            {{"encrypted FP=sekai UP=123456520baba"}, {"jdbc:h2:file:D:/Sekai_two/memory-9/SekaiForm/data/sekai_friend;MODE=MySQL;CIPHER=AES"}, {"sekai", "sekai 123456520baba"}},
            {{"encrypted FP=123456520baba UP=sekai"}, {"jdbc:h2:file:D:/Sekai_two/memory-9/SekaiForm/data/sekai_friend;MODE=MySQL;CIPHER=AES"}, {"sekai", "123456520baba sekai"}},
            {{"encrypted FP=123456520baba UP=123456520baba"}, {"jdbc:h2:file:D:/Sekai_two/memory-9/SekaiForm/data/sekai_friend;MODE=MySQL;CIPHER=AES"}, {"sekai", "123456520baba 123456520baba"}},
        };
        
        for (String[][] attempt : attempts) {
            String label = attempt[0][0];
            String url = attempt[1][0];
            String user = attempt[2][0];
            String pass = attempt[2][1];
            try {
                Class.forName("org.h2.Driver");
                Connection conn = DriverManager.getConnection(url, user, pass);
                System.out.println("SUCCESS: " + label);
                
                ResultSet tables = conn.getMetaData().getTables(null, "PUBLIC", "%", new String[]{"TABLE"});
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    System.out.println("\n--- TABLE: " + tableName + " ---");
                    ResultSet cols = conn.getMetaData().getColumns(null, "PUBLIC", tableName, "%");
                    while (cols.next()) {
                        System.out.println("  " + cols.getString("COLUMN_NAME") + " " + cols.getString("TYPE_NAME") + "(" + cols.getInt("COLUMN_SIZE") + ")");
                    }
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName);
                    rs.next();
                    System.out.println("  ROWS: " + rs.getInt(1));
                }
                conn.close();
                return;
            } catch (Exception e) {
                System.out.println("FAIL: " + label + " -> " + e.getMessage().substring(0, Math.min(100, e.getMessage().length())));
            }
        }
    }
}
