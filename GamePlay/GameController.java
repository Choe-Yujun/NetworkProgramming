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

	// 가장 처음 게임을 시작할 때 초기화 해줌. 플레이어 정보들도 함께 초기화
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

	// 게임을 시작할 때 처음에 덱을 섞어줘야 하므로 덱을 초기화 한 다음 랜덤으로 재배치함
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

	// 가각의 플레이어의 패와 예비 카드를 세팅해줌
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
			// 덱에서 스페어카드 추가
			Card card = DrawCard();
			player.spare[i] = card;
		}
	}

	// 덱에서 카드를 뽑아서 반환
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
		// 한바퀴가 다 돎
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

		// 만약에 한바퀴가 다 안돌았다면 다음 차례로 넘어감.
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

	// 게임이 끝났는지 확인하여서 값을 넘겨줌
	public boolean CheckGameEnd()
	{
		for (int i = 0; i < playerList.size(); i++)
		{
			if (playerList.get(i).life == -1)
				return true;
		}
		return false;
	}

	// 카드의 조합을 확인하여 처리하는 함수
	public boolean PutCombination(Combination comb, int turn)
	{
		int dif = this.curCom.compareTo(comb);
		// STOP카드를 냈을 경우
		if (dif == 100)
		{
			// 현재 상태에서 턴을 넘겨야하므로 passCount를 더해줌
			this.curWinPlayer = turn;
			this.passCount = this.numberOfPlayer;
			return true;
		}
		// 벌칙카드를 냈을 경우
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
