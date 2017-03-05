package fa.grubber;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vk.api.sdk.objects.wall.WallpostFull;

public class Controller {

	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);
	
	public static void main(String[] args) {
		LOG.info("=== START ===");
		DBHelper db = new DBHelper();
		
		// Получаем список пабликов.
		ArrayList<Public> publics = db.getPublics();
		
		List<WallpostFull> wallPosts = null;
		
		// Для каждого паблика получаем список постов со стены и записываем их в БД.
		for (Public pub : publics) {
			wallPosts = Requester.getWallPostsFromPublic(pub);
			db.WriteDownloadedPosts(wallPosts);
			wallPosts.clear();
		}
		
		LOG.info("=== END ===");	
	}
}