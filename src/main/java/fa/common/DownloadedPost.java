package fa.common;

import java.text.SimpleDateFormat;

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
	
	// ���� � ����� ���������� ����� � VK.
	private String _postDatetime;

	
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
	
	// ������/������ Datetime
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
	
	// ����������� � int postDatetime
	public DownloadedPost(int publicId, int postId, String text, int likesCount, int repostsCount, int postDatetime)
	{
		this.setPublicId(publicId);
		this.setPostId(postId);
		this.setText(text);
		this.setLikesCount(likesCount);
		this.setRepostsCount(repostsCount);
		this.setPostDatetime(postDatetime);
	}
	
	// ����������� � String postDatetime
	public DownloadedPost(int publicId, int postId, String text, int likesCount, int repostsCount, String postDatetime)
	{
		this.setPublicId(publicId);
		this.setPostId(postId);
		this.setText(text);
		this.setLikesCount(likesCount);
		this.setRepostsCount(repostsCount);
		this.setPostDatetime(postDatetime);
	}	
	
	// �����, �������������� ����������� Unix time � Date, ������� ������ MySQL.
	private String convertUnixtimeToDate(int unixtime)
	{
		java.util.Date time=new java.util.Date((long)unixtime*1000);
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
	}
}
