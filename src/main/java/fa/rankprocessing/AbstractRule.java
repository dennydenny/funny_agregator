package fa.rankprocessing;

public abstract class AbstractRule {
	
	// Выполнение правила.
	public abstract void executeRanking();
	
	// Получение имени правила.
	public abstract String getRuleName();
}
