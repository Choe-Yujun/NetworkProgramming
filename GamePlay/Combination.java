package GamePlay;

import java.util.ArrayList;
import java.util.Arrays;

public class Combination
{
	public enum ComType { none, single, straight, pare, redraw, stop }

	private int cardAmount;
	private int highNumber;
	private ComType comType;

	public int GetAmount()
	{
		return cardAmount;
	}

	public int GetHighNumber()
	{
		return highNumber;
	}

	public ComType GetCombinationType()
	{
		return comType;
	}

	// 조합이 없는 경우로 기본 생성
	public Combination()
	{
		cardAmount = -1;
		highNumber = -1;
		comType = ComType.none;
	}

	// 생성자로 카드의 목록을 받아 만들어지는 조합이 됨.
	public Combination(ArrayList<Card> cardList)
	{
		cardAmount = cardList.size();
		comType = ComType.none;
		highNumber = -1;

		// 카드의 숫자가 한 종류일 경우
		if (cardAmount == 1)
		{
			Card card = cardList.get(0);
			Card.CardType type = card.GetType();
			// 숫자카드의 경우 최대 숫자 설정
			if (type == Card.CardType.number)
			{
				highNumber = card.GetNumber();
				comType = ComType.single;
			}
			if (type == Card.CardType.redraw)
			{
				highNumber = -1;
				comType = ComType.redraw;
			}
			if (type == Card.CardType.stop)
			{
				highNumber = -1;
				comType = ComType.stop;
			}
		}
		else
		{
			Card prevCard = null;
			for (int i = 0; i < cardAmount; i++)
			{
				Card card = cardList.get(i);
				Card.CardType type = card.GetType();

				// 숫자의 조합이 아니면 조합 X
				if (type != Card.CardType.number)
				{
					highNumber = -1;
					comType = ComType.none;
					break;
				}

				if (prevCard != null)
				{
					int prevNum = prevCard.GetNumber();
					int curNum = card.GetNumber();

					// 페어나 조합이 없는 상태에서 숫자가 같으면 페어.
					if (prevNum == curNum && (comType == ComType.none || comType == ComType.pare))
					{
						comType = ComType.pare;
						highNumber = curNum;
					}
					// 그 외의 경우는 먼저 스트레이트로 설정해둔 뒤 후에 재확인해줌.
					else comType = ComType.straight;
				}
				prevCard = cardList.get(i);
			}

			// 스트레이트의 경우에는 순서 상관없이 낼 수 있기 때문에 모든 예외로 설정해둔 뒤 식을 통해서 재확인함.
			if (comType == ComType.straight)
			{
				// 먼저 숫자들을 정렬해준 뒤 각 값들이 1씩 차이난다면 스트레이트
				int[] numberList = new int[cardAmount];
				for (int i = 0; i < cardAmount; i++)
				{
					numberList[i] = cardList.get(i).GetNumber();
				}
				Arrays.sort(numberList);

				int prevNum = -1;
				for (int i = 0; i < cardAmount; i++)
				{
					if (i != 0)
					{
						// 현재 값에서 이전 값을 빼면 1이 나와야 함.
						if (numberList[i] - prevNum == 1)
						{
							// 마지막 숫자일 경우 최고값을 저장해둠.
							if (i == cardAmount - 1)
							{
								highNumber = numberList[i];
							}
						}
						else
						{
							highNumber = -1;
							comType = ComType.none;
							break;
						}
					}
					prevNum = numberList[i];
				}
			}
		}
	}

	// 조합 내용을 변경함.
	public void ChangeCombination(Combination c)
	{
		this.cardAmount = c.GetAmount();
		this.highNumber = c.GetHighNumber();
		this.comType = c.GetCombinationType();
	}

	// 조합끼리 어떠한 조합이 더 강한지 비교함. 0은 동일, 0보다 크면 자신이 크고 작으면 비교대상이 큼.
	public int compareTo(Combination c)
	{
		if (c.comType == ComType.none)
		{
			if (this.comType == ComType.none)
				return 0;
			else return 1;
		}
		if (c.comType == ComType.stop)
			return 100;
		if (c.comType == ComType.redraw)
			return 200;

		int diff = cardAmount - c.GetAmount();
		if (diff == 0) // 카드의 갯수가 다르면 순위가 정해짐
		{
			diff = comType.compareTo(c.GetCombinationType());
			if (diff == 0) // 카드의 등급이 다르면 순위가 정해짐
			{
				diff = highNumber - c.GetHighNumber();
			}
		}
		// TODO Auto-generated method stub
		return diff;
	}

	public String CombinationInfo()
	{
		String str = "";
		switch (comType)
		{
			case single:
				str += Integer.toString(this.highNumber) + " ";
				str += "Single";
				break;
			case straight:
				for (int i = cardAmount - 1; i >= 0; i--)
					str += Integer.toString(this.highNumber - i) + " ";
				str += "Straight";
				break;
			case pare:
				for (int i = 0; i < this.cardAmount; i++)
					str += Integer.toString(this.highNumber) + " ";
				str += "Pare";
				break;
			case none:
				str += "None";
				break;
			case redraw:
				str += "벌칙카드";
				break;
			case stop:
				str += "STOP";
				break;
		}
		return str;
	}
}
