package fa.rankprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudienceInvolvementRule extends AbstractRule {

	private static final Logger LOG = LoggerFactory.getLogger(AudienceInvolvementRule.class);
	private final String _ruleName = "Вовлеченность_аудитории";
	
	// Метод запуска оценки (IRankable).
	public void executeRanking()
	{
			
	}
		
	// Метод, возвращающий имя правила.
	public String getRuleName()
	{
		return _ruleName;
	}
}
