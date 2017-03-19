package fa.rankprocessing;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fa.common.DownloadedPost;
import fa.common.Public;

public class AudienceInvolvementRule extends AbstractRule {

	private static final Logger LOG = LoggerFactory.getLogger(AudienceInvolvementRule.class);
	private final String _ruleName = "Вовлеченность_аудитории";
	private List <DownloadedPost> _posts;
	private final Public _pub;
	
	// Метод запуска оценки (IRankable).
	public void executeRanking()
	{
		LOG.info(String.format("Начинаем обработку постов . Правило: %s", this.getRuleName()));
		RankProcessingDBHelper rpdb = new RankProcessingDBHelper();
		// Массив "Пост - оценка".
		Map<DownloadedPost, Integer> ratings = new Hashtable<DownloadedPost, Integer>();
		
		try {				
			// Для каждого поста вычисляем отношение числа лайков и репостов к общему числу подписчиков паблика.
			for (DownloadedPost post : _posts)
			{
				float value = (((float)post.getLikesCount() + post.getRepostsCount())/_pub.getPublicSubsCount());
				value = value * 100;

				int rank = this.rankPostByInvolvement(value);
				LOG.info(String.format("Пост: %d, паблик: %d, value: %s, оценка: %s",
						post.getPostId(),
						post.getPublicId(),
						String.valueOf(value),
						String.valueOf(rank)));
				ratings.put(post, rank);
			}
			rpdb.setRankToDB(ratings, this);	
		}
		catch (Exception e) {
			LOG.error(String.format("При обработке правила возникла ошибка. Правило: %S, ошибка %S",
					this.getRuleName(),
					e.getMessage()));
			e.printStackTrace();
		}		
			
	}
		
	// Метод, возвращающий имя правила.
	public String getRuleName()
	{
		return _ruleName;
	}
	
	// Дефолтный конструктор.
	public AudienceInvolvementRule (List<DownloadedPost> posts, Public pub)
	{
		if (!posts.isEmpty() && posts != null) 
		{
			this._posts = posts;
		}
		else
		{
			throw new IllegalStateException("Список постов пуст.");
		}
		
		if (pub != null) 
		{
			this._pub = pub;
		}
		else
		{
			throw new IllegalStateException("Передан пустой паблик.");
		}
	}
	
	// Метод непосредственного расчёта оценки.
	private int rankPostByInvolvement(float value)
	{
		int rank = 1;

		if (value >= 1.9) rank = 10;
		if (value >= 1.8 && value < 1.9) rank = 9;
		if (value >= 1.7 && value < 1.8) rank = 8;
		if (value >= 1.5 && value < 1.7) rank = 7;
		if (value >= 1.3 && value < 1.5) rank = 6;
		if (value >= 1.1 && value < 1.3) rank = 5;
		if (value >= 0.9 && value < 1.1) rank = 4;
		if (value >= 0.7 && value < 0.9) rank = 3;
		if (value >= 0.4 && value < 0.7) rank = 2;
		return rank;
	}

}
