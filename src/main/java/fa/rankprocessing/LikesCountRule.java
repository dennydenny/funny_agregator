package fa.rankprocessing;

import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
		RankProcessingDBHelper_old rpdb = new RankProcessingDBHelper_old();
		// Массив "Пост - оценка".
		Map<DownloadedPost, Integer> ratings = new Hashtable<DownloadedPost, Integer>();
		
		// Массив "Значение вычисления - Пост" (отсортированный TreeMap).
		Map<Float, DownloadedPost> values = new TreeMap<Float, DownloadedPost>(Collections.reverseOrder());
		
		try {	
			// Получаем кол-во лайков для рассматриваемый период для паблика.
			int likes = rpdb.getLikesCountForPeriod(_pub);
			
			// Для каждого поста вычисляем отношение числа лайков к общему числу лайков за период.
			for (DownloadedPost post : _posts)
			{
				float value = (float) post.getLikesCount()/likes;
				LOG.info(String.format("Пост: %d, паблик: %d, value: %s",
						post.getPostId(),
						post.getPublicId(),
						String.valueOf(value)));
				values.put(value, post);
			}
			
			// Производим оценку.
			ratings = this.rankPostsByLikes(values);
			
			// Записываем в БД.
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

	// Метод, осуществляющий оценку списка поста по переданному 
	// значению отношения кол-во лайков поста к общему 
	// числу лайков за период.
	private Map<DownloadedPost, Integer> rankPostsByLikes(Map<Float, DownloadedPost> values)
	{
		Map<DownloadedPost, Integer> result = new Hashtable<DownloadedPost, Integer>();
		// Первым 9 постам даём последовательную оценку 10..2, а всем остальным оценка 1.
		int rank = 10;
		for (Map.Entry<Float, DownloadedPost> entry : values.entrySet())
		{
			if (rank > 1)
			{
				result.put(entry.getValue(), rank);
				rank--;
			}
			else
			{
				result.put(entry.getValue(), 1);		
			}
		}
				
		return result;
	}
}