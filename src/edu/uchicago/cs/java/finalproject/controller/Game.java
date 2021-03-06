package edu.uchicago.cs.java.finalproject.controller;

import sun.audio.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sound.sampled.Clip;

import edu.uchicago.cs.java.finalproject.game.model.*;
import edu.uchicago.cs.java.finalproject.game.view.*;
import edu.uchicago.cs.java.finalproject.sounds.Sound;

// ===============================================
// == This Game class is the CONTROLLER
// ===============================================

public class Game implements Runnable, KeyListener {

	// ===============================================
	// FIELDS
	// ===============================================

	public static final Dimension DIM = new Dimension(1100, 900); //the dimension of the game.
	private GamePanel gmpPanel;
	public static Random R = new Random();
	public final static int ANI_DELAY = 45; // milliseconds between screen
											// updates (animation)
	private Thread thrAnim;
	private int nLevel = 1;      // bonus points for clearing a level
	private int totalScore = 0;  //score for the current game
	private int nTick = 0;
	private ArrayList<Tuple> tupMarkForRemovals;
	private ArrayList<Tuple> tupMarkForAdds;
	private boolean bMuted = true;
	UFO ufo = new UFO(2);
	boolean ownSpecialWeapon = false;
	boolean killMode = false;
	boolean nuke = false;
	private int shieldPower;
	private int nukeNum = 0;

	

	private final int PAUSE = 80, // p key
			QUIT = 81, // q key
			LEFT = 37, // rotate left; left arrow
			RIGHT = 39, // rotate right; right arrow
			UP = 38, // thrust; up arrow
			START = 83, // s key
			FIRE = 32, // space key
			MUTE = 77, // m-key mute
			CHEAT = 192, // `
			KILLMODE = 75, //k
     		NUKE = 10, //enter
//			HYPERSPACE = 10, //enter to go to hyperspace
	// for possible future use
	// HYPER = 68, 					// d key
	// SHIELD = 65, 				// a key arrow
	// NUM_ENTER = 10, 				// hyp
	 SPECIAL = 70; 					// fire special weapon;  F key

	private Clip clpThrust;
	private Clip clpMusicBackground;
	private Clip laserKillBasic;

	private static final int SPAWN_NEW_SHIP_FLOATER = 1200;
	private static final int SPAWN_WEAPONS_UPGRADE = 1200;
	private static final int SPAWN_NEW_SHIP_UFO = 1200;
	private static final int SPAWN_NEW_SHIELD_FLOATER = 1200;
	private static final int SPAWN_NEW_NUKE_FLOATER = 1100;



	// ===============================================
	// ==CONSTRUCTOR
	// ===============================================

	public Game() {

		gmpPanel = new GamePanel(DIM);
		gmpPanel.addKeyListener(this);

		clpThrust = Sound.clipForLoopFactory("whitenoise.wav");
		clpMusicBackground = Sound.clipForLoopFactory("music-background.wav");
		laserKillBasic = Sound.clipForLoopFactory("kapow.wav");
	

	}

	// ===============================================
	// ==METHODS
	// ===============================================

	public static void main(String args[]) {
		EventQueue.invokeLater(new Runnable() { // uses the Event dispatch thread from Java 5 (refactored)
					public void run() {
						try {
							Game game = new Game(); // construct itself
							game.fireUpAnimThread();

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
	}

	private void fireUpAnimThread() { // called initially
		if (thrAnim == null) {
			thrAnim = new Thread(this); // pass the thread a runnable object (this)
			thrAnim.start();
		}
	}

	// implements runnable - must have run method
	public void run() {

		// lower this thread's priority; let the "main" aka 'Event Dispatch'
		// thread do what it needs to do first
		thrAnim.setPriority(Thread.MIN_PRIORITY);

		// and get the current time
		long lStartTime = System.currentTimeMillis();

		// this thread animates the scene
		while (Thread.currentThread() == thrAnim) {
			tick();
			spawnNewShipFloater();
			spawnUpgradeWeaponFloater();
			spawnUFO();
			spawnNewShieldFloater();
			spawnNewNukeFloater();
			gmpPanel.update(gmpPanel.getGraphics()); // update takes the graphics context we must 
														// surround the sleep() in a try/catch block
														// this simply controls delay time between 
														// the frames of the animation

			//this might be a good place to check for collisions
			checkCollisions();
			//this might be a god place to check if the level is clear (no more foes)
			//if the level is clear then spawn some big asteroids -- the number of asteroids 
			//should increase with the level. 
			checkNewLevel();

			try {
				// The total amount of time is guaranteed to be at least ANI_DELAY long.  If processing (update) 
				// between frames takes longer than ANI_DELAY, then the difference between lStartTime - 
				// System.currentTimeMillis() will be negative, then zero will be the sleep time
				lStartTime += ANI_DELAY;
				Thread.sleep(Math.max(0,
						lStartTime - System.currentTimeMillis()));
			} catch (InterruptedException e) {
				// just skip this frame -- no big deal
				continue;
			}
		} // end while
	} // end run

	private void checkCollisions() {

		
		//@formatter:off
		//for each friend in movFriends
			//for each foe in movFoes
				//if the distance between the two centers is less than the sum of their radii
					//mark it for removal
		
		//for each mark-for-removal
			//remove it
		//for each mark-for-add
			//add it
		//@formatter:on
		
		//we use this ArrayList to keep pairs of movMovables/movTarget for either
		//removal or insertion into our arrayLists later on
		tupMarkForRemovals = new ArrayList<Tuple>();
		tupMarkForAdds = new ArrayList<Tuple>();

		Point pntFriendCenter, pntFoeCenter;
		int nFriendRadiux, nFoeRadiux;

		for (Movable movFriend : CommandCenter.movFriends) {
			for (Movable movFoe : CommandCenter.movFoes) {

				pntFriendCenter = movFriend.getCenter();
				pntFoeCenter = movFoe.getCenter();
				nFriendRadiux = movFriend.getRadius();
				nFoeRadiux = movFoe.getRadius();

				//detect collision
				if (pntFriendCenter.distance(pntFoeCenter) < (nFriendRadiux + nFoeRadiux)) {

					//falcon
					if ((movFriend instanceof Falcon) ){
						 if (CommandCenter.getFalcon().getOwnShield())
						 {
							 if (CommandCenter.getFalcon().getShield() == 0){
								 CommandCenter.getFalcon().setOwnShield(false);
							 }
							 CommandCenter.getFalcon().setShield(CommandCenter.getFalcon().getShield()-1);
							 killFoe(movFoe);
							 for (int i = 0; i < 50; i++) {
								 CommandCenter.movDebris.add(new Debris((Sprite) movFoe, movFoe.getCenter(), movFoe.getCenter()));
							 }
						 }
						 if (!CommandCenter.getFalcon().getProtected() && !CommandCenter.getFalcon().getOwnShield()){
							tupMarkForRemovals.add(new Tuple(CommandCenter.movFriends, movFriend));
							ownSpecialWeapon = false;
							nuke = false;
							nukeNum = 0;
							for (int i = 0; i < 50; i++){
								CommandCenter.movDebris.add(new Debris((Sprite)movFriend, movFriend.getCenter(), movFriend.getCenter()));
							}

							CommandCenter.spawnFalcon(false);
							killFoe(movFoe);

						}
					}
					//not the falcon
					else if(movFriend instanceof Nuke) {
						tupMarkForRemovals.add(new Tuple(CommandCenter.movFriends, movFriend));
					    killFoe(movFoe);
						CommandCenter.movFriends.add(new NukeExplosion((Nuke) movFriend));
					}
					else {
						tupMarkForRemovals.add(new Tuple(CommandCenter.movFriends, movFriend));
						Sound.playSound("kapow.wav");

						for (int i = 0; i < 50; i++) {
							CommandCenter.movDebris.add(new Debris((Sprite) movFoe, movFoe.getCenter(), movFoe.getCenter()));

						}

						killFoe(movFoe);
					}//end else 

					//explode/remove foe

					
				
				}//end if 
			}//end inner for
		}//end outer for


		//check for collisions between falcon and floaters
		if (CommandCenter.getFalcon() != null){
			Point pntFalCenter = CommandCenter.getFalcon().getCenter();
			int nFalRadiux = CommandCenter.getFalcon().getRadius();
			Point pntFloaterCenter;
			int nFloaterRadiux;
			
			for (Movable movFloater : CommandCenter.movFloaters) {
				pntFloaterCenter = movFloater.getCenter();
				nFloaterRadiux = movFloater.getRadius();
	
				//detect collision
				if (pntFalCenter.distance(pntFloaterCenter) < (nFalRadiux + nFloaterRadiux)) {
	
					
					tupMarkForRemovals.add(new Tuple(CommandCenter.movFloaters, movFloater));
					totalScore += 10;
					CommandCenter.setScore(totalScore);
					CommandCenter.setHighScore();
					if (movFloater instanceof NewNukeFloater)
					{
						nuke = true;
						nukeNum = 2;
					}
					if (movFloater instanceof UpgradeWeaponFloater)
					{
						ownSpecialWeapon = true;
						Sound.playSound("pacman_eatghost.wav");
						break;
					}
					else if (movFloater instanceof NewShieldFloater)
					{
						CommandCenter.getFalcon().setShield(5);
						shieldPower = CommandCenter.getFalcon().getShield();
						CommandCenter.getFalcon().setOwnShield(true);
						CommandCenter.setOwnShield(true);
						Sound.playSound("pacman_eatghost.wav");
						break;
					}
					else if (movFloater instanceof NewNukeFloater){
						nukeNum += 1;
						nuke = true;
						Sound.playSound("pacman_eatghost.wav");
						break;
					}
					else {
						CommandCenter.setNumFalcons(CommandCenter.getNumFalcons() + 1);
					}
					Sound.playSound("pacman_eatghost.wav");
	
				}//end if 
			}//end inner for
		}//end if not null
		
		//remove these objects from their appropriate ArrayLists
		//this happens after the above iterations are done
		for (Tuple tup : tupMarkForRemovals) 
			tup.removeMovable();
		
		//add these objects to their appropriate ArrayLists
		//this happens after the above iterations are done
		for (Tuple tup : tupMarkForAdds) 
			tup.addMovable();

		//call garbage collection
		System.gc();
		
	}//end meth

	private void killFoe(Movable movFoe) {
		
		if (movFoe instanceof Asteroid){

			//we know this is an Asteroid, so we can cast without threat of ClassCastException
			Asteroid astExploded = (Asteroid)movFoe;
			//big asteroid 
			if(astExploded.getSize() == 0){
				//spawn two medium Asteroids
				tupMarkForAdds.add(new Tuple(CommandCenter.movFoes,new Asteroid(astExploded)));
				tupMarkForAdds.add(new Tuple(CommandCenter.movFoes,new Asteroid(astExploded)));
				//if kill mode is on, killing spiders will draw the ire of small witches. They don't shoot though. only
				//the big ones do.
				if (killMode == true){
					tupMarkForAdds.add(new Tuple(CommandCenter.movFoes, new UFO(3)));
				}
				CommandCenter.setScore(totalScore += 100);
				CommandCenter.setHighScore();
				
			} 
			//medium size aseroid exploded
			else if(astExploded.getSize() == 1){
				//spawn three small Asteroids
				tupMarkForAdds.add(new Tuple(CommandCenter.movFoes,new Asteroid(astExploded)));
				tupMarkForAdds.add(new Tuple(CommandCenter.movFoes,new Asteroid(astExploded)));
				tupMarkForAdds.add(new Tuple(CommandCenter.movFoes,new Asteroid(astExploded)));
				CommandCenter.setScore(totalScore += 50);
				CommandCenter.setHighScore();
			}
			//remove the original Foe	
			tupMarkForRemovals.add(new Tuple(CommandCenter.movFoes, movFoe));
			CommandCenter.setScore(totalScore += 25);
			CommandCenter.setHighScore();
			
		} 
		//not an asteroid
		else {
			//remove the original Foe
			tupMarkForRemovals.add(new Tuple(CommandCenter.movFoes, movFoe));
			CommandCenter.setScore(totalScore += 1000);
			CommandCenter.setHighScore();
		}
	}

	//some methods for timing events in the game,
	//such as the appearance of UFOs, floaters (power-ups), etc. 
	public void tick() {
		if (nTick == Integer.MAX_VALUE)
			nTick = 0;
		else
			nTick++;
	}

	public int getTick() {
		return nTick;
	}

	private void spawnNewShipFloater() {
		//make the appearance of power-up dependent upon ticks and levels
		//the higher the level the more frequent the appearance
		if (nTick % (SPAWN_NEW_SHIP_FLOATER - nLevel * 7) == 0) {
			CommandCenter.movFloaters.add(new NewShipFloater());
		}
	}
	private void spawnNewNukeFloater(){
		//these become way move valuable deeper into the game.  Piece of advice, just use the cheat code and get one whenever
		// you want.
		if (nTick % (SPAWN_NEW_NUKE_FLOATER - nLevel * 15) == 0){
			CommandCenter.movFloaters.add(new NewNukeFloater());
		}
	}

	private void spawnNewShieldFloater(){
		if (nTick % (SPAWN_NEW_SHIELD_FLOATER - nLevel * 10) == 0){
			CommandCenter.movFloaters.add(new NewShieldFloater());

		}
	}

	private void spawnUpgradeWeaponFloater(){
		//this weapon should be given early in the game, and then occassionally thereafter
		if (nTick % (SPAWN_WEAPONS_UPGRADE - nLevel * 5) == 0){
			CommandCenter.movFloaters.add(new UpgradeWeaponFloater());
		}
	}



	// Called when user presses 's'
	private void startGame() {
		CommandCenter.clearAll();
		CommandCenter.initGame();
		CommandCenter.setLevel(0);
		ownSpecialWeapon = false;
		CommandCenter.setHighScore();
		totalScore = 0;
		CommandCenter.setPlaying(true);
		CommandCenter.setPaused(false);
		//if (!bMuted)
		   // clpMusicBackground.loop(Clip.LOOP_CONTINUOUSLY);
	}

	//this method spawns new asteroids
	private void spawnAsteroids(int nNum) {
		for (int nC = 0; nC < nNum; nC++) {
			//Asteroids with size of zero are big
			CommandCenter.movFoes.add(new Asteroid(0));
		}
	}
	private void spawnUFO(){
		if (nTick % (SPAWN_NEW_SHIP_UFO - nLevel * 100) == 0) {
			CommandCenter.movFoes.add(ufo);
			if (getTick() % 15 == 0){
				CommandCenter.movFoes.add(new BulletUFO(ufo, 0));
			}
			Sound.playSound("witchLaugh.wav");

		}
	}
	
	
	private boolean isLevelClear(){
		//if there are no more Asteroids on the screen
		boolean bAsteroidFree = true;
		for (Movable movFoe : CommandCenter.movFoes) {
			if (movFoe instanceof Asteroid){
				bAsteroidFree = false;
				break;
			}
		}
		
		return bAsteroidFree;
	}
	
	private void checkNewLevel(){
		
		if (isLevelClear() ){
			if (CommandCenter.getFalcon() !=null)
				CommandCenter.getFalcon().setProtected(true);

			
			spawnAsteroids(CommandCenter.getLevel() + 2);
			CommandCenter.setLevel(CommandCenter.getLevel() + 1);
			totalScore += (CommandCenter.getLevel()-1)*100;
			CommandCenter.setScore(totalScore);

		}
	}
	

	// Varargs for stopping looping-music-clips
	private static void stopLoopingSounds(Clip... clpClips) {
		for (Clip clp : clpClips) {
			clp.stop();
		}
	}

	// ===============================================
	// KEYLISTENER METHODS
	// ===============================================

	@Override
	public void keyPressed(KeyEvent e) {
		Falcon fal = CommandCenter.getFalcon();
		int nKey = e.getKeyCode();
		// System.out.println(nKey);

		if (nKey == START && !CommandCenter.isPlaying())
			startGame();

		if (fal != null) {

			switch (nKey) {
			case PAUSE:
				CommandCenter.setPaused(!CommandCenter.isPaused());
				if (CommandCenter.isPaused())
					stopLoopingSounds(clpMusicBackground, clpThrust);
				else
					clpMusicBackground.loop(Clip.LOOP_CONTINUOUSLY);
				break;
			case QUIT:
				System.exit(0);
				break;
			case UP:
				fal.thrustOn();
				if (!CommandCenter.isPaused())
					clpThrust.loop(Clip.LOOP_CONTINUOUSLY);
				break;
			case LEFT:
				fal.rotateLeft();
				break;
			case RIGHT:
				fal.rotateRight();
				break;
			case CHEAT:
				ownSpecialWeapon = true;
				CommandCenter.getFalcon().setOwnShield(true);
				CommandCenter.getFalcon().setShield(5);
				CommandCenter.setOwnShield(true);
				System.out.println(CommandCenter.getOwnShield());
				nukeNum = 1;
				nuke = true;
				break;
			case KILLMODE:
				if (killMode == false)
				killMode = true;
				else
				killMode = false;
				break;
			case NUKE:
				if (nuke == true && nukeNum > 0)
				{
					Sound.playSound("nukeLaunch.wav");
					CommandCenter.movFriends.add(new Nuke(fal));
					nukeNum -= 1;
					break;
				}
				break;


			// possible future use
			// case KILL:
			// case SHIELD:
			// case NUM_ENTER:

			default:
				break;
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		Falcon fal = CommandCenter.getFalcon();
		int nKey = e.getKeyCode();
		 System.out.println(nKey);

		if (fal != null) {
			switch (nKey) {


			case FIRE:
				if (ownSpecialWeapon == false)
				{
					CommandCenter.movFriends.add(new Bullet(fal));
					Sound.playSound("laser.wav");
				}
				else if (ownSpecialWeapon == true)
				{
					CommandCenter.movFriends.add(new SpecialBullet(fal, 320, 320));
					CommandCenter.movFriends.add(new SpecialBullet(fal, 340, 340));
					CommandCenter.movFriends.add(new SpecialBullet(fal, 20, 20));
					CommandCenter.movFriends.add(new SpecialBullet(fal, 40, 40));
					Sound.playSound("specialLaser.wav");
				}
				if (CommandCenter.movFoes.contains(ufo))
				{
					CommandCenter.movFoes.add(new BulletUFO(ufo, 90));
					CommandCenter.movFoes.add(new BulletUFO(ufo, 270));
					CommandCenter.movFoes.add(new BulletUFO(ufo, 0));

				}



				break;
				
			//special is a special weapon, current it just fires the cruise missile. 
			case SPECIAL:
				CommandCenter.movFriends.add(new Cruise(fal));
				//Sound.playSound("laser.wav");
				break;

			case LEFT:
				fal.stopRotating();
				break;
			case RIGHT:
				fal.stopRotating();
				break;
			case UP:
				fal.thrustOff();
				clpThrust.stop();
				break;
			case MUTE:
				if (!bMuted){
					stopLoopingSounds(clpMusicBackground);
					bMuted = !bMuted;
					break;
				}
				else {
					clpMusicBackground.loop(Clip.LOOP_CONTINUOUSLY);
					bMuted = !bMuted;
				}
				break;
				
				
			default:
				break;
			}
		}
	}

	@Override
	// Just need it b/c of KeyListener implementation
	public void keyTyped(KeyEvent e) {
	}
	

	
}

// ===============================================
// ==A tuple takes a reference to an ArrayList and a reference to a Movable
//This class is used in the collision detection method, to avoid mutating the array list while we are iterating
// it has two public methods that either remove or add the movable from the appropriate ArrayList 
// ===============================================

class Tuple{
	//this can be any one of several CopyOnWriteArrayList<Movable>
	private CopyOnWriteArrayList<Movable> movMovs;
	//this is the target movable object to remove
	private Movable movTarget;
	
	public Tuple(CopyOnWriteArrayList<Movable> movMovs, Movable movTarget) {
		this.movMovs = movMovs;
		this.movTarget = movTarget;
	}
	
	public void removeMovable(){
		movMovs.remove(movTarget);
	}
	
	public void addMovable(){
		movMovs.add(movTarget);
	}

}
