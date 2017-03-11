package fa.rankprocessing;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fa.common.DownloadedPost;

public class Summarizer extends AbstractRule {
	
	private static final Logger LOG = LoggerFactory.getLogger(Summarizer.class);
	private final String _ruleName = "SUMMARY";
	private List <DownloadedPost> _posts;

	@Override
	public void executeRanking() {
		LOG.info(String.format("Начинаем обработку постов . Правило: %s", this.getRuleName()));
		RankProcessingDBHelper rpdb = new RankProcessingDBHelper();
		
		// Массив "Пост - оценка".
		Map<DownloadedPost, Integer> ratings = new Hashtable<DownloadedPost, Integer>();
		
		try {			
			// Для каждого поста находим значение суммы оценок по всем правилам, кроме SUMMARY и записываем в БД.
			for (DownloadedPost post : _posts)
			{
				int rank = rpdb.getRulesRankSumForPost(post);
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

	// Дефолтный конструктор.
	public Summarizer (List<DownloadedPost> posts)
	{		
		if (posts != null && !posts.isEmpty())
		{
			this._posts = posts;
		}
		else
		{
			throw new IllegalStateException("Список правил для суммирования пуст.");
		}
	}
}
