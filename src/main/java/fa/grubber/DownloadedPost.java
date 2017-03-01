package fa.grubber;

public class DownloadedPost {
	// Идентификатор паблика.
	private int _publicId;
	
	// Идентификатор поста в паблике.
	private int _postId;
	
	// Текст поста.
	private String _text;
	
	// Кол-во лайков поста.
	private int _likesCount;
	
	// Кол-во репостов поста.
	private int _repostsCount;

	
	// Геттер/сеттер PublicId
	public int getPublicId ()
	{
		return this._publicId;
	}
	
	public void setPublicId (int publicId)
	{
		this._publicId = publicId;
	}
	
	// Геттер/сеттер PostId
	public int getPostId ()
	{
		return this._postId;
	}
		
	public void setPostId (int postId)
	{
		this._postId = postId;
	}

	// Геттер/сеттер Text
	public String getText ()
	{
		return this._text;
	}
		
	public void setText (String text)
	{
		this._text = text;
	}
	
	// Геттер/сеттер LikesCount
	public int getLikesCount ()
	{
		return this._likesCount;
	}
		
	public void setLikesCount (int likesCount)
	{
		this._likesCount = likesCount;
	}

	// Геттер/сеттер RepostsCount
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
