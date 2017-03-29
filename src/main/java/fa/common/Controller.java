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
import fa.poster.PosterDBHelper;
import fa.rankprocessing.AbstractRule;
import fa.rankprocessing.AudienceInvolvementRule;
import fa.rankprocessing.LikesCountRule;
import fa.rankprocessing.LikesToRepostsRule;
import fa.rankprocessing.RankProcessingDBHelper;
import fa.rankprocessing.Summarizer;

public class Controller {

	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);
	private static DBHelper db = new DBHelper();
	
	public static void main(String[] args) throws InterruptedException, ApiException, ClientException  {
		LOG.info("=== START ===");
		
		if (Integer.valueOf(Settings.settings.get("is_grubber_enabled")) == 1) runGrubber();
		if (Integer.valueOf(Settings.settings.get("is_rankprocessing_enabled")) == 1) runRankProcessing();
		if (Integer.valueOf(Settings.settings.get("is_poster_enabled")) == 1) runPoster();
		
		LOG.info("=== END ===");	
	}
	
	// Запуск граббера.
	private static void runGrubber() throws InterruptedException, ApiException, ClientException
	{	
		int timeout = Integer.valueOf(Settings.settings.get("vk_api_request_timeout"));
		// Получаем список пабликов.
		ArrayList<Public> publics = db.getPublics();
		
		List<WallpostFull> wallPosts = null;
		
		for (Public pub : publics) 
		{
			// Получаем список постов со стены.
			wallPosts = Requester.getWallPostsFromPublic(pub);
			
			// Записываем посты в БД.
			db.WriteDownloadedPosts(wallPosts);

			// Сверяем число подписчиков в БД и в VK. Если не совпадают, то обновляем в БД.
			int subsCount = Requester.getPublicSubsCount(pub);
			
			if (subsCount !=0 && subsCount != pub.getPublicSubsCount())
			{
				db.updatePublicSubsCount(pub, subsCount);
			}
			
			TimeUnit.SECONDS.sleep(timeout);
			wallPosts.clear();
		}
	}

	// Запуск оценки постов.
	private static void runRankProcessing()
	{
		RankProcessingDBHelper rpdb = new RankProcessingDBHelper();
		
		// Список пабликов.
		ArrayList<Public> publics = db.getPublics();
		List<DownloadedPost> posts;
		
		// Список правил для обработки.
		ArrayList<AbstractRule> rules = new ArrayList<AbstractRule>();
		
		for (Public pub : publics)
		{
			try {
				posts = rpdb.getPostsForRanking(pub);
				
				// Правило наибольшего кол-ва лайков.
				LikesCountRule likesCountRule = new LikesCountRule(posts, pub);
				rules.add(likesCountRule);
				
				// Правило отношения лайков к репостам.
				LikesToRepostsRule likesToRepostsRule = new LikesToRepostsRule(posts);
				rules.add(likesToRepostsRule);
				
				// Правило вовлеченности аудитории.
				AudienceInvolvementRule invRule = new AudienceInvolvementRule(posts, pub);
				rules.add(invRule);
				
				// Суммирование (в самую последнюю очередь).
				Summarizer sum = new Summarizer(posts);
				rules.add(sum);
							
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
	}

	// Запуск постера.
	private static void runPoster()
	{
		//Requester.repostPostToPublic(null);
		PosterDBHelper pdb = new PosterDBHelper();
		
		if (pdb.isRepostAllowNow())
		{
			LOG.info("Репост разрешён. Продолжаем работу.");
			// Выбор постов, которые можно публиковать.
			List<DownloadedPost> posts = pdb.getPostsForPosting();
			
			if (!posts.isEmpty())
			{
				DownloadedPost post = posts.get(0);
				LOG.info(String.format("Репостим запись на стену. Пост: %d, паблик: %d",
						post.getPostId(),
						post.getPublicId()));			
				// Записываем в БД инфо о репосте.
				pdb.insertRepostInfo(post);
				// Осуществляем репост записи на стену паблика.
				Requester.repostPostToPublic(post);
				LOG.info("Записываем информацию о репосте в БД.");	
			}		
		}
		else
		{
			LOG.info("Репост запрещён. Poster завершает свою работу.");	
		}	
	}
}