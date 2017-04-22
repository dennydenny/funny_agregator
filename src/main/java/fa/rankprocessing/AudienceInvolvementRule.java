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
	
	// ����� ������� ������.
	public void executeRanking()
	{
		LOG.info(String.format("�������� ��������� ������ . �������: %s", this.getRuleName()));
		RankProcessingDBHelper rpdb = new RankProcessingDBHelper();
		
		
		// ������ "�������� ���������� - ����" (��������������� TreeMap).
		Map<DownloadedPost, Float> values = new Hashtable<DownloadedPost, Float>();
		
		try {				
			// ��� ������� ����� ��������� ��������� ����� ������ � �������� � ������ ����� ����������� �������.
			for (DownloadedPost post : _posts)
			{
				float value = (((float)post.getLikesCount() + post.getRepostsCount())/post.getViewsCount());
				LOG.info(String.format("����: %d, ������: %d, value: %s",
						post.getPostId(),
						post.getPublicId(),
						String.valueOf(value)));
				values.put(post, value);
			}

			rpdb.setRankToDB(values, this);	
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
	
	// ��������� �����������.
	public AudienceInvolvementRule (List<DownloadedPost> posts, Public pub)
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
}