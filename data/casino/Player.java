package Casino;

import java.util.LinkedList;
import java.util.Stack;

/**
 * This inteface depicts a player
 */

public interface Player {

	/**
	 * This method returns player's name
	 * 
	 * @return String name of the player
	 */

	public String getName();

	/**
	 * This method returns player's current points
	 * 
	 * @return integer value of the player's points
	 */

	public int getPoints();

	/**
	 * This method sets a card on table from player's hand, this move
	 * automatically ends turn
	 */

	public void setCardOnTable(Card card);

	/**
	 * This method calculates player's points
	 */

	public void calculatePoints();

	/**
	 * This method returns players hand
	 * 
	 * @return LinkedList<Card> Player's hand
	 */

	public LinkedList<Card> getHand();

	/**
	 * This method returns next player next to this player
	 */

	public Player getNext();

	/**
	 * This method returns the player's amount of cards in the point stack
	 * 
	 * @return int point cards of this player
	 */

	public int getPointCards();

	/**
	 * This method sets next player next to this player
	 */

	public void setNext(Player player);

	/**
	 * This methods makes player draw a card from stack or from table
	 */

	public void draw(Card card);

	/**
	 * This method makes player gain cards to his/her point stack
	 */

	public void gainCards(LinkedList<Card> pointCards);

	/**
	 * This method set for player the card player decided to play
	 * 
	 * @param card
	 *            to be set as play card
	 */

	public void setCardToPlay(Card card);

	/**
	 * This method returns the card player decided to play This is needed by
	 * checkValidity() in Table
	 * 
	 * @return card set as play cards
	 */

	public Card getCardToPlay();

	/**
	 * This method tells if player is human
	 * 
	 * @return true is player is human else false
	 */

	public boolean isHuman();

	/**
	 * This method add a full house for player
	 */

	public void addFullHouse();

	/**
	 * This method tells if player is allowed to draw a card
	 */

	public boolean canDraw();

	/**
	 * This method adds one point for player
	 */

	public void gainPoint();

	/**
	 * This method is a helping method for calculating points
	 * 
	 * @return int amount of spades in the point stack
	 */

	public int getSpades();

	/**
	 * This method plays turn for this AI player
	 * 
	 * @return String describing what player did
	 */

	public String playTurn();

	/**
	 * This method makes this player gain rest cards from table
	 * 
	 * @param cards
	 *            to gain, last cards in table at the end of round
	 */

	public void gainRest(LinkedList<Card> cards);

	/**
	 * This is a helper method for save game, returns full houses
	 * 
	 * @return int full houses
	 */

	public int getFullHouses();

	/**
	 * This is a helper method for load game, sets full houses for player
	 */

	public void setFullHouses(int fullHouses);

	/**
	 * This method is helper method for saves game
	 * 
	 * @return Stack<Card> point stack
	 */
	public Stack<Card> getPointStack();

}
