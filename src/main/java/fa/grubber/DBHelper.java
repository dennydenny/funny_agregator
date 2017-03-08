package fa.grubber;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vk.api.sdk.objects.wall.WallpostFull;

public class DBHelper {

    // JDBC URL, username and password of MySQL server
    private static final String url = Settings.settings.get("database_url");
    private static final String user = Settings.settings.get("database_user");
    private static final String password = Settings.settings.get("database_password");
    private static final Logger LOG = LoggerFactory.getLogger(DBHelper.class);
 
    // JDBC variables for opening and managing connection
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
    
    // �����, ������������ ��������� ��������.
    public ArrayList<Public> getPublics()
    {
    	LOG.debug("�������� �������� ������ ��������.");
    	String query = "select id, name, url, subs_count from publics";
    	ArrayList<Public> list = new ArrayList<Public> ();
        Statement stmt = null;
        ResultSet rs = null;
    	 
        try {
        	openConnection();
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);
 
            while (rs.next()) {
            	Public pub = new Public();
            	
            	pub.setPublicId(rs.getInt("id"));
            	pub.setPublicName(rs.getString("Name"));
            	pub.setPublicUrl(rs.getString("URL"));
            	pub.setPublicSubsCount(rs.getInt("Subs_count"));
            	
            	list.add(pub);	
            }
         LOG.info("������ �������� ������� ��������.");
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� �������� ������ �������� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            closeConnection();
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
        return list;
    }

    // �����, ������������ ����� � ��.
    public void WriteDownloadedPosts (List<WallpostFull> posts)
    {
    	if (posts.isEmpty()) throw new IllegalStateException("������ ������ ��� ������ ����.");
    	
    	//TODO: ������� ������ "�����������" � ��������� �����.
    	
    	// "������������ WallpostFull � ��� DownloadedPost.
    	ArrayList<DownloadedPost> downloadedPosts = new ArrayList<DownloadedPost>();
    	for (WallpostFull post : posts)
    	{
    		DownloadedPost dpost = new DownloadedPost(post.getOwnerId() * -1, 
    				post.getId(), 
    				post.getText(), 
    				post.getLikes().getCount(), 
    				post.getReposts().getCount(),
    				post.getDate());
    		downloadedPosts.add(dpost);
    	}
    	
    	openConnection();	
    	// ��������� ������� ������� ����� � ����� ��.
    	for (DownloadedPost dpost : downloadedPosts)
    	{
    		if (isPostExist(dpost))
    		{
    			LOG.info(String.format("���� ��� ���� � ����� ��. PublicId: %d, postId %d", 
    					dpost.getPublicId(), 
    					dpost.getPostId()));
    			
    			// ���������, ���������� �� ���-�� ������ � �����.
    			if (!isLikeCountActual(dpost))
    			{
    				LOG.info(String.format("���������� � �� � ����� �����������. ��������� ����������. PublicId: %d, postId %d", 
        					dpost.getPublicId(), 
        					dpost.getPostId()));
    				updateDBPostInfo(dpost);
    			}
    			else
    			{
    				LOG.info(String.format("���������� � �� � ����� ���������. PublicId: %d, postId %d", 
        					dpost.getPublicId(), 
        					dpost.getPostId()));
    			}
    		}
    		else
    		{
    			LOG.info(String.format("����� ����. ���������� � ���� ��. PublicId: %d, postId %d", 
    					dpost.getPublicId(), 
    					dpost.getPostId()));
    			insertNewPost(dpost);
    		}
    	}	
    	closeConnection();
    }
    
