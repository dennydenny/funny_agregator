package fa.rankprocessing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fa.common.DownloadedPost;
import fa.common.Public;
import fa.common.Settings;

public class RankProcessingDBHelper {

    private static final String url = Settings.settings.get("database_url");
    private static final String user = Settings.settings.get("database_user");
    private static final String password = Settings.settings.get("database_password");
    private static final Logger LOG = LoggerFactory.getLogger(RankProcessingDBHelper.class);
    // ���-�� ���� �� �������� �������, �� ������� ���������� ������.
    private static final int _dayToAnalyse = Integer.valueOf(Settings.settings.get("day_to_analyse"));
 
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

    // �����, ������������ ����� � ��, ���������� ������.
    public List<DownloadedPost> getPostsForRanking(Public pub)
    {
    	LOG.debug("�������� �������� ������ ������ ��� ������.");
    	String query = 
    			"SELECT public_id, post_id, text, likes_count, reposts_count, post_datetime FROM downloaded_posts "
    			+ "WHERE post_datetime >= (NOW() - INTERVAL ? DAY) AND public_id = ?";
    	ArrayList<DownloadedPost> list = new ArrayList<DownloadedPost> ();
    	PreparedStatement stmt = null;
        ResultSet rs = null;
    	 
        try {
        	openConnection();
        	stmt = con.prepareStatement(query);
        	// ���-�� ���� �� ������� ����, �� ������� ����� ��������� �����.
            stmt.setInt(1, _dayToAnalyse);
            stmt.setInt(2, pub.getPublicId());
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
            
         LOG.info(String.format("������ ������ ��� ������ ������� ��������. ���-�� ������: %d.", list.size()));
         return list;
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� �������� ������ ������ ��� ������ �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            closeConnection();
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return null;
    }

    // ����� ��� ��������� ���-�� ������ ���� ������ �� ������, ��������� � _dayToAnalyse.
    public int getLikesCountForPeriod(Public pub)
    {
    	LOG.debug(String.format("��������� ��������� ���-�� ������. PublicId: %d", pub.getPublicId()));
    	String query = 
    			"SELECT sum(likes_count) as 'likes'  FROM downloaded_posts WHERE post_datetime >= (NOW() - INTERVAL ? DAY) AND public_id = ?";
    	PreparedStatement stmt = null;
        ResultSet rs = null;
    	 
        try {
        	openConnection();
        	stmt = con.prepareStatement(query);
        	
        	// ���-�� ���� �� ������� ����, �� ������� ����� ��������� �����.
            stmt.setInt(1, _dayToAnalyse);
            stmt.setInt(2, pub.getPublicId());
            rs = stmt.executeQuery();
            
 
            if (rs.next()) {
            	int likes = rs.getInt("likes");
            	LOG.info(String.format("��������� ���-�� ������ ������� ���������. ���-�� ������: %d, PublicId: %d.", 
            			likes,
            			pub.getPublicId()));
            	return likes;
            }
            LOG.error(String.format("�� ������� ��������� ��������� ���-�� ������. PublicId: %d.", pub.getPublicId()));
        } 
        catch (Exception e) {
            LOG.error(String.format("��� �������� ���������� ���-�� ������ �������� ������: %S", e.getMessage()));
        } 
        finally {
            closeConnection();
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return 0;
    }

    // �����, �������������� ���������� ���������� �� ������� � ��.
    public void setRankToDB(Map<DownloadedPost, Integer> ranks, AbstractRule rule)
    {
    	LOG.debug(String.format("������������� ������ ����� � ��. �������: %s.", rule.getRuleName()));
    	
    	openConnection();
    	// ��������� ������� ������ �� ���� �������.
    	// ���� ������ ����, �� ��������� ������ � �����. ���� ���, �� ��������� �����.
    	for (Map.Entry<DownloadedPost, Integer> entry : ranks.entrySet())
    	{
    		DownloadedPost post = entry.getKey();
    		int rank = entry.getValue();
    		
    		if (this.isRankRecordExist(post, rule))
    		{
    			LOG.info(String.format("������ �� ������ ��� ����. ��������� ������. PublicId: %d, postId %d, �������: %s", 
    	    			post.getPublicId(), 
    	    			post.getPostId(),
    	    			rule.getRuleName()));
    			this.updateRuleRank(post, rule, rank);
    		}
    		else
    		{
    			LOG.info(String.format("������ �� ������ ���. ���������. PublicId: %d, postId %d, �������: %s", 
    	    			post.getPublicId(), 
    	    			post.getPostId(),
    	    			rule.getRuleName()));
    			this.insertNewRuleRecord(post, rule, rank);
    		}		
    	}
    	LOG.debug(String.format("��������� ������ ������ � �� ���������. �������: %s", rule.getRuleName()));
    	closeConnection();    	
    }

    // �����, �������������� �������� ������� ������ �� ������ ����������� �����, �������� ����������� �������.
    private boolean isRankRecordExist(DownloadedPost post, AbstractRule rule)
    {
    	LOG.debug(String.format("���������, ���� �� ������ �� ������ �����. PublicId: %d, postId %d, �������: %s", 
    			post.getPublicId(), 
    			post.getPostId(), 
    			rule.getRuleName()));
    	
    	String query = 
    			"select count(1) as value from rank_processing where downloaded_post_id = ? and rule_name = ? and downloaded_public_id = ?";
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
    	
        try {
        	stmt = con.prepareStatement(query);
        	stmt.setInt(1, post.getPostId());
        	stmt.setString(2, rule.getRuleName());
        	stmt.setInt(3, post.getPublicId());
        	
            rs = stmt.executeQuery();
            // ���� � ������ ����� 1, ������ ����� ������ ����.
            if (rs.next()) {
            	
            	if (rs.getInt("value") == 1) 
            		{
            		LOG.debug(String.format("����� ������ ��� ����. PublicId: %d, postId %d, �������: %s", 
                			post.getPublicId(), 
                			post.getPostId(),
                			rule.getRuleName()));
            		return true;
            		}
            }
            else
            {
            	LOG.debug(String.format("������ � ����� ������ ��� ���. PublicId: %d, postId %d, �������: %s", 
            			post.getPublicId(), 
            			post.getPostId()),
            			rule.getRuleName());
            	return false;
            }
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� �������� ������� ������ �� ������ � �� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return false;
    }

    // �����, ����������� ���������� �� ������ ������� �����, �������� ����������� �������.
    private void updateRuleRank(DownloadedPost post, AbstractRule rule, int rank)
    {
    	LOG.debug(String.format("��������� ������. PublicId: %d, postId %d, �������: %s", 
    			post.getPublicId(), 
    			post.getPostId(),
    			rule.getRuleName()));
    	
    	String query = 
    			"UPDATE rank_processing SET RANK = ?, TIMESTAMP = current_timestamp WHERE downloaded_post_id = ? AND rule_name = ? AND downloaded_public_id = ?";
    	PreparedStatement stmt = null;
    	
    	try {
        	stmt = con.prepareStatement(query);
        	
        	stmt.setInt(1, rank);
        	stmt.setInt(2, post.getPostId());
        	stmt.setString(3, rule.getRuleName());
        	stmt.setInt(4, post.getPublicId());
        	
            stmt.executeUpdate();
            LOG.debug(String.format("���������� �� ������ ���� ������ ���������. PublicId: %d, postId %d, �������: %s", 
    			post.getPublicId(), 
    			post.getPostId(),
    			rule.getRuleName()));
           
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� ���������� ���������� � ����� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
    }

    // �����, �������������� ������� ����� ������ �� ������ �����, �������� ����������� �������.
    private void insertNewRuleRecord (DownloadedPost post, AbstractRule rule, int rank)
    {
    	LOG.debug(String.format("��������� ����� ������ �� ������ � ���� ��. PublicId: %d, postId %d, �������: %s", 
    	    			post.getPublicId(), 
    	    			post.getPostId(),
    	    			rule.getRuleName()));
 
    	String query = 
    			"INSERT INTO rank_processing(downloaded_post_id, rule_name, rank, downloaded_public_id) VALUES (?, ?, ?, ?)";
    	PreparedStatement stmt = null;
    	
    	try {
        	stmt = con.prepareStatement(query);
        	
        	stmt.setInt(1, post.getPostId());
        	stmt.setString(2, rule.getRuleName());
        	stmt.setInt(3, rank);
        	stmt.setInt(4, post.getPublicId());
        	
            stmt.executeUpdate();
            LOG.debug(String.format("����� ������ �� ������ ������� ���� ��������� � ���� ��. PublicId: %d, postId %d, �������: %s", 
    	    			post.getPublicId(), 
    	    			post.getPostId(),
    	    			rule.getRuleName()));
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� ���������� ����� ������ �� ������ � �� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
    }

    // �����, ������������ ����� ������ ��� ����� �� ���� ��������, ����� SUMMARY.
    public int getRulesRankSumForPost (DownloadedPost post)
    {
    	LOG.debug(String.format("�������� �������� ����� ������ ��� �����. PublicId: %d, PostId: %d",
    			post.getPostId(),
    			post.getPostId()));
    	
    	// ��������� ������.
    	String query = 
    			"SELECT SUM(rank) as 'sum' FROM rank_processing WHERE downloaded_post_id = ? AND downloaded_public_id = ? "
    			+ "AND rule_name != 'SUMMARY'";
    	
    	PreparedStatement stmt = null;
        ResultSet rs = null;
    	 
        try {
        	openConnection();
        	stmt = con.prepareStatement(query);
        	// ���-�� ���� �� ������� ����, �� ������� ����� ��������� �����.
            stmt.setInt(1, post.getPostId());
            stmt.setInt(2, post.getPublicId());
            rs = stmt.executeQuery();
            int rankSum = 0;
 
            if (rs.next()) {
            	rankSum = rs.getInt("sum");
            }
            
         LOG.debug(String.format("C���� ������ ��� ����� ������� ���������. PublicId: %d, PostId: %d, �����: %d",
        			post.getPostId(),
        			post.getPostId(),
        			rankSum));
         return rankSum;
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("��� �������� ����� ������ ��� ����� �������� ������: %S", sqlEx.getMessage()));
        } 
        finally {
            closeConnection();
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return 0;
    }
}