package fa.grubber;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.slf4j.SLF4JLoggingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.wall.WallpostFull;

public class Controller {

	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);
	
	public static void main(String[] args) throws InterruptedException, ApiException, ClientException {
		LOG.info("=== START ===");
		DBHelper db = new DBHelper();
		
		// Получаем список пабликов.
		ArrayList<Public> publics = db.getPublics();
		
		List<WallpostFull> wallPosts = null;
		
		for (Public pub : publics) {

			// Получаем список постов со стены.
			wallPosts = Requester.getWallPostsFromPublic(pub);
			
			// Записываем посты в БД.
			db.WriteDownloadedPosts(wallPosts);
			TimeUnit.SECONDS.sleep(1);

			// Сверяем число подписчиков в БД и в VK. Если не совпадают, то обновляем в БД.
			int subsCount = Requester.getPublicSubsCount(pub);
			
			if (subsCount !=0 && subsCount != pub.getPublicSubsCount())
			{
				db.updatePublicSubsCount(pub, subsCount);
			}
			
			TimeUnit.SECONDS.sleep(5);
			//wallPosts.clear();
		}
		
		LOG.info("=== END ===");	
	}
}