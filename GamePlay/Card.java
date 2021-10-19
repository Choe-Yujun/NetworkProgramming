package GamePlay;

public class Card
{
	enum CardType { number, joker, stop, redraw }

	private CardType cardType;
	private int number;

	public int GetNumber()
	{
		return number;
	}

	public CardType GetType()
	{
		return cardType;
	}

	public Card(CardType t, int n)
	{
		cardType = t;
		number = n;
	}

	public String CardName()
	{
		String str = "";
		switch (cardType)
		{
			case number:
				str += String.format(" %2d ", number);
				break;
			case joker:
				str += "  X ";
				break;
			case stop:
				str += "  S ";
				break;
			case redraw:
				str += "  R ";
				break;
		}
		return str;
	}

	public String CardInfo()
	{
		String str = "";
		switch (cardType)
		{
			case number:
				str += "[���� " + number + "]";
				break;
			case joker:
				str += "[��Ŀ]";
				break;
			case stop:
				str += "[����]";
				break;
			case redraw:
				str += "[��Ģ]";
				break;
		}
		return str;
	}
}
