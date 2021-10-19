package GamePlay;

import java.util.ArrayList;

public class Player
{
	public ArrayList<Card> hands;
	public Card[] spare;
	public int life;
	public int playerTurn;
	public int playerNumber;

	public Player(int number)
	{
		hands = new ArrayList<Card>();
		spare = new Card[2];
		playerNumber = number;
	}

	// ������ ù ���۽� �÷��̾ �ʱ�ȭ ����.
	public void InitializePlayer(int turn)
	{
		life = 2;
		playerTurn = turn;
	}

	// ���� �н��� ���� �����ī�带 ȹ����. �Լ� ȣ�� ���� ����ó���� �� ���ֱ� ������ �Լ������� ����ó�� X
	public void GetSpare(int index, int pos)
	{
		if (pos >= hands.size()) hands.add(spare[index]);
		else hands.add(pos, spare[index]);
		spare[index] = null;
	}

	// ī�带 ������ ���з� �߰���. �̋� ī���� ��ġ�� ����.
	public void AddCard(Card card, int index)
	{
		if (index >= hands.size()) hands.add(card);
		else hands.add(index, card);
	}

	// �����ī�尡 � ī������ ǥ������
	public String PrintSpare()
	{
		String str = "";

		for (int i = 0; i < spare.length; i++)
		{
			if (spare[i] == null) str += "  - ";
			else str += spare[i].CardName();
		}
		return str;
	}

	// �տ� � �а� �ִ��� ǥ������.
	public String PrintHands()
	{
		String str = "";
		str += "Index: ";
		for (int i = 0; i < spare.length; i++)
		{
			str += String.format(" %2d ", i + 1);
		}
		str += "\n";

		str += "Spare: ";
		for (int i = 0; i < spare.length; i++)
		{
			if (spare[i] == null) str += "  - ";
			else str += spare[i].CardName();
		}
		str += "\n";

		str += "Index: ";
		for (int i = 0; i < hands.size(); i++)
		{
			str += String.format(" %2d ", i + 1);
		}
		str += "\n";

		str += "Hands: ";
		for (int i = 0; i < hands.size(); i++)
		{
			str += hands.get(i).CardName();
		}
		return str;
	}

	// ���� ���� ī�带 ������.
	public Combination SelectCardList(int startIndex, int endIndex, int jokerNumber)
	{
		if ((endIndex - startIndex) >= 3)
		{
			System.out.println("ī��� �ִ� 3������� �� �� �ֽ��ϴ�.");
			return new Combination();
		}

		ArrayList<Card> cardList = new ArrayList<Card>();
		for (int i = startIndex - 1; i < endIndex; i++)
		{
			Card card = hands.get(i);
			Card.CardType type = card.GetType();

			if (type == Card.CardType.joker && jokerNumber != -1)
			{
				// ���ڰ� ����� ��Ŀ�� �־���.
				card = new Card(Card.CardType.number, jokerNumber);
			}

			cardList.add(card);
		}
		Combination comb = new Combination(cardList);
		return comb;
	}

	// �� ī�带 �п��� ����
	public void RemoveCard(int si, int ei)
	{
		int amount = ei - si + 1;
		for (int i = 0; i < amount; i++)
		{
			this.hands.remove(si - 1);
		}
	}
}
