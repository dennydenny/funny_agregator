package fa.grubber;

public class Public {
	
	// ������������� �������.
	private int _id;
	
	// �������� �������.
	private String _name;
	
	// URL �������.
	private String _url;
	
	// ���-�� �����������.
	private int _subsCount;
	
	public int getPublicId()
	{
		return _id;
	}
	
	public void setPublicId(int id)
	{
		_id = id;
	}
	
	public void setPublicName(String name)
	{
		_name = name;
	}
	
	public String getPublicName()
	{
		return _name;
	}
	
	public String getPublicUrl()
	{
		return _url;
	}
	
	public void setPublicUrl(String url)
	{
		_url = url;
	}
	
	public int getPublicSubsCount()
	{
		return _subsCount;
	}
	
	public void setPublicSubsCount(int count)
	{
		_subsCount = count;
	}
}
