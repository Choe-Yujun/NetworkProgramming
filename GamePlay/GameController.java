package GamePlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class GameController
{
	private ArrayList<Card> cardDeck = new ArrayList<Card>();
	private int curTurn;
	private int passCount;
	private int passCheck;
	private int numberOfPlayer;

	private int curWinPlayer;
	private Combination curCom;
	private int redrawAmount;

	private ArrayList<Player> playerList;
	public ArrayList<Integer> winTurn;

	public int round;

	public GameController(ArrayList<Player> playerList)
	{
		round = 0;
		this.playerList = playerList;
		numberOfPlayer = this.playerList.size();
		winTurn = new ArrayList<Integer>();

		long seed = System.nanoTime();
		Collections.shuffle(playerList, new Random(seed));
		for (int i = 0; i < numberOfPlayer; i++)
		{
			playerList.get(i).InitializePlayer(i);
		}
		InitializeGame(0);
	}

	// ���� ó�� ������ ������ �� �ʱ�ȭ ����. �÷��̾� �����鵵 �Բ� �ʱ�ȭ
	public boolean InitializeGame(int startTurn)
	{
		if (CheckGameEnd())
		{
			return false;
		}
		winTurn.clear();
		curWinPlayer = curTurn = startTurn;
		passCount = 0;
		passCheck = 0;
		redrawAmount = 0;
		curCom = new Combination();
		ShuffleDeck();
		int handAmt = (numberOfPlayer == 5 ? 7 : 2);
		for (int i = 0; i < numberOfPlayer; i++)
			SetPlayerHands(i, handAmt);
		round++;

		return true;
	}

	// ������ ������ �� ó���� ���� ������� �ϹǷ� ���� �ʱ�ȭ �� ���� �������� ���ġ��
	private void ShuffleDeck()
	{
		cardDeck.clear();

		ArrayList<Card> tmpDeck = new ArrayList<Card>();
		for (int i = 0; i < 4; i++)
		{
			for (int n = 1; n <= 12; n++)
			{
				tmpDeck.add(new Card(Card.CardType.number, n));
			}
		}
		for (int i = 0; i < 2; i++)
		{
			tmpDeck.add(new Card(Card.CardType.joker, -1));
			tmpDeck.add(new Card(Card.CardType.stop, -1));
			tmpDeck.add(new Card(Card.CardType.redraw, -1));
		}

		long seed = System.currentTimeMillis();
		Random rand = new Random(seed);
		for (int i = tmpDeck.size(); i > 0; i--)
		{
			int index = rand.nextInt(i);
			cardDeck.add(tmpDeck.get(index));
			tmpDeck.remove(index);
		}
	}

	// ������ �÷��̾��� �п� ���� ī�带 ��������
	private void SetPlayerHands(int playerNumber, int handAmount)
	{
		Player player = playerList.get(playerNumber);

		player.hands.clear();

		for (int i = 0; i < handAmount; i++)
		{
			Card card = DrawCard();
			player.hands.add(card);
		}

		for (int i = 0; i < 2; i++)
		{
			// ������ �����ī�� �߰�
			Card card = DrawCard();
			player.spare[i] = card;
		}
	}

	// ������ ī�带 �̾Ƽ� ��ȯ
	public Card DrawCard()
	{
		int index = cardDeck.size() - 1;
		Card tmp = cardDeck.get(index);
		cardDeck.remove(index);

		return tmp;
	}

	public boolean PassTurn()
	{
		this.passCount++;
		// �ѹ����� �� ��
		if (this.passCount >= this.numberOfPlayer - this.winTurn.size())
		{
			for (int i = 0; i < playerList.size(); i++)
			{
				if (winTurn.contains(i) == false && playerList.get(i).hands.size() == 0)
				{
					winTurn.add(i);
				}
			}
			this.curCom = new Combination();
			this.curTurn = this.curWinPlayer;

			if (winTurn.size() != playerList.size())
				while (winTurn.contains(this.curTurn))
				{
					this.curTurn--;
					if (this.curTurn < 0)
						this.curTurn = this.numberOfPlayer - 1;
					this.curWinPlayer = curTurn;
				}

			this.passCount = 0;
			this.passCheck = 0;
			return true;
		}

		// ���࿡ �ѹ����� �� �ȵ��Ҵٸ� ���� ���ʷ� �Ѿ.
		for (int i = 0; i < this.numberOfPlayer; i++)
		{
			this.curTurn++;
			if (this.curTurn >= this.numberOfPlayer)
			{
				this.curTurn = 0;
			}
			if (winTurn.contains(curTurn) == false)
				break;
			this.passCount++;
			this.passCheck++;
		}
		return false;
	}

	// ������ �������� Ȯ���Ͽ��� ���� �Ѱ���
	public boolean CheckGameEnd()
	{
		for (int i = 0; i < playerList.size(); i++)
		{
			if (playerList.get(i).life == -1)
				return true;
		}
		return false;
	}

	// ī���� ������ Ȯ���Ͽ� ó���ϴ� �Լ�
	public boolean PutCombination(Combination comb, int turn)
	{
		int dif = this.curCom.compareTo(comb);
		// STOPī�带 ���� ���
		if (dif == 100)
		{
			// ���� ���¿��� ���� �Ѱܾ��ϹǷ� passCount�� ������
			this.curWinPlayer = turn;
			this.passCount = this.numberOfPlayer;
			return true;
		}
		// ��Ģī�带 ���� ���
		else if (dif == 200)
		{
			this.redrawAmount += 3;
			return true;
		}
		else if (dif < 0)
		{
			this.curCom.ChangeCombination(comb);
			this.curWinPlayer = turn;
			return true;
		}
		else
		{
			return false;
		}
	}

	public void RedrawCard()
	{
		redrawAmount--;
	}

	public int GetRedrawAmount()
	{
		return redrawAmount;
	}

	public int GetPassCount()
	{
		return this.passCount - passCheck;
	}

	public int GetTurn()
	{
		return this.curTurn;
	}

	public int GetCurWin()
	{
		return this.curWinPlayer;
	}

	public String GetCombInfo()
	{
		if (this.curCom != null)
			return curCom.CombinationInfo();
		return "";
	}

	public int GetLosePlayer()
	{
		for (int i = 0; i < playerList.size(); i++)
		{
			if (winTurn.contains(i) == false)
				return i;
		}
		return 0;
	}
}
