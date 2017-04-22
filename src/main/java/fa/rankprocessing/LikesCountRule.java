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
	private final String _ruleName = "����������_���-��_������";
	private final Public _pub;
	
	// ����� ������� ������ (IRankable).
	public void executeRanking()
	{
		LOG.info(String.format("�������� ��������� ������ . �������: %s", this.getRuleName()));
		RankProcessingDBHelper_old rpdb = new RankProcessingDBHelper_old();
		// ������ "���� - ������".
		Map<DownloadedPost, Integer> ratings = new Hashtable<DownloadedPost, Integer>();
		
		// ������ "�������� ���������� - ����" (��������������� TreeMap).
		Map<Float, DownloadedPost> values = new TreeMap<Float, DownloadedPost>(Collections.reverseOrder());
		
		try {	
			// �������� ���-�� ������ ��� ��������������� ������ ��� �������.
			int likes = rpdb.getLikesCountForPeriod(_pub);
			
			// ��� ������� ����� ��������� ��������� ����� ������ � ������ ����� ������ �� ������.
			for (DownloadedPost post : _posts)
			{
				float value = (float) post.getLikesCount()/likes;
				LOG.info(String.format("����: %d, ������: %d, value: %s",
						post.getPostId(),
						post.getPublicId(),
						String.valueOf(value)));
				values.put(value, post);
			}
			
			// ���������� ������.
			ratings = this.rankPostsByLikes(values);
			
			// ���������� � ��.
			rpdb.setRankToDB(ratings, this);	
		}
		catch (Exception e) {
			LOG.error(String.format("��� ��������� ������� �������� ������. �������: %S, ������ %S",
					this.getRuleName(),
					e.getMessage()));
			e.printStackTrace();
		}		
	}
	
	// �����, ������������ ��� �������.
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
			throw new IllegalStateException("������ ������ ����.");
		}
		
		if (pub != null) 
		{
			this._pub = pub;
		}
		else
		{
			throw new IllegalStateException("������� ������ ������.");
		}
	}

	// �����, �������������� ������ ������ ����� �� ����������� 
	// �������� ��������� ���-�� ������ ����� � ������ 
	// ����� ������ �� ������.
	private Map<DownloadedPost, Integer> rankPostsByLikes(Map<Float, DownloadedPost> values)
	{
		Map<DownloadedPost, Integer> result = new Hashtable<DownloadedPost, Integer>();
		// ������ 9 ������ ��� ���������������� ������ 10..2, � ���� ��������� ������ 1.
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