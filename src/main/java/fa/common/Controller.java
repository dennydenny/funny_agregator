package fa.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.wall.WallpostFull;

import fa.grubber.DBHelper;
import fa.grubber.Requester;

public class Controller {

	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);
	
	public static void main(String[] args) throws InterruptedException, ApiException, ClientException  {
		LOG.info("=== START ===");
		
		runGrubber();
		
		LOG.info("=== END ===");	
	}
	
	// ������ ��������.
	private static void runGrubber() throws InterruptedException, ApiException, ClientException
	{
		DBHelper db = new DBHelper();
		
		// �������� ������ ��������.
		ArrayList<Public> publics = db.getPublics();
		
		List<WallpostFull> wallPosts = null;
		
		for (Public pub : publics) {

			// �������� ������ ������ �� �����.
			wallPosts = Requester.getWallPostsFromPublic(pub);
			
			// ���������� ����� � ��.
			db.WriteDownloadedPosts(wallPosts);
			TimeUnit.SECONDS.sleep(1);

			// ������� ����� ����������� � �� � � VK. ���� �� ���������, �� ��������� � ��.
			int subsCount = Requester.getPublicSubsCount(pub);
			
			if (subsCount !=0 && subsCount != pub.getPublicSubsCount())
			{
				db.updatePublicSubsCount(pub, subsCount);
			}
			
			TimeUnit.SECONDS.sleep(5);
			wallPosts.clear();
		}
	}
}