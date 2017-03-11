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
    // Кол-во дней от текущего времени, за которые проводится оценка.
    private static final int _dayToAnalyse = Integer.valueOf(Settings.settings.get("day_to_analyse"));
 
    private static Connection con;
    
    // Метод подключения к БД.
    private void openConnection()
    {
    	try {
			con = DriverManager.getConnection(url, user, password);
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }
    
    // Метод закрытия подключения к БД.
    private void closeConnection()
    {
    	try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }

    // Метод, возвращающий посты с БД, подлежащие оценке.
    public List<DownloadedPost> getPostsForRanking(Public pub)
    {
    	LOG.debug("Начинаем загрузку списка постов для оценки.");
    	String query = 
    			"SELECT public_id, post_id, text, likes_count, reposts_count, post_datetime FROM downloaded_posts "
    			+ "WHERE post_datetime >= (NOW() - INTERVAL ? DAY) AND public_id = ?";
    	ArrayList<DownloadedPost> list = new ArrayList<DownloadedPost> ();
    	PreparedStatement stmt = null;
        ResultSet rs = null;
    	 
        try {
        	openConnection();
        	stmt = con.prepareStatement(query);
        	// Кол-во дней от текущей даты, за которое будем загружать посты.
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
            
         LOG.info(String.format("Список постов для оценки успешно загружен. Кол-во постов: %d.", list.size()));
         return list;
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При загрузке списка постов для оценки возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            closeConnection();
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return null;
    }

    // Метод для получения кол-ва лайков всех постов за период, указанный в _dayToAnalyse.
    public int getLikesCountForPeriod(Public pub)
    {
    	LOG.debug(String.format("Загружаем суммарное кол-во лайков. PublicId: %d", pub.getPublicId()));
    	String query = 
    			"SELECT sum(likes_count) as 'likes'  FROM downloaded_posts WHERE post_datetime >= (NOW() - INTERVAL ? DAY) AND public_id = ?";
    	PreparedStatement stmt = null;
        ResultSet rs = null;
    	 
        try {
        	openConnection();
        	stmt = con.prepareStatement(query);
        	
        	// Кол-во дней от текущей даты, за которое будем загружать посты.
            stmt.setInt(1, _dayToAnalyse);
            stmt.setInt(2, pub.getPublicId());
            rs = stmt.executeQuery();
            
 
            if (rs.next()) {
            	int likes = rs.getInt("likes");
            	LOG.info(String.format("Суммарное кол-во лайков успешно загружено. Кол-во лайков: %d, PublicId: %d.", 
            			likes,
            			pub.getPublicId()));
            	return likes;
            }
            LOG.error(String.format("Не удалось загрузить суммарное кол-во лайков. PublicId: %d.", pub.getPublicId()));
        } 
        catch (Exception e) {
            LOG.error(String.format("При загрузке суммарного кол-ва лайков возникла ошибка: %S", e.getMessage()));
        } 
        finally {
            closeConnection();
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return 0;
    }

    // Метод, осуществляющий сохранение информации об оценках в БД.
    public void setRankToDB(Map<DownloadedPost, Integer> ranks, AbstractRule rule)
    {
    	LOG.debug(String.format("Устанавливаем оценку поста в БД. Правило: %s.", rule.getRuleName()));
    	
    	openConnection();
    	// Проверяем наличие записи об этом правиле.
    	// Если запись есть, то обновляем оценку и время. Если нет, то вставляем новую.
    	for (Map.Entry<DownloadedPost, Integer> entry : ranks.entrySet())
    	{
    		DownloadedPost post = entry.getKey();
    		int rank = entry.getValue();
    		
    		if (this.isRankRecordExist(post, rule))
    		{
    			LOG.info(String.format("Запись об оценке уже есть. Обновляем оценку. PublicId: %d, postId %d, Правило: %s", 
    	    			post.getPublicId(), 
    	    			post.getPostId(),
    	    			rule.getRuleName()));
    			this.updateRuleRank(post, rule, rank);
    		}
    		else
    		{
    			LOG.info(String.format("Записи об оценке нет. Добавляем. PublicId: %d, postId %d, Правило: %s", 
    	    			post.getPublicId(), 
    	    			post.getPostId(),
    	    			rule.getRuleName()));
    			this.insertNewRuleRecord(post, rule, rank);
    		}		
    	}
    	LOG.debug(String.format("Устанавка оценок постов в БД завершена. Правило: %s", rule.getRuleName()));
    	closeConnection();    	
    }

    // Метод, осуществляющий проверку наличия записи об оценке переданного поста, согласно переданному правилу.
    private boolean isRankRecordExist(DownloadedPost post, AbstractRule rule)
    {
    	LOG.debug(String.format("Проверяем, есть ли запись об оценке поста. PublicId: %d, postId %d, Правило: %s", 
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
            // Если в ответе будет 1, значит такая запись есть.
            if (rs.next()) {
            	
            	if (rs.getInt("value") == 1) 
            		{
            		LOG.debug(String.format("Такая запись уже есть. PublicId: %d, postId %d, Правило: %s", 
                			post.getPublicId(), 
                			post.getPostId(),
                			rule.getRuleName()));
            		return true;
            		}
            }
            else
            {
            	LOG.debug(String.format("Записи о такой оценки ещё нет. PublicId: %d, postId %d, Правило: %s", 
            			post.getPublicId(), 
            			post.getPostId()),
            			rule.getRuleName());
            	return false;
            }
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При проверке наличия записи об оценке в БД возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return false;
    }

    // Метод, обновляющий информацию об оценке данного поста, согласно переданному правилу.
    private void updateRuleRank(DownloadedPost post, AbstractRule rule, int rank)
    {
    	LOG.debug(String.format("Обновляем оценку. PublicId: %d, postId %d, Правило: %s", 
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
            LOG.debug(String.format("Информация об оценке была успешо обновлена. PublicId: %d, postId %d, Правило: %s", 
    			post.getPublicId(), 
    			post.getPostId(),
    			rule.getRuleName()));
           
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При обновлении информации о посте возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
    }

    // Метод, осуществляющий вставку новой записи об оценке поста, согласно переданному правилу.
    private void insertNewRuleRecord (DownloadedPost post, AbstractRule rule, int rank)
    {
    	LOG.debug(String.format("Вставляем новую запись об оценке в нашу БД. PublicId: %d, postId %d, Правило: %s", 
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
            LOG.debug(String.format("Новая запись об оценке успешно была добавлена в нашу БД. PublicId: %d, postId %d, Правило: %s", 
    	    			post.getPublicId(), 
    	    			post.getPostId(),
    	    			rule.getRuleName()));
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При добавлении новой запись об оценке в БД возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
    }

    // Метод, возвращающий сумму оценок для поста по всем правилам, кроме SUMMARY.
    public int getRulesRankSumForPost (DownloadedPost post)
    {
    	LOG.debug(String.format("Начинаем загрузку суммы оценок для поста. PublicId: %d, PostId: %d",
    			post.getPostId(),
    			post.getPostId()));
    	
    	// Формируем запрос.
    	String query = 
    			"SELECT SUM(rank) as 'sum' FROM rank_processing WHERE downloaded_post_id = ? AND downloaded_public_id = ? "
    			+ "AND rule_name != 'SUMMARY'";
    	
    	PreparedStatement stmt = null;
        ResultSet rs = null;
    	 
        try {
        	openConnection();
        	stmt = con.prepareStatement(query);
        	// Кол-во дней от текущей даты, за которое будем загружать посты.
            stmt.setInt(1, post.getPostId());
            stmt.setInt(2, post.getPublicId());
            rs = stmt.executeQuery();
            int rankSum = 0;
 
            if (rs.next()) {
            	rankSum = rs.getInt("sum");
            }
            
         LOG.debug(String.format("Cумма оценок для поста успешно загружена. PublicId: %d, PostId: %d, Сумма: %d",
        			post.getPostId(),
        			post.getPostId(),
        			rankSum));
         return rankSum;
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При загрузке суммы оценок для поста возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            closeConnection();
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return 0;
    }
}