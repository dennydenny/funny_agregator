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
import fa.rankprocessing.AbstractRule;
import fa.rankprocessing.LikesCountRule;
import fa.rankprocessing.LikesToRepostsRule;
import fa.rankprocessing.RankProcessingDBHelper;

public class Controller {

	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);
	private static DBHelper db = new DBHelper();
	private static int timeout = Integer.valueOf(Settings.settings.get("vk_api_request_timeout"));
	
	public static void main(String[] args) throws InterruptedException, ApiException, ClientException  {
		LOG.info("=== START ===");
		
		if (Integer.valueOf(Settings.settings.get("is_grubber_enabled")) == 1) runGrubber();
		if (Integer.valueOf(Settings.settings.get("is_rankprocessing_enabled")) == 1) runRankProcessing();
		
		LOG.info("=== END ===");	
	}
	
	// ������ ��������.
	private static void runGrubber() throws InterruptedException, ApiException, ClientException
	{		
		// �������� ������ ��������.
		ArrayList<Public> publics = db.getPublics();
		
		List<WallpostFull> wallPosts = null;
		
		for (Public pub : publics) 
		{
			// �������� ������ ������ �� �����.
			wallPosts = Requester.getWallPostsFromPublic(pub);
			
			// ���������� ����� � ��.
			db.WriteDownloadedPosts(wallPosts);

			// ������� ����� ����������� � �� � � VK. ���� �� ���������, �� ��������� � ��.
			int subsCount = Requester.getPublicSubsCount(pub);
			
			if (subsCount !=0 && subsCount != pub.getPublicSubsCount())
			{
				db.updatePublicSubsCount(pub, subsCount);
			}
			
			TimeUnit.SECONDS.sleep(timeout);
			wallPosts.clear();
		}
	}

	// ������ ������ ������.
	private static void runRankProcessing()
	{
		RankProcessingDBHelper rpdb = new RankProcessingDBHelper();
		
		// ������ ��������.
		ArrayList<Public> publics = db.getPublics();
		List<DownloadedPost> posts;
		
		// ������ ������ ��� ���������.
		ArrayList<AbstractRule> rules = new ArrayList<AbstractRule>();
		
		for (Public pub : publics)
		{
			posts = rpdb.getPostsForRanking(pub);
			
			// ������� ����������� ���-�� ������.
			LikesCountRule likesCountRule = new LikesCountRule(posts, pub);
			rules.add(likesCountRule);
			
			// ������� ��������� ������ � ��������.
			LikesToRepostsRule likesToRepostsRule = new LikesToRepostsRule(posts, pub);
			rules.add(likesToRepostsRule);
			
			for (AbstractRule rule : rules)
			{
				rule.executeRanking();
			}
			posts.clear();			
		}
	}
}