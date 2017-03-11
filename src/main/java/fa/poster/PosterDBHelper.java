package fa.poster;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fa.common.DownloadedPost;
import fa.common.Settings;

public class PosterDBHelper {
	
    private static final String url = Settings.settings.get("database_url");
    private static final String user = Settings.settings.get("database_user");
    private static final String password = Settings.settings.get("database_password");
    private static final Logger LOG = LoggerFactory.getLogger(PosterDBHelper.class);
    // ���-�� ���� �� �������� �������, �� ������� ���������� ������.
    private static final int _dayToAnalyse = Integer.valueOf(Settings.settings.get("day_to_analyse"));
    // ����� �� ���-�� �������� ����� ������ �� �����.
    private static final int _repostsLimit = Integer.valueOf(Settings.settings.get("reposts_count_limit"));
 
    private static Connection con;
    
    // ����� ����������� � ��.
    private void openConnection()
    {
    	try {
			con = DriverManager.getConnection(url, user, password);
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }
    
    // ����� �������� ����������� � ��.
    private void closeConnection()
    {
    	try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }

    // �����, ������������ ������ ������, ���������� ��� ������� ��������.
    /*
     * �������:
     * 1. ���� ������ ��� ����� ���-�� ���� �� ��������.
     * 		�
     * 2. ��� ����� ���������� ������ � ��������� �������.
     * 		�
     * 3. ���� ���� ����� �� ����������
     * 		���
     * 4. ���� ���� ����� ����������, �� ������ ��� ������ ����� X ���� �����.
     * 		�
     * 5. ���-�� �������� ���� ������� �� ����� Y ���.
     */
    public List<DownloadedPost> getPostsForPosting ()
    {
    	LOG.debug("�������� �������� ������ ������ ��� ��������");
    	String query = 
    			"SELECT dp.public_id, dp.post_id, dp.text, dp.likes_count, dp.reposts_count, dp.post_datetime "
    			+ "FROM downloaded_posts dp, rank_processing rp "
    			+ "WHERE dp.post_id=rp.downloaded_post_id AND "
    			+ "dp.public_id=rp.downloaded_public_id AND "
    			+ "rp.rule_name='SUMMARY'AND "
    			+ "dp.post_datetime >= (NOW() - INTERVAL ? DAY) AND "
    			+ "dp.post_id NOT IN (select downloaded_post_id FROM reposted_posts WHERE count < ?)"
    			+ "order by rp.rank desc";
    	ArrayList<DownloadedPost> list = new ArrayList<DownloadedPost> ();
    	PreparedStatement stmt = null;
        ResultSet rs = null;
    	 
        try {
        	openConnection();
        	stmt = con.prepareStatement(query);
        	// ���-�� ���� �� ������� ����, �� ������� ����� ��������� �����.
            stmt.setInt(1, _dayToAnalyse);
            stmt.setInt(1, _repostsLimit);
            rs = stmt.executeQuery();
 
            while (rs.next()) {
            	DownloadedPost post = new DownloadedPost(
            			rs.getInt("public_id"),
            			rs.getInt("post_id"), 
            			rs.getString("text"), 
            			rs.getInt("likes_count"),
            			rs.getInt("reposts_count"),
            			rs.getString("post_datetime"));
            	
            	list.add(post);	
            }
            
         LOG.info(String.format("������ ������ ��� �������� ������� ��������. ���-�� ������: %d.", list.size()));
         return list;
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� �������� ������ ������ ��� �������� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            closeConnection();
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return null;
    }
}
