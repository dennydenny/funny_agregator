package fa.rankprocessing;

public abstract class AbstractRule {
	
	// ���������� �������.
	public abstract void executeRanking();
	
	// ��������� ����� �������.
	public abstract String getRuleName();
	
	// ������ ����������� � ��.
	public void setRankToDB()
	{
		System.out.println("Works!");
	}
}
