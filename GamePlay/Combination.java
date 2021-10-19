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

	// ������ ���� ���� �⺻ ����
	public Combination()
	{
		cardAmount = -1;
		highNumber = -1;
		comType = ComType.none;
	}

	// �����ڷ� ī���� ����� �޾� ��������� ������ ��.
	public Combination(ArrayList<Card> cardList)
	{
		cardAmount = cardList.size();
		comType = ComType.none;
		highNumber = -1;

		// ī���� ���ڰ� �� ������ ���
		if (cardAmount == 1)
		{
			Card card = cardList.get(0);
			Card.CardType type = card.GetType();
			// ����ī���� ��� �ִ� ���� ����
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

				// ������ ������ �ƴϸ� ���� X
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

					// �� ������ ���� ���¿��� ���ڰ� ������ ���.
					if (prevNum == curNum && (comType == ComType.none || comType == ComType.pare))
					{
						comType = ComType.pare;
						highNumber = curNum;
					}
					// �� ���� ���� ���� ��Ʈ����Ʈ�� �����ص� �� �Ŀ� ��Ȯ������.
					else comType = ComType.straight;
				}
				prevCard = cardList.get(i);
			}

			// ��Ʈ����Ʈ�� ��쿡�� ���� ������� �� �� �ֱ� ������ ��� ���ܷ� �����ص� �� ���� ���ؼ� ��Ȯ����.
			if (comType == ComType.straight)
			{
				// ���� ���ڵ��� �������� �� �� ������ 1�� ���̳��ٸ� ��Ʈ����Ʈ
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
						// ���� ������ ���� ���� ���� 1�� ���;� ��.
						if (numberList[i] - prevNum == 1)
						{
							// ������ ������ ��� �ְ����� �����ص�.
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

	// ���� ������ ������.
	public void ChangeCombination(Combination c)
	{
		this.cardAmount = c.GetAmount();
		this.highNumber = c.GetHighNumber();
		this.comType = c.GetCombinationType();
	}

	// ���ճ��� ��� ������ �� ������ ����. 0�� ����, 0���� ũ�� �ڽ��� ũ�� ������ �񱳴���� ŭ.
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
		if (diff == 0) // ī���� ������ �ٸ��� ������ ������
		{
			diff = comType.compareTo(c.GetCombinationType());
			if (diff == 0) // ī���� ����� �ٸ��� ������ ������
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
				str += "��Ģī��";
				break;
			case stop:
				str += "STOP";
				break;
		}
		return str;
	}
}