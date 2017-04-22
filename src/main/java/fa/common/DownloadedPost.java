package fa.common;

import java.text.SimpleDateFormat;
import java.util.Date;

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
	
	// Кол-во просмотров поста.
	private int _viewsCount;
	
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
	
	// Геттер/сеттер ViewsCount
	public int getViewsCount ()
	{
		return this._viewsCount;
	}
		
	public void setViewsCount (int viewsCount)
	{
		this._viewsCount = viewsCount;
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
	public DownloadedPost(int publicId, int postId, String text, int likesCount, int repostsCount, int viewsCount, int postDatetime)
	{
		this.setPublicId(publicId);
		this.setPostId(postId);
		this.setText(text);
		this.setLikesCount(likesCount);
		this.setRepostsCount(repostsCount);
		this.setViewsCount(viewsCount);
		this.setPostDatetime(postDatetime);
	}
	
	// Конструктор с String postDatetime
	public DownloadedPost(int publicId, int postId, String text, int likesCount, int repostsCount, int viewsCount, String postDatetime)
	{
		this.setPublicId(publicId);
		this.setPostId(postId);
		this.setText(text);
		this.setLikesCount(likesCount);
		this.setRepostsCount(repostsCount);
		this.setViewsCount(viewsCount);
		this.setPostDatetime(postDatetime);
	}	
	
	// Метод, осуществляющий конвертацию Unix time в Date, который примет MySQL.
	private String convertUnixtimeToDate(int unixtime)
	{
		Date time=new java.util.Date((long)unixtime*1000);
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
	}

	// Метод, возвращающий пост в таком виде, который подходит для репоста через SDK.
	public String ToRepostObject()
	{
		StringBuilder sb = new StringBuilder("wall-");
		sb.append(this.getPublicId());
		sb.append("_" + this.getPostId());
		return sb.toString();
	}
}
