package fa.rankprocessing;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fa.common.DownloadedPost;
import fa.common.Public;

public class LikesToRepostsRule extends AbstractRule {
	
	private List <DownloadedPost> _posts;
	private static final Logger LOG = LoggerFactory.getLogger(LikesToRepostsRule.class);
	private final String _ruleName = "Отношение_лайков_к_репостам";
	private final Public _pub;

	@Override
	public void executeRanking() {
		LOG.info(String.format("Начинаем обработку постов . Правило: %s", this.getRuleName()));
		RankProcessingDBHelper rpdb = new RankProcessingDBHelper();
		
		// Массив "Пост - оценка".
		Map<DownloadedPost, Integer> ratings = new Hashtable<DownloadedPost, Integer>();
		
		try {			
			// Для каждого поста вычисляем отношение числа репостов к числу лайков.
			for (DownloadedPost post : _posts)
			{
				float value = (float) post.getRepostsCount()/post.getLikesCount();
				int rank = this.rankPostByR2L(value);
				
				System.out.println(String.format("Пост: %d, паблик: %d, отношение: %s, оценка: %s",
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

	@Override
	public String getRuleName() {
		return _ruleName;
	}

	public LikesToRepostsRule (List<DownloadedPost> posts, Public pub)
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

	private int rankPostByR2L(float value)
	{
		int rank = 0;

		if (value >= 0.9) rank = 10;
		if (value >= 0.85 && value < 0.9) rank = 9;
		if (value >= 0.8 && value < 0.85) rank = 8;
		if (value >= 0.75 && value < 0.8) rank = 7;
		if (value >= 0.7 && value < 0.75) rank = 6;
		if (value >= 0.65 && value < 0.7) rank = 5;
		if (value >= 0.6 && value < 0.65) rank = 4;
		if (value >= 0.5 && value < 0.6) rank = 3;
		if (value >= 0.4 && value < 0.5) rank = 2;
		if (value >= 0.3 && value < 0.4) rank = 1;
		
		return rank;
	}

}
