import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DBManager {
    String url;
    Connection conn = null;
    Statement stmt = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    public void doFinal(){
        try{ // 연결 해제(한정돼 있으므로)
            if(rs!=null){        rs.close();            }
            if(pstmt!=null){    pstmt.close();        }
            if(stmt!=null){    stmt.close();        }
            if(conn!=null){    conn.close();        }
        }catch(SQLException se2){
            se2.printStackTrace();
        }
    }

    public void connect(){
        try{
            Class.forName("com.mysql.jdbc.Driver"); // JDBC 드라이버 로드
            conn = DriverManager.getConnection("jdbc:mysql://35.229.98.104:3306/kaubrain_db", "root", "kaubrain123");
            if(conn==null){
                System.out.println("연결실패");
            }else {
                System.out.println("연결성공");
            }
        }catch(ClassNotFoundException ce){
        ce.printStackTrace();
    }catch(SQLException se){
        se.printStackTrace();
    }catch(Exception e){
        e.printStackTrace();
    }
    }

    public boolean checkStopWord(String checkWord, String checkPos){
        try {
            String sql = "select * from mostword";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            int count = 0;
            // 출력
            while (rs.next()) {
                if(rs.getString(1).equals(checkPos)){
                    return true;
                }
            }
            return false;
        }catch (Exception e){

        }
        return false;
    }

    public double extractEmotion(ArrayList<String> sample, ArrayList<Integer> index, ArrayList<String> origin, ArrayList<Integer> originHTI, ArrayList<String> pos){
        //google natural language의 상태값이 뒤에 세개
        //앞에 두개는 형태소 분석한 이후
        double sum = 0;
        int count = 0;
        try {
            String sql = "select word, mean, emotion from dict";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            // 출력
            while (rs.next()) {
                String dbString = rs.getString(1);
                double m = rs.getDouble(2);
                String emo = rs.getString(3);
                String[] dbSplit = dbString.split(" ");
                ArrayList<String> checkArray = new ArrayList<>();
                for(int i=0; i<dbSplit.length;i++){
                    checkArray.add(dbSplit[i]);
                }

                for(int i=0; i<checkArray.size();i++){
                    if(!sample.contains(checkArray.get(i))){
                        break;
                    }
                    if(i==checkArray.size()-1){
                        count++;
                        int sign = 1;
                        int idx = originHTI.get(index.get(sample.indexOf(checkArray.get(checkArray.size() - 2))));
                        int findNeg = index.get(sample.indexOf(checkArray.get(checkArray.size() - 2)));
                        int tag = 1;
                        for(int j=0;j<originHTI.size();j++){
                            if(originHTI.get(j) == findNeg && pos.get(j).equals("NEG")){
                                sign = -1;
                                tag = 0;
                            }
                        }
                        //while(tag == 1){

                       // }
                        while(tag == 1) {
                            if(origin.get(idx).contains("않") || origin.get(idx).contains("못") || origin.get(idx).contains("아니") || origin.get(idx).contains("없")){
                                sign = -1;
                                break;
                            } else if(pos.get(idx).equals("ROOT")) {
                                break;
                            } else {
                                idx = originHTI.get(idx);
                            }
                        }

                        if(emo.equals("POS")){
                            sum += m * sign / 10;
                        } else if((emo.equals("NEG"))){
                            sum -= m * sign / 10;
                        }
                    }
                }


            }
        }catch (Exception e){

        }

        if(count == 0)  return 0;
        return sum/count;
    }
}