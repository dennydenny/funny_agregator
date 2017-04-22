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
	private final String _ruleName = "Audience Involvement";
	private List <DownloadedPost> _posts;
	private final Public _pub;
	
	// Метод запуска оценки.
	public void executeRanking()
	{
		LOG.info(String.format("Начинаем обработку постов . Правило: %s", this.getRuleName()));
		RankProcessingDBHelper rpdb = new RankProcessingDBHelper();
		
		
		// Массив "Значение вычисления - Пост" (отсортированный TreeMap).
		Map<DownloadedPost, Float> values = new Hashtable<DownloadedPost, Float>();
		
		try {				
			// Для каждого поста вычисляем отношение числа лайков и репостов к общему числу подписчиков паблика.
			for (DownloadedPost post : _posts)
			{
				float value = (((float)post.getLikesCount() + post.getRepostsCount())/post.getViewsCount());
				LOG.info(String.format("Пост: %d, паблик: %d, value: %s",
						post.getPostId(),
						post.getPublicId(),
						String.valueOf(value)));
				values.put(post, value);
			}

			rpdb.setRankToDB(values, this);	
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
}