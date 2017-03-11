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
		LOG.info(String.format("�������� ��������� ������ . �������: %s", this.getRuleName()));
		RankProcessingDBHelper rpdb = new RankProcessingDBHelper();
		
		// ������ "���� - ������".
		Map<DownloadedPost, Integer> ratings = new Hashtable<DownloadedPost, Integer>();
		
		try {			
			// ��� ������� ����� ������� �������� ����� ������ �� ���� ��������, ����� SUMMARY � ���������� � ��.
			for (DownloadedPost post : _posts)
			{
				int rank = rpdb.getRulesRankSumForPost(post);
				ratings.put(post, rank);
			}
			rpdb.setRankToDB(ratings, this);	
		}
		catch (Exception e) {
			LOG.error(String.format("��� ��������� ������� �������� ������. �������: %S, ������ %S",
					this.getRuleName(),
					e.getMessage()));
			e.printStackTrace();
		}
	}

	@Override
	public String getRuleName() {
		return _ruleName;
	}

	// ��������� �����������.
	public Summarizer (List<DownloadedPost> posts)
	{		
		if (posts != null && !posts.isEmpty())
		{
			this._posts = posts;
		}
		else
		{
			throw new IllegalStateException("������ ������ ��� ������������ ����.");
		}
	}
}