    // �����, �������������� ��������, ���� �� ����� ���� � ����� ��.
    private boolean isPostExist(DownloadedPost post)
    {
    	LOG.debug(String.format("���������, ���� �� ���� � ����� ��. PublicId: %d, postId %d", 
    			post.getPublicId(), 
    			post.getPostId()));
    	
    	String query = "select count(1) as value from downloaded_posts dp where dp.public_id = ? and dp.post_id = ?";
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
    	
        try {
        	stmt = con.prepareStatement(query);
        	stmt.setInt(1, post.getPublicId());
        	stmt.setInt(2, post.getPostId());
        	
            rs = stmt.executeQuery();
            // ���� � ������ ����� 1, ������ ����� ���� ����.
            if (rs.next()) {
            	
            	if (rs.getInt("value") == 1) 
            		{
            		LOG.debug(String.format("����� ���� ���� � ����� ��. PublicId: %d, postId %d",
                			post.getPublicId(), 
                			post.getPostId()));
            		return true;
            		}
            }
            else
            {
            	LOG.debug(String.format("������ ����� ��� � ����� ��. PublicId: %d, postId %d",
            			post.getPublicId(), 
            			post.getPostId()));
            	return false;
            }
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� �������� ������� ����� � �� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return false;
    }

    // �����, ����������� ���������� � ������������ ����� � ����� ��.
    private void updateDBPostInfo(DownloadedPost post)
    {
    	LOG.debug(String.format("��������� ��������� � ����� � ����� ��. PublicId: %d, postId %d", 
    			post.getPublicId(), 
    			post.getPostId()));

    	// �������� ���������� � ���-�� ������ �� ����������.
    	int oldLikesCount = getPostLikesCountFromDB(post);
    	
    	String query = "update downloaded_posts set likes_count = ?, reposts_count = ?, timestamp = current_timestamp where public_id = ? and post_id = ?";
    	PreparedStatement stmt = null;
    	
    	try {
        	stmt = con.prepareStatement(query);
        	
        	stmt.setInt(1, post.getLikesCount());
        	stmt.setInt(2, post.getRepostsCount());
        	stmt.setInt(3, post.getPublicId());
        	stmt.setInt(4, post.getPostId());
        	
            stmt.executeUpdate();
            LOG.debug(String.format("���������� � ����� ���� ������ ���������. PublicId: %d, postId %d", 
        			post.getPublicId(), 
        			post.getPostId()));
            
            LOG.debug(String.format("���� ������ - %d, ����� ������ - %d. PublicId: %d, postId %d", 
            		oldLikesCount,
            		post.getLikesCount(),
        			post.getPublicId(), 
        			post.getPostId()));
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� ���������� ���������� � ����� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
    }
    
    // �����, ������������ ���-�� ������ ��� ����������� �����, ������� �������� � ��.
    private int getPostLikesCountFromDB (DownloadedPost post)
    {
    	LOG.debug(String.format("��������� ��������� � ���-�� ������ ����� �� ����� ��. postId %d",  
    			post.getPostId()));
    	String query = "select likes_count from downloaded_posts where public_id = ? and post_id = ?";
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
    	
    	try {
        	stmt = con.prepareStatement(query);
        	
        	stmt.setInt(1, post.getPublicId());
        	stmt.setInt(2, post.getPostId());
        	
            rs = stmt.executeQuery();
            
            
            if (rs.next()) {
            	return rs.getInt("likes_count");
            }
            LOG.debug(String.format("���������� � ���-�� ������ ���� ������ ���������. postId %d",
        			post.getPostId()));
            return 0;
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� ���������� ���������� � ���-�� ������ � ����� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return 0;	
    }

    // �����, �������������� ������ ������ �����, �������� ��� � ����� ��.
    private void insertNewPost(DownloadedPost post)
    {
    	LOG.debug(String.format("��������� ����� ���� � ���� ��. PublicId: %d, postId %d", 
    			post.getPublicId(), 
    			post.getPostId()));
 
    	String query = 
    			"INSERT INTO downloaded_posts(public_id, post_id, text, likes_count, reposts_count, post_datetime) VALUES (?, ?, ?, ?, ?, ?)";
    	PreparedStatement stmt = null;
    	
    	try {
        	stmt = con.prepareStatement(query);
        	
        	stmt.setInt(1, post.getPublicId());
        	stmt.setInt(2, post.getPostId());
        	stmt.setString(3, post.getText());
        	stmt.setInt(4, post.getLikesCount());
        	stmt.setInt(5, post.getRepostsCount());
        	stmt.setString(6, post.getPostDatetime());
        	
            stmt.executeUpdate();
            LOG.debug(String.format("����� ���� ������� ��� �������� � ���� ��. PublicId: %d, postId %d", 
        			post.getPublicId(), 
        			post.getPostId())); 
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� ���������� ������ ����� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
    }

    // �����, �����������, ��������� �� ����� ������ � �� �� ��������� � ���������� ������.
    private boolean isLikeCountActual (DownloadedPost post)
    {
    	LOG.debug(String.format("���������, ��������� �� ����� ������ ����� � ��. PublicId: %d, postId %d", 
    			post.getPublicId(), 
    			post.getPostId()));
    	
    	String query = "select likes_count, reposts_count from downloaded_posts dp where dp.public_id = ? and dp.post_id = ?";
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
    	
        try {
        	stmt = con.prepareStatement(query);
        	stmt.setInt(1, post.getPublicId());
        	stmt.setInt(2, post.getPostId());
        	
            rs = stmt.executeQuery();

            if (rs.next()) {
            	int dbLikes = rs.getInt("likes_count");
            	int dbReposts = rs.getInt("reposts_count");
            	
            	if ((dbLikes != post.getLikesCount()) || (dbReposts != post.getRepostsCount()))
            	{
            		LOG.debug(String.format("���-�� ������ ����� � �� �� ���������. PublicId: %d, postId %d",
                			post.getPublicId(), 
                			post.getPostId()));
            		return false;
            	}
            	else
            	{
            		LOG.debug(String.format("���-�� ������ ����� � �� ���������. PublicId: %d, postId %d",
                			post.getPublicId(), 
                			post.getPostId()));
            		return true;
            	}
            }
            else
            {
            	LOG.debug(String.format("��� �������� ������������ ����� ������ �� ������� ����� ����� ����. PublicId: %d, postId %d",
            			post.getPublicId(), 
            			post.getPostId()));
            	return false;
            }
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� �������� ������� ����� � �� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return false;	
    }

    // �����, �������������� ������ ����������� ����� ����������� �������.
    public void updatePublicSubsCount(Public pub, int newCount)
    {
    	LOG.debug(String.format("��������� ���������� � ���-�� ����������� � ��. PublicId: %d ", pub.getPublicId()));
    	
    	String query = "update publics set Subs_count = ?, timestamp = current_timestamp where id = ?";
    	PreparedStatement stmt = null;
    	
        try {
        	openConnection();
        	stmt = con.prepareStatement(query);
        	stmt.setInt(1, newCount);
        	stmt.setInt(2, pub.getPublicId());
        	
            stmt.executeUpdate();

            LOG.debug(String.format("���� � ���-�� ������������ ������� ��������� � ��. ����: %d, �����: %d, PublicId: %d", 
					pub.getPublicSubsCount(),
					newCount,
					pub.getPublicId()));
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� ���������� ���-�� ����������� � �� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
        	closeConnection();
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
    }
}