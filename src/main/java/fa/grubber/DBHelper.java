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
    private static final String url = "jdbc:mysql://localhost:3306/agregator_db";
    private static final String user = "johny";
    private static final String password = "johny";
    private static final Logger LOG = LoggerFactory.getLogger(DBHelper.class);
 
    // JDBC variables for opening and managing connection
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
    
    // Метод, возвращающий коллекцию пабликов.
    public ArrayList<Public> getPublics()
    {
    	LOG.debug("Начинаем загрузку списка пабликов");
    	String query = "select id, name, url from publics";
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
            	
            	list.add(pub);	
            }
         LOG.info("Список пабликов успешно загружен");
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При загрузке списка пабликов возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            closeConnection();
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
        return list;
    }

    // Метод, записывающий посты в БД.
    public void WriteDownloadedPosts (List<WallpostFull> posts)
    {
    	if (posts.isEmpty()) throw new IllegalStateException("Список постов для записи пуст.");
    	
    	// "Конвертируем WallpostFull в наш DownloadedPost.
    	ArrayList<DownloadedPost> downloadedPosts = new ArrayList<DownloadedPost>();
    	for (WallpostFull post : posts)
    	{
    		DownloadedPost dpost = new DownloadedPost(post.getOwnerId() * -1, 
    				post.getId(), 
    				post.getText(), 
    				post.getLikes().getCount(), 
    				post.getReposts().getCount());
    		downloadedPosts.add(dpost);
    	}
    	
    	openConnection();	
    	// Проверяем наличие каждого поста в нашей БД.
    	for (DownloadedPost dpost : downloadedPosts)
    	{
    		if (isPostExist(dpost))
    		{
    			LOG.info(String.format("Пост уже есть в нашей БД.. PublicId: %d, postId %d", 
    					dpost.getPublicId(), 
    					dpost.getPostId()));
    			if (!isLikeCountActual(dpost))
    			{
    				LOG.info(String.format("Информация в БД о посте неактуальна. Обновляем информацию. PublicId: %d, postId %d", 
        					dpost.getPublicId(), 
        					dpost.getPostId()));
    				updateDBPostInfo(dpost);
    			}
    			else
    			{
    				LOG.info(String.format("Информация в БД о посте актуальна. PublicId: %d, postId %d", 
        					dpost.getPublicId(), 
        					dpost.getPostId()));
    			}
    		}
    		else
    		{
    			LOG.info(String.format("Новый пост. Записываем в нашу БД. PublicId: %d, postId %d", 
    					dpost.getPublicId(), 
    					dpost.getPostId()));
    			insertNewPost(dpost);
    		}
    	}	
    	closeConnection();
    }
    
    // Метод, осуществляющий проверку, есть ли такой пост в нашей БД.
    private boolean isPostExist(DownloadedPost post)
    {
    	LOG.debug(String.format("Проверяем, есть ли пост в нашей БД. PublicId: %d, postId %d", 
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
            // Если в ответе будет 1, значит такой пост есть.
            if (rs.next()) {
            	
            	if (rs.getInt("value") == 1) 
            		{
            		LOG.debug(String.format("Такой пост есть в нашей БД. PublicId: %d, postId %d",
                			post.getPublicId(), 
                			post.getPostId()));
            		return true;
            		}
            }
            else
            {
            	LOG.debug(String.format("Такого поста нет в нашей БД. PublicId: %d, postId %d",
            			post.getPublicId(), 
            			post.getPostId()));
            	return false;
            }
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При проверке наличия поста в БД возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return false;
    }

    // Метод, обновляющий информацию о существующем посте в нашей БД.
    private void updateDBPostInfo(DownloadedPost post)
    {
    	LOG.debug(String.format("Обновляем информацю о посте в нашей БД. PublicId: %d, postId %d", 
    			post.getPublicId(), 
    			post.getPostId()));

    	// Получаем информацию о кол-ве лайков ДО обновления.
    	int oldLikesCount = getPostLikesCountFromDB(post);
    	
    	String query = "update downloaded_posts set likes_count = ?, reposts_count = ? where public_id = ? and post_id = ?";
    	PreparedStatement stmt = null;
    	
    	try {
        	stmt = con.prepareStatement(query);
        	
        	stmt.setInt(1, post.getLikesCount());
        	stmt.setInt(2, post.getRepostsCount());
        	stmt.setInt(3, post.getPublicId());
        	stmt.setInt(4, post.getPostId());
        	
            stmt.executeUpdate();
            LOG.debug(String.format("Информация о посте была успешо обновлена. PublicId: %d, postId %d", 
        			post.getPublicId(), 
        			post.getPostId()));
            
            LOG.debug(String.format("Было лайков - %d, стало лайков - %d. PublicId: %d, postId %d", 
            		oldLikesCount,
            		post.getLikesCount(),
        			post.getPublicId(), 
        			post.getPostId()));
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При обновлении информации о посте возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
    }
    
    // Метод, возвращающий кол-во лайков для переданного поста, которое записано в БД.
    private int getPostLikesCountFromDB (DownloadedPost post)
    {
    	LOG.debug(String.format("Обновляем информацю о кол-ве лайков поста из нашей БД. postId %d",  
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
            LOG.debug(String.format("Информация о кол-ве лайков была успешо обновлена. postId %d",
        			post.getPostId()));
            return 0;
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При обновлении информации о кол-ве лайков в посте возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return 0;	
    }

    // Метод, осуществляющий запись нового поста, которого нет в нашей БД.
    private void insertNewPost(DownloadedPost post)
    {
    	LOG.debug(String.format("Вставляем новый пост в нашу БД. PublicId: %d, postId %d", 
    			post.getPublicId(), 
    			post.getPostId()));
 
    	
    	String query = "INSERT INTO downloaded_posts(public_id, post_id, text, likes_count, reposts_count) VALUES (?, ?, ?, ?, ?)";
    	PreparedStatement stmt = null;
    	
    	try {
        	stmt = con.prepareStatement(query);
        	
        	stmt.setInt(1, post.getPublicId());
        	stmt.setInt(2, post.getPostId());
        	stmt.setString(3, post.getText());
        	stmt.setInt(4, post.getLikesCount());
        	stmt.setInt(5, post.getRepostsCount());
        	
            stmt.executeUpdate();
            LOG.debug(String.format("Новый пост успешно был добавлен в нашу БД. PublicId: %d, postId %d", 
        			post.getPublicId(), 
        			post.getPostId())); 
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При добавлении нового поста возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
    }

    // Метод, проверяющий, актуально ли число лайков в БД по сравнению с переданным постом.
    private boolean isLikeCountActual (DownloadedPost post)
    {
    	LOG.debug(String.format("Проверяем, актуально ли число лайков поста в БД. PublicId: %d, postId %d", 
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
            		LOG.debug(String.format("Кол-во лайков поста в БД не актуально. PublicId: %d, postId %d",
                			post.getPublicId(), 
                			post.getPostId()));
            		return false;
            	}
            	else
            	{
            		LOG.debug(String.format("Кол-во лайков поста в БД актуально. PublicId: %d, postId %d",
                			post.getPublicId(), 
                			post.getPostId()));
            		return true;
            	}
            }
            else
            {
            	LOG.debug(String.format("При проверке актуальности числа лайков не удалось найти такой пост. PublicId: %d, postId %d",
            			post.getPublicId(), 
            			post.getPostId()));
            	return false;
            }
        } 
        catch (SQLException sqlEx) {
            LOG.error(String.format("При проверке наличия поста в БД возникла ошибка: %S", sqlEx.getMessage()));
        } 
        finally {
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
		return false;
    	
    }
}