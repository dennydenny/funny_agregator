package fa.rankprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudienceInvolvementRule extends AbstractRule {

	private static final Logger LOG = LoggerFactory.getLogger(AudienceInvolvementRule.class);
	private final String _ruleName = "�������������_���������";
	
	// ����� ������� ������ (IRankable).
	public void executeRanking()
	{
			
	}
		
	// �����, ������������ ��� �������.
	public String getRuleName()
	{
		return _ruleName;
	}
}
