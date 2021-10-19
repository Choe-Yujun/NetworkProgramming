package Network;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameCommand extends Remote
{
	public int GetPlayerAmount() throws RemoteException;

	public boolean start() throws RemoteException;

	public void AddPlayer(int playerNumber) throws RemoteException;

	public String RemovePlayer(int playerNumber) throws RemoteException;

	public String ShowHands(int playerNumber) throws RemoteException;

	public int PassWithSpare(int playerNumber, int index, int pos) throws RemoteException;

	public int PutCard(int playerNumber, int startindex, int endindex, int joker) throws RemoteException;

	public boolean GetCard(int playerNumber, int index) throws RemoteException;

	public String CurrentCombination() throws RemoteException;

	public void PassTurn() throws RemoteException;

	public String GetRedrawInfo() throws RemoteException;

	public int GetRedrawAmount(int playerNumber) throws RemoteException;

	public int GetTurn(int playerNumber) throws RemoteException;

	public String GetCurrentTurn() throws RemoteException;

	public String CheckWin(int playerNum) throws RemoteException;

	public String LifeInfo() throws RemoteException;

	public String ResultInfo() throws RemoteException;
}
