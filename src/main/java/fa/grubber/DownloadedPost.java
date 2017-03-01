package fa.grubber;

public class DownloadedPost {
	// ������������� �������.
	private int _publicId;
	
	// ������������� ����� � �������.
	private int _postId;
	
	// ����� �����.
	private String _text;
	
	// ���-�� ������ �����.
	private int _likesCount;
	
	// ���-�� �������� �����.
	private int _repostsCount;

	
	// ������/������ PublicId
	public int getPublicId ()
	{
		return this._publicId;
	}
	
	public void setPublicId (int publicId)
	{
		this._publicId = publicId;
	}
	
	// ������/������ PostId
	public int getPostId ()
	{
		return this._postId;
	}
		
	public void setPostId (int postId)
	{
		this._postId = postId;
	}

	// ������/������ Text
	public String getText ()
	{
		return this._text;
	}
		
	public void setText (String text)
	{
		this._text = text;
	}
	
	// ������/������ LikesCount
	public int getLikesCount ()
	{
		return this._likesCount;
	}
		
	public void setLikesCount (int likesCount)
	{
		this._likesCount = likesCount;
	}

	// ������/������ RepostsCount
	public int getRepostsCount ()
	{
		return this._repostsCount;
	}
		
	public void setRepostsCount (int repostsCount)
	{
		this._repostsCount = repostsCount;
	}
	
	public DownloadedPost(int publicId, int postId, String text, int likesCount, int repostsCount)
	{
		this._publicId = publicId;
		this._postId = postId;
		this._text = text;
		this._likesCount = likesCount;
		this._repostsCount = repostsCount;
	}
}
