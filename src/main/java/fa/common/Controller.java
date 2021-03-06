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
import fa.grubber.wallPosts2DPostsConverter;
import fa.poster.PosterDBHelper;
import fa.rankprocessing.AbstractRule;
import fa.rankprocessing.AudienceInvolvementRule;
import fa.rankprocessing.RankProcessingDBHelper;


public class Controller {

	private static final String _version = "1.2";
	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);
	private static DBHelper db = new DBHelper();
	
	public static void main(String[] args) throws InterruptedException, ApiException, ClientException  {
		LOG.info("=== Funny START === version " + _version);
				
		if (Integer.valueOf(Settings.settings.get("is_grubber_enabled")) == 1) runGrubber();
		if (Integer.valueOf(Settings.settings.get("is_rankprocessing_enabled")) == 1) runRankProcessing();
		if (Integer.valueOf(Settings.settings.get("is_poster_enabled")) == 1) runPoster();
		
		LOG.info("=== Funny END ===");	
	}
	
	// ������ ��������.
	private static void runGrubber() throws InterruptedException, ApiException, ClientException
	{	
		long startTime = System.currentTimeMillis();

		int timeout = Integer.valueOf(Settings.settings.get("vk_api_request_timeout"));
		// �������� ������ ��������.
		ArrayList<Public> publics = db.getPublics();
		
		List<WallpostFull> wallPosts = null;
		
			for (Public pub : publics) 
			{
				try {
					// �������� ������ ������ �� �����.
					wallPosts = Requester.getWallPostsFromPublic(pub);
					
					// ������������ WallpostFull � ������ ������� DownloadedPost.
					List<DownloadedPost> posts = wallPosts2DPostsConverter.convert(wallPosts);
					        
					// ���������� ����� � ��.
					db.WriteDownloadedPosts(posts);
		
					// ������� ����� ����������� � �� � � VK. ���� �� ���������, �� ��������� � ��.
					int subsCount = Requester.getPublicSubsCount(pub);
					
					if (subsCount !=0 && subsCount != pub.getPublicSubsCount())
					{
						db.updatePublicSubsCount(pub, subsCount);
					}
					
					TimeUnit.SECONDS.sleep(timeout);
					wallPosts.clear();
				}
				catch (Exception e)
				{
					LOG.error(e.getMessage());
					LOG.error(String.valueOf(e.getStackTrace()));
				}
			}
		long endTime = System.currentTimeMillis();
		LOG.info(String.format("����� ���������� grubber %d ms.", (endTime - startTime)));
	}

	// ������ ������ ������.
	private static void runRankProcessing()
	{
		long startTime = System.currentTimeMillis();
		
		RankProcessingDBHelper rpdb = new RankProcessingDBHelper();
		
		// ������ ��������.
		ArrayList<Public> publics = db.getPublics();
		List<DownloadedPost> posts;
		
		// ������ ������ ��� ���������.
		ArrayList<AbstractRule> rules = new ArrayList<AbstractRule>();
		
		for (Public pub : publics)
		{
			try {
				posts = rpdb.getPostsForRanking(pub);
				/*
				// ������� ����������� ���-�� ������.
				LikesCountRule likesCountRule = new LikesCountRule(posts, pub);
				rules.add(likesCountRule);
				
				// ������� ��������� ������ � ��������.
				RepostsToLikesRule likesToRepostsRule = new RepostsToLikesRule(posts);
				rules.add(likesToRepostsRule);
				
				// ������� ������������� ���������.
				AudienceInvolvementRule invRule = new AudienceInvolvementRule(posts, pub);
				rules.add(invRule);
				
				// ������������ (� ����� ��������� �������).
				Summarizer sum = new Summarizer(posts);
				rules.add(sum);
				*/
				AudienceInvolvementRule invRule = new AudienceInvolvementRule(posts, pub);
				rules.add(invRule);
				
				for (AbstractRule rule : rules)
				{
					rule.executeRanking();
				}
				posts.clear();	
			}
			catch (IllegalStateException e)
			{
				LOG.warn(e.getMessage());
				LOG.warn(String.valueOf(e.getStackTrace()));
			}
			catch (Exception e)
			{
				LOG.error(e.getMessage());
				LOG.error(String.valueOf(e.getStackTrace()));
			}
		}
		long endTime = System.currentTimeMillis();
		LOG.info(String.format("����� ���������� rankProcessing %d ms.", (endTime - startTime)));
	}

	// ������ �������.
	private static void runPoster()
	{
		long startTime = System.currentTimeMillis();
		
		//Requester.repostPostToPublic(null);
		PosterDBHelper pdb = new PosterDBHelper();
		
		if (pdb.isRepostAllowNow())
		{
			LOG.info("������ ��������. ���������� ������.");
			// ����� ������, ������� ����� �����������.
			List<DownloadedPost> posts = pdb.getPostsForPosting();
			
			if (!posts.isEmpty())
			{
				DownloadedPost post = posts.get(0);
				LOG.info(String.format("�������� ������ �� �����. ����: %d, ������: %d",
						post.getPostId(),
						post.getPublicId()));			
				// ���������� � �� ���� � �������.
				pdb.insertRepostInfo(post);
				// ������������ ������ ������ �� ����� �������.
				Requester.repostPostToPublic(post);
				LOG.info("���������� ���������� � ������� � ��.");	
			}
			else
			{
				LOG.info("��� ���������� ������ ��� ��������.");
			}
		}
		else
		{
			LOG.info("������ ��������. Poster ��������� ���� ������.");	
		}
		
		long endTime = System.currentTimeMillis();
		LOG.info(String.format("����� ���������� poster %d ms.", (endTime - startTime)));
	}
}