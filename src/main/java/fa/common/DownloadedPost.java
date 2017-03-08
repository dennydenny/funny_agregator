package fa.common;

import java.text.SimpleDateFormat;

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
	
	// Дата и время публикации поста в VK.
	private String _postDatetime;

	
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
	
	// Геттер/сеттер Datetime
	public String getPostDatetime ()
	{
		return this._postDatetime;
	}
			
	public void setPostDatetime (String postDatetime)
	{
		this._postDatetime = postDatetime;
	}
	
	public void setPostDatetime (int postDatetime)
	{
		this._postDatetime = this.convertUnixtimeToDate(postDatetime);
	}
	
	// Конструктор с int postDatetime
	public DownloadedPost(int publicId, int postId, String text, int likesCount, int repostsCount, int postDatetime)
	{
		this.setPublicId(publicId);
		this.setPostId(postId);
		this.setText(text);
		this.setLikesCount(likesCount);
		this.setRepostsCount(repostsCount);
		this.setPostDatetime(postDatetime);
	}
	
	// Конструктор с String postDatetime
	public DownloadedPost(int publicId, int postId, String text, int likesCount, int repostsCount, String postDatetime)
	{
		this.setPublicId(publicId);
		this.setPostId(postId);
		this.setText(text);
		this.setLikesCount(likesCount);
		this.setRepostsCount(repostsCount);
		this.setPostDatetime(postDatetime);
	}	
	
	// Метод, осуществляющий конвертацию Unix time в Date, который примет MySQL.
	private String convertUnixtimeToDate(int unixtime)
	{
		java.util.Date time=new java.util.Date((long)unixtime*1000);
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
	}
}
