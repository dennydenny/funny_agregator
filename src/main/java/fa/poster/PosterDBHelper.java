package fa.poster;

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

import fa.common.DownloadedPost;
import fa.common.Public;
import fa.common.Settings;

public class PosterDBHelper {
	
    private static final String url = Settings.settings.get("database_url");
    private static final String user = Settings.settings.get("database_user");
    private static final String password = Settings.settings.get("database_password");
    private static final Logger LOG = LoggerFactory.getLogger(PosterDBHelper.class);
    // Кол-во дней от текущего времени, за которые проводится оценка.
    private static final int _dayToAnalyse = Integer.valueOf(Settings.settings.get("day_to_analyse"));
    // Лимит на кол-во репостов одной записи на стену.
    private static final int _repostsLimit = Integer.valueOf(Settings.settings.get("reposts_count_limit"));
    // Минимальное расстояние между двумя репостами.
    private static final int _hoursAfterLastRepost = Integer.valueOf(Settings.settings.get("hours_after_last_repost"));
 
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

    // Метод, возвращающий список постов, подходящих под условия постинга.
    /*
     * Условия:
     * 1. Пост младше или равен кол-ву дней из настроек.
     * 		И
     * 2. Для поста существует запись с суммарной оценкой.
     * 		И
     * 3. Пост нами ранее не репостился
     * 		ИЛИ
     * 4. Пост нами ранее репостился, но репост был сделан более X дней назад.
     * 		И
     * 5. Кол-во репостов нами сделано не более Y раз.
     */
    public List<DownloadedPost> getPostsForPosting ()
    {
    	LOG.debug("Начинаем загрузку списка постов для постинга");
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
        	// Кол-во дней от текущей даты, за которое будем загружать посты.
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
            
         LOG.info(String.format("Список постов для постинга успешно загружен. Кол-во постов: %d.", list.size()));
         return list;
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При загрузке списка постов для постинга возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            closeConnection();
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return null;
    }

    // Метод, записывающий информацию о репосте.
    public void insertRepostInfo (DownloadedPost post)
    {
    	LOG.debug(String.format("Записываем информацию о репосте в БД. PostId: %d, PublicId: %d", 
    			post.getPostId(),
    			post.getPublicId()));
    	openConnection();
    	
    	if (this.isReposted(post))
    	{
    		LOG.info(String.format("Информация об этом репосте уже есть в нашей БД. Обновляем счётчик. PostId: %d, PublicId: %d.", 
        			post.getPostId(),
        			post.getPublicId()));
    		this.updatePostRepostsCount(post);
    	}
    	else
    	{
    		LOG.info(String.format("Новая запись о репосте. Вставляем в БД. PostId: %d, PublicId: %d.", 
        			post.getPostId(),
        			post.getPublicId()));
    		this.insetNewRepostInfo(post);    		
    	}	
    	closeConnection();
    }
    
    // Метод, проверяющий, есть ли запись о репосте в БД.
    private boolean isReposted (DownloadedPost post)
    {
    	LOG.debug(String.format("Проверяем, есть ли запись о репосте поста. PublicId: %d, postId %d.", 
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
            // Если в ответе будет 1, значит такая запись есть.
            if (rs.next()) {
            	
            	if (rs.getInt("value") == 1) 
            		{
            		LOG.debug(String.format("Запись о репосте уже есть. PublicId: %d, postId %d.", 
                			post.getPublicId(), 
                			post.getPostId()));
            		return true;
            		}
            }
            else
            {
            	LOG.debug(String.format("Записи о репосте ещё нет. PublicId: %d, postId %d.", 
            			post.getPublicId(), 
            			post.getPostId()));
            	return false;
            }
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При проверке наличия записи о репосте в БД возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return false;
    }

    // Метод, увеличивающий счётчик числа репостов на стену нашего паблика.
    private void updatePostRepostsCount (DownloadedPost post)
    {
    	LOG.debug(String.format("Обновляем счётчик репостов. PublicId: %d, postId %d", 
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
            LOG.debug(String.format("Счётчик репостов был успешно обновлён. PublicId: %d, postId %d,", 
    			post.getPublicId(), 
    			post.getPostId()));
           
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При обновлении счётчика репостов возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }  	
    }

    // Метод, возвращающий текущее число публикации записи в нашем паблике
    private int getCurrentRepostsCount (DownloadedPost post)
    {
    	LOG.debug(String.format("Получаем текущее число публикаций поста в нашем паблике. PublicId: %d, PostId: %d",
    			post.getPostId(),
    			post.getPostId()));
    	
    	// Формируем запрос.
    	String query = 
    			"SELECT rp.count as 'count' FROM reposted_posts rp WHERE rp.downloaded_post_id = ? AND rp.downloaded_public_id = ?";
    	
    	PreparedStatement stmt = null;
        ResultSet rs = null;
    	 
        try {
        	stmt = con.prepareStatement(query);
        	// Кол-во дней от текущей даты, за которое будем загружать посты.
            stmt.setInt(1, post.getPostId());
            stmt.setInt(2, post.getPublicId());
            rs = stmt.executeQuery();
            int counter = 0;
 
            if (rs.next()) {
            	counter = rs.getInt("count");
            }
            else
            {
            	LOG.warn(String.format("Не удалось получить текущее число публикаций поста в нашем паблике. PublicId: %d, PostId: %d",
            			post.getPostId(),
            			post.getPostId()));
            }
            
         LOG.debug(String.format("Текущее числу публикаций поста в нашем паблике успешно загружено. PublicId: %d, PostId: %d, Число: %d",
        			post.getPostId(),
        			post.getPostId(),
        			counter));
         return counter;
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При загрузке числа публикаций поста в нашем паблике возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return 0;
    }

    // Метод, осуществляющий запись о новом посте, опубликованном в нашем паблике.
    private void insetNewRepostInfo(DownloadedPost post)
    {
    	LOG.debug(String.format("Вставляем новую запись о репосте в нашу БД. PublicId: %d, postId %d", 
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
		    LOG.debug(String.format("Новая запись о репосте успешно была добавлена в нашу БД. PublicId: %d, postId %d", 
		    			post.getPublicId(), 
		    			post.getPostId()));
		} 
		catch (SQLException sqlEx) {
		    LOG.error(String.format("При добавлении новой запись о репосте в БД возникла ошибка: %S", sqlEx.getMessage()));
		} 
		finally {
		    try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
		}
    }

    // Метод, возвращающий инфо о том, можно ли сейчас делать репост или нет (смотрит на настройку hours_after_last_repost)
    public boolean isRepostAllowNow()
    {
    	LOG.debug("Проверяем, можно ли сейчас репостить.");
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
            	LOG.debug("Репост запрещён.");
            	return false;
            }
            
            LOG.debug("Репост разрешён."); 
            return true;
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При загрузке проверке разрешения на репост возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            closeConnection();
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
        return true;
    }
}
