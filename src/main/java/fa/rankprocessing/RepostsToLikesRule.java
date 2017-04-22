package fa.rankprocessing;

import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fa.common.DownloadedPost;

public class RepostsToLikesRule extends AbstractRule {
	
	private List <DownloadedPost> _posts;
	private static final Logger LOG = LoggerFactory.getLogger(RepostsToLikesRule.class);
	private final String _ruleName = "Отношение_репостов_к_лайкам";

	@Override
	public void executeRanking() {
		LOG.info(String.format("Начинаем обработку постов . Правило: %s", this.getRuleName()));
		RankProcessingDBHelper_old rpdb = new RankProcessingDBHelper_old();
		
		// Массив "Пост - оценка".
		Map<DownloadedPost, Integer> ratings = new Hashtable<DownloadedPost, Integer>();
		
		// Массив "Значение вычисления - Пост" (отсортированный TreeMap).
		Map<Float, DownloadedPost> values = new TreeMap<Float, DownloadedPost>(Collections.reverseOrder());
		
		try {			
			// Для каждого поста вычисляем отношение числа репостов к числу лайков.
			for (DownloadedPost post : _posts)
			{
				float value = (float) post.getRepostsCount()/post.getLikesCount();
				LOG.info(String.format("Пост: %d, паблик: %d, value: %s",
						post.getPostId(),
						post.getPublicId(),
						String.valueOf(value)));
				values.put(value, post);
			}
			// Производим оценку.
			ratings = this.rankPostByR2L(values);
			
			rpdb.setRankToDB(ratings, this);	
		}
		catch (Exception e) {
			LOG.error(String.format("При обработке правила возникла ошибка. Правило: %S, ошибка %S",
					this.getRuleName(),
					e.getMessage()));
			e.printStackTrace();
		}	
	}

	@Override
	public String getRuleName() {
		return _ruleName;
	}

	public RepostsToLikesRule (List<DownloadedPost> posts)
	{
		if (!posts.isEmpty() && posts != null) 
		{
			this._posts = posts;
		}
		else
		{
			throw new IllegalStateException("Список постов пуст.");
		}
	}

	private Map<DownloadedPost, Integer> rankPostByR2L(Map<Float, DownloadedPost> values)
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
