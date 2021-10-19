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

	// 게임의 첫 시작시 플레이어를 초기화 해줌.
	public void InitializePlayer(int turn)
	{
		life = 2;
		playerTurn = turn;
	}

	// 턴을 패스할 때에 스페어카드를 획득함. 함수 호출 전에 예외처리를 다 해주기 때문에 함수에서는 예외처리 X
	public void GetSpare(int index, int pos)
	{
		if (pos >= hands.size()) hands.add(spare[index]);
		else hands.add(pos, spare[index]);
		spare[index] = null;
	}

	// 카드를 덱에서 손패로 추가함. 이떄 카드의 위치는 선택.
	public void AddCard(Card card, int index)
	{
		if (index >= hands.size()) hands.add(card);
		else hands.add(index, card);
	}

	// 스페어카드가 어떤 카드인지 표시해줌
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

	// 손에 어떤 패가 있는지 표시해줌.
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

	// 내고 싶은 카드를 선택함.
	public Combination SelectCardList(int startIndex, int endIndex, int jokerNumber)
	{
		if ((endIndex - startIndex) >= 3)
		{
			System.out.println("카드는 최대 3장까지만 낼 수 있습니다.");
			return new Combination();
		}

		ArrayList<Card> cardList = new ArrayList<Card>();
		for (int i = startIndex - 1; i < endIndex; i++)
		{
			Card card = hands.get(i);
			Card.CardType type = card.GetType();

			if (type == Card.CardType.joker && jokerNumber != -1)
			{
				// 숫자가 변경된 조커를 넣어줌.
				card = new Card(Card.CardType.number, jokerNumber);
			}

			cardList.add(card);
		}
		Combination comb = new Combination(cardList);
		return comb;
	}

	// 낸 카드를 패에서 제거
	public void RemoveCard(int si, int ei)
	{
		int amount = ei - si + 1;
		for (int i = 0; i < amount; i++)
		{
			this.hands.remove(si - 1);
		}
	}
}
