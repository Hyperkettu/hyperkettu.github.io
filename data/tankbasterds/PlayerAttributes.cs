using UnityEngine;
using System.Collections;

public class PlayerAttributes : MonoBehaviour {
	
	public static float health = 100.0f;
	public static float points = 0.0f; 
	public static int ammo = 0;
	public static float shield = 0f;
	public Transform head;
	public Transform foot;
	public GameObject shieldSphere;
	public float duckLimit = 3.0f;
	
	private static int shotCount = 0;
	
	public static ArrayList cargo = new ArrayList();
	
	private RUISSkeletonManager skeletonManager;
	
	// define all army rankings
	public enum Rank {
		PRIVATE,
		LANCE_CORPORAL,
		CORPORAL,
		SERGEANT,
		STAFF_SERGEANT,
		SECOND_LIEUTENANT,
		FIRST_LIEUTENANT,
		CAPTAIN,
		MAJOR, 
		LIEUTENANT_COLONEL,
		COLONEL
		
	};
	
	public static Rank rank = Rank.PRIVATE;
	
	// Use this for initialization
	void Start () {
		skeletonManager = FindObjectOfType(typeof(RUISSkeletonManager)) as RUISSkeletonManager;
	}
	
	// Update is called once per frame
	void Update () {
		if(skeletonManager == null || !skeletonManager.skeletons[0].isTracking){
			return;
		}
		
		// check from the skeleton model that player moving the skeleton crouches
		float headToFootDistance = skeletonManager.skeletons[0].head.position.y;
		PlayerAttributes.shield = (headToFootDistance < duckLimit) ?  0.5f : 0.0f;
		
		// if player crouches, show shield sphere
		if (shield == 0.5f)
			shieldSphere.SetActive(true);
		else
			shieldSphere.SetActive(false);
	}
	
	// manipulates player health
	public static void changeHealth(float change) {
		
		health += (1.0f - PlayerAttributes.shield)*change;
		
		if(health < 0.0f){ 
			health = 0.0f;
		}
		else if( health > 100.0f)
			health = 100.0f;
		
	}
	
	// adds points to the player and army ranking accordingly
	public static bool addPoints(float increase) {
		
		Rank current = rank;
		
		points += increase;
		
		if (points < 20) {
			rank = Rank.PRIVATE;
		} else if (points < 40) {
			rank = Rank.LANCE_CORPORAL;
		} else if (points < 75) {
			rank = Rank.CORPORAL;
		} else if (points < 120) {
			rank = Rank.SERGEANT;
		} else if (points < 180) {
			rank = Rank.STAFF_SERGEANT;
		} else if (points < 250) {
			rank = Rank.SECOND_LIEUTENANT;
		} else if (points < 350) {
			rank = Rank.FIRST_LIEUTENANT;
		} else if (points < 500) {
			rank = Rank.CAPTAIN;
		} else if (points < 700) {
			rank = Rank.MAJOR;
		} else if (points < 1000) {
			rank = Rank.LIEUTENANT_COLONEL;
		} else {
			rank = Rank.COLONEL;
		}
		
		return rank != current;
	}
	
	// shoots one ammo
	public static bool shoot() {
		if (ammo > 0) {
			ammo -= 1;
			shotCount += 1;
			
			// if player has shot 100 ammos, remove one box from the cargo of the pickup
			if(shotCount == 100) {
				shotCount = 0;
				GameObject box = PlayerAttributes.cargo[0] as GameObject;
				PlayerAttributes.cargo.RemoveAt(0);
				Destroy(box);
			}
			return true;
		}
		return false;
	}
	
	// adds ammo
	public static void addAmmo(int count) { 
		ammo += count;
		
		if(ammo < 0) ammo = 0;
	}

}
