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

    // ����������� ���������� ����� ����� ���������.
    private static final int _hoursAfterLastRepost = Integer.valueOf(Settings.settings.get("hours_after_last_repost"));
    private static final int _excludePublicInterval = Integer.valueOf(Settings.settings.get("exclude_public_interval"));

 
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
    	String query = "SELECT dp.public_id, dp.post_id, dp.text, dp.likes_count, dp.reposts_count, dp.views_count, dp.post_datetime "
    			+ "FROM rank_processing as rp, downloaded_posts as dp "
				+ "WHERE rp.downloaded_public_id = dp.public_id AND rp.downloaded_post_id=dp.post_id "
				+ "and dp.is_approved = 1 "
				+ "AND dp.post_id NOT IN (SELECT DISTINCT downloaded_post_id FROM reposted_posts) "
				+ "AND dp.public_id NOT IN (SELECT DISTINCT downloaded_public_id FROM reposted_posts WHERE TIMESTAMP >= NOW() - INTERVAL ? HOUR)"
				+ "ORDER by rp.rank DESC";
    	ArrayList<DownloadedPost> list = new ArrayList<DownloadedPost> ();
    	PreparedStatement stmt = null;
        ResultSet rs = null;
    	 
        try {
        	openConnection();
        	stmt = con.prepareStatement(query);
        	// ���-�� ���� �� ������� ����, �� ������� ����� ��������� �����.
            //stmt.setInt(1, _dayToAnalyse);
            stmt.setInt(1, _excludePublicInterval);
            rs = stmt.executeQuery();
 
            while (rs.next()) {
            	DownloadedPost post = new DownloadedPost(
            			rs.getInt("public_id"),
            			rs.getInt("post_id"), 
            			rs.getString("text"), 
            			rs.getInt("likes_count"),
            			rs.getInt("reposts_count"),
            			rs.getInt("views_count"),
            			rs.getString("post_datetime"));
            	
            	list.add(post);	
            }
            
         LOG.info(String.format("������ ������ ��� �������� ������� ��������. ���-�� ������: %d.", list.size()));
         return list;
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� �������� ������ ������ ��� �������� �������� ������ �� ������ ��: %S", sqlEx.getMessage()));
        }
        catch (Exception e) {
            LOG.error(String.format("��� �������� ������ ������ ��� �������� �������� ������: %S", e.getMessage()));
        } 
        finally {
            closeConnection();
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return null;
    }

    // �����, ������������ ���������� � �������.
    public void insertRepostInfo (DownloadedPost post)
    {
    	LOG.debug(String.format("���������� ���������� � ������� � ��. PostId: %d, PublicId: %d", 
    			post.getPostId(),
    			post.getPublicId()));
    	openConnection();
    	
    	if (this.isReposted(post))
    	{
    		LOG.info(String.format("���������� �� ���� ������� ��� ���� � ����� ��. ��������� �������. PostId: %d, PublicId: %d.", 
        			post.getPostId(),
        			post.getPublicId()));
    		this.updatePostRepostsCount(post);
    	}
    	else
    	{
    		LOG.info(String.format("����� ������ � �������. ��������� � ��. PostId: %d, PublicId: %d.", 
        			post.getPostId(),
        			post.getPublicId()));
    		this.insetNewRepostInfo(post);    		
    	}	
    	closeConnection();
    }
    
    // �����, �����������, ���� �� ������ � ������� � ��.
    private boolean isReposted (DownloadedPost post)
    {
    	LOG.debug(String.format("���������, ���� �� ������ � ������� �����. PublicId: %d, postId %d.", 
    			post.getPublicId(), 
    			post.getPostId()));
    	
    	String query = 
    			"select count(1) as value from reposted_posts where downloaded_post_id = ? and downloaded_public_id = ?";
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
    	
        try {
        	stmt = con.prepareStatement(query);
        	stmt.setInt(1, post.getPostId());
        	stmt.setInt(2, post.getPublicId());
        	
            rs = stmt.executeQuery();
            // ���� � ������ ����� 1, ������ ����� ������ ����.
            if (rs.next()) {
            	
            	if (rs.getInt("value") == 1) 
            		{
            		LOG.debug(String.format("������ � ������� ��� ����. PublicId: %d, postId %d.", 
                			post.getPublicId(), 
                			post.getPostId()));
            		return true;
            		}
            }
            else
            {
            	LOG.debug(String.format("������ � ������� ��� ���. PublicId: %d, postId %d.", 
            			post.getPublicId(), 
            			post.getPostId()));
            	return false;
            }
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� �������� ������� ������ � ������� � �� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return false;
    }

    // �����, ������������� ������� ����� �������� �� ����� ������ �������.
    private void updatePostRepostsCount (DownloadedPost post)
    {
    	LOG.debug(String.format("��������� ������� ��������. PublicId: %d, postId %d", 
    			post.getPublicId(), 
    			post.getPostId()));
    	int counter = this.getCurrentRepostsCount(post);
    	
    	String query = 
    			"UPDATE reposted_posts SET COUNT = ?, TIMESTAMP = current_timestamp WHERE downloaded_post_id = ? AND downloaded_public_id = ?";
    	PreparedStatement stmt = null;
    	
    	try {
        	stmt = con.prepareStatement(query);
        	
        	stmt.setInt(1, counter++);
        	stmt.setInt(2, post.getPostId());
        	stmt.setInt(3, post.getPublicId());
        	
            stmt.executeUpdate();
            LOG.debug(String.format("������� �������� ��� ������� �������. PublicId: %d, postId %d,", 
    			post.getPublicId(), 
    			post.getPostId()));
           
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� ���������� �������� �������� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }  	
    }

    // �����, ������������ ������� ����� ���������� ������ � ����� �������
    private int getCurrentRepostsCount (DownloadedPost post)
    {
    	LOG.debug(String.format("�������� ������� ����� ���������� ����� � ����� �������. PublicId: %d, PostId: %d",
    			post.getPostId(),
    			post.getPostId()));
    	
    	// ��������� ������.
    	String query = 
    			"SELECT rp.count as 'count' FROM reposted_posts rp WHERE rp.downloaded_post_id = ? AND rp.downloaded_public_id = ?";
    	
    	PreparedStatement stmt = null;
        ResultSet rs = null;
    	 
        try {
        	stmt = con.prepareStatement(query);
        	// ���-�� ���� �� ������� ����, �� ������� ����� ��������� �����.
            stmt.setInt(1, post.getPostId());
            stmt.setInt(2, post.getPublicId());
            rs = stmt.executeQuery();
            int counter = 0;
 
            if (rs.next()) {
            	counter = rs.getInt("count");
            }
            else
            {
            	LOG.warn(String.format("�� ������� �������� ������� ����� ���������� ����� � ����� �������. PublicId: %d, PostId: %d",
            			post.getPostId(),
            			post.getPostId()));
            }
            
         LOG.debug(String.format("������� ����� ���������� ����� � ����� ������� ������� ���������. PublicId: %d, PostId: %d, �����: %d",
        			post.getPostId(),
        			post.getPostId(),
        			counter));
         return counter;
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� �������� ����� ���������� ����� � ����� ������� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return 0;
    }

    // �����, �������������� ������ � ����� �����, �������������� � ����� �������.
    private void insetNewRepostInfo(DownloadedPost post)
    {
    	LOG.debug(String.format("��������� ����� ������ � ������� � ���� ��. PublicId: %d, postId %d", 
    			post.getPublicId(), 
    			post.getPostId()));

    	String query = 
		"INSERT INTO reposted_posts(downloaded_post_id, downloaded_public_id, count) VALUES (?, ?, ?)";
    	PreparedStatement stmt = null;

		try {
			stmt = con.prepareStatement(query);
			
			stmt.setInt(1, post.getPostId());
			stmt.setInt(2, post.getPublicId());
			stmt.setInt(3, 1);
			
		    stmt.executeUpdate();
		    LOG.debug(String.format("����� ������ � ������� ������� ���� ��������� � ���� ��. PublicId: %d, postId %d", 
		    			post.getPublicId(), 
		    			post.getPostId()));
		} 
		catch (SQLException sqlEx) {
		    LOG.error(String.format("��� ���������� ����� ������ � ������� � �� �������� ������: %S", sqlEx.getMessage()));
		} 
		finally {
		    try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
		}
    }

    // �����, ������������ ���� � ���, ����� �� ������ ������ ������ ��� ��� (������� �� ��������� hours_after_last_repost)
    public boolean isRepostAllowNow()
    {
    	LOG.debug("���������, ����� �� ������ ���������.");
    	String query = "select count(*) as 'count' from reposted_posts rp where rp.timestamp >=  (current_timestamp - INTERVAL ? HOUR)";
    	PreparedStatement stmt = null;
        ResultSet rs = null;
    	 
        try {
        	openConnection();
        	stmt = con.prepareStatement(query);
            stmt.setInt(1, _hoursAfterLastRepost);
            rs = stmt.executeQuery();
            int count = 0;
 
            if (rs.next()) {
            	count = rs.getInt("count");
            }
            
            if (count > 0)
            {
            	LOG.debug("������ ��������.");
            	return false;
            }
            
            LOG.debug("������ ��������."); 
            return true;
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� �������� �������� ���������� �� ������ �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            closeConnection();
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
        return true;
    }
}
