package fa.rankprocessing;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fa.common.DownloadedPost;
import fa.common.Public;

public class LikesCountRule extends AbstractRule {
	
	private static final Logger LOG = LoggerFactory.getLogger(LikesCountRule.class);
	private List <DownloadedPost> _posts;
	private final String _ruleName = "Наибольшее_кол-во_лайков";
	private final Public _pub;
	
	// Метод запуска оценки (IRankable).
	public void executeRanking()
	{
		LOG.info(String.format("Начинаем обработку постов . Правило: %s", this.getRuleName()));
		RankProcessingDBHelper rpdb = new RankProcessingDBHelper();
		// Массив "Пост - оценка".
		Map<DownloadedPost, Integer> ratings = new Hashtable<DownloadedPost, Integer>();
		
		try {	
			// Получаем кол-во лайков для рассматриваемый период для паблика.
			int likes = rpdb.getLikesCountForPeriod(_pub);
			
			// Для каждого поста вычисляем отношение числа лайков к общему числу лайков за период, производим оценку и записываем в БД.
			for (DownloadedPost post : _posts)
			{
				float value = (float) post.getLikesCount()/likes;
				value = value * 100;
				int rank = this.rankPostByLikes(value);
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

	public LikesCountRule(List<DownloadedPost> posts, Public pub)
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

	// Метод, осуществляющий оценку поста по переданному 
	// значению отношения кол-во лайков поста к общему 
	// числу лайков за период.
	private int rankPostByLikes(float value)
	{
		int rank = 1;

		if (value >= 5) rank = 10;
		if (value >= 4.5 && value < 5) rank = 9;
		if (value >= 4 && value < 4.5) rank = 8;
		if (value >= 3.5 && value < 4) rank = 7;
		if (value >= 3 && value < 3.5) rank = 6;
		if (value >= 2.5 && value < 3) rank = 5;
		if (value >= 2 && value < 2.5) rank = 4;
		if (value >= 1.5 && value < 2) rank = 3;
		if (value >= 1 && value < 1.5) rank = 2;
		
		return rank;
	}
}