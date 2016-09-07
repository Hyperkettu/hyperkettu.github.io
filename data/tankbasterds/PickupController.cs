using UnityEngine;
using System.Collections;

public class PickupController : MonoBehaviour {
	
	public WheelCollider FrontLeftWheel;
	public WheelCollider FrontRightWheel;
	public WheelCollider BackLeftWheel;
	public WheelCollider BackRightWheel;
	public float[] GearRatio;
	public int CurrentGear;
	public float EngineTorque = 230.0f;
	public float MaxEngineRPM = 3000.0f;
	public float MinEngineRPM = 1000.0f;
	public Vector3 CenterOfMassOffset = new Vector3(0, -0.75f, 0.25f);
	public float EngineRPM = 0.0f;
	public float brakePercentage = 0.3f;
	public float engineBrakePercentage = 0.01f;
	
	public bool DEBUG = true;
	
	private PSMoveWrapper psMoveWrapper;
	public int controllerId = 0;
	
	public float brakeRatio = 1.0f;
	
	public float rearLimiter = 0.1f;
	
	public GameObject explosion;
	
	private Vector3 initPos;
	private Quaternion initRot;
	private float deadTimer = 0f;
	private bool dead = false;
	
	
	// Use this for initialization
	void Start () {
		rigidbody.centerOfMass += CenterOfMassOffset; //this.CenterOfMass.position;
		psMoveWrapper = FindObjectOfType(typeof(PSMoveWrapper)) as PSMoveWrapper;
		this.initPos = transform.position;
		this.initRot = transform.rotation;
	}
	
	// Update is called once per frame
	void Update () {

		// if health runs out, make player disappear
		if (PlayerAttributes.health == 0 && !dead) {
			this.deadTimer = 8f;
			GameObject.Find("GUI").GetComponent<OurGUI>().displayMessage("GAME OVER!");
			Renderer[] renderers = GetComponentsInChildren<Renderer>();
			foreach (Renderer r in renderers) {
				r.enabled = false;	
			}
			rigidbody.velocity = Vector3.zero;
			GameObject exp = Instantiate(explosion, transform.position, Quaternion.identity) as GameObject;
			Destroy(exp, 5.0f);
			dead = true;
		}
		
		if (Input.GetKey(KeyCode.R)) {
			PlayerAttributes.health = 0f;
		}
		
		
		// if player is dead, respawn after the timer runs out of time
		if (dead) {
			deadTimer -= Time.deltaTime;
			if (deadTimer < 0f) {
				dead = false;
				Renderer[] renderers = GetComponentsInChildren<Renderer>();
				foreach (Renderer r in renderers) {
					r.enabled = true;	
				}
				PlayerAttributes.points = 0f;
				RankHandler.changeRank = PlayerAttributes.addPoints(0f);
				this.transform.position = this.initPos;
				this.rigidbody.velocity = Vector3.zero;
				this.transform.rotation = this.initRot;
				this.rigidbody.angularVelocity = Vector3.zero;
				PlayerAttributes.health = 100f;
				PlayerAttributes.ammo = 0;
				PlayerAttributes.rank = PlayerAttributes.Rank.PRIVATE;
			}
			return;
		}
		
		
		float x, y;
		
		if(!DEBUG) {
			// use PlayStation Move to handle the pickup
			x = psMoveWrapper.valueNavAnalogX[controllerId] / 128.0f;
			y = -psMoveWrapper.valueNavAnalogY[controllerId] / 128.0f;
			
		} else { // use keyboard in DEBUG mode to control the warWagon
			
			x = Input.GetAxis("Horizontal");
			y = Input.GetAxis("Vertical");
		}
		

		
		this.EngineRPM = (this.FrontLeftWheel.rpm + this.FrontRightWheel.rpm) / 2 *
		this.GearRatio[this.CurrentGear];
		this.ShiftGears();
		
		audio.pitch = Mathf.Abs(EngineRPM / MaxEngineRPM) + 1.0f ;
		
		if ( audio.pitch > 2.0f ) 
		{
			audio.pitch = 2.0f;
		}
		
		// if no gas
		if (y == 0) {
			audio.pitch = 1;
		}
		

		
		this.FrontLeftWheel.steerAngle = 10 * x;
		this.FrontRightWheel.steerAngle = 10 * x;
		float brake = 0.0f;
		
		
		if(Vector3.Dot(rigidbody.velocity, transform.forward) > 0.2f) {
 			brake = y < 0 ? rigidbody.mass * brakePercentage: 0.0f;
		}
		
		// make the pickup brake
		if(brake > 0.0){
			
            FrontLeftWheel.brakeTorque = brake;
            FrontRightWheel.brakeTorque = brake;
            BackLeftWheel.brakeTorque = brake;
            BackRightWheel.brakeTorque = brake;
			this.FrontLeftWheel.motorTorque = 0;
			this.FrontRightWheel.motorTorque = 0;
			
        } else {
			// if player accelerates
			if (y > 0.0f) {
            	FrontLeftWheel.brakeTorque = 0.0f;
            	FrontRightWheel.brakeTorque = 0.0f;
            	BackLeftWheel.brakeTorque = 0.0f;
            	BackRightWheel.brakeTorque = 0.0f;
			} else if(y == 0) {
				// if no gas, make the engine to brake automatically
				float engineBrake = rigidbody.mass * engineBrakePercentage;
				FrontLeftWheel.brakeTorque = engineBrake;
            	FrontRightWheel.brakeTorque = engineBrake;
            	BackLeftWheel.brakeTorque = engineBrake;
            	BackRightWheel.brakeTorque = engineBrake;
			}
	
			// make the pickup accelerate, front wheels only
			this.FrontLeftWheel.motorTorque = this.EngineTorque / this.GearRatio[this.CurrentGear] * y;
			this.FrontRightWheel.motorTorque = this.EngineTorque / this.GearRatio[CurrentGear] * y;
			
			// if player wants to brake
			if (y < 0) {
				this.FrontLeftWheel.motorTorque = this.EngineTorque / brakeRatio * y;
				this.FrontRightWheel.motorTorque = this.EngineTorque / brakeRatio * y;
				float v = rigidbody.velocity.magnitude;
				FrontLeftWheel.brakeTorque = rigidbody.mass * rearLimiter * v;
            	FrontRightWheel.brakeTorque = rigidbody.mass * rearLimiter * v;
            	BackLeftWheel.brakeTorque = rigidbody.mass * rearLimiter * v;
            	BackRightWheel.brakeTorque = rigidbody.mass * rearLimiter * v;
			}
        }
	}
	
	// this shifts gears automatically
	private void ShiftGears()
	{
		int AppropriateGear = CurrentGear;
		
		if ( this.EngineRPM >= this.MaxEngineRPM ) 
		{
			for ( int i = 0; i < this.GearRatio.Length; i ++ ) 
			{
				if ( this.FrontLeftWheel.rpm * this.GearRatio[i] < this.MaxEngineRPM ) 
				{
					AppropriateGear = i;
					break;
				}
			}
		
			this.CurrentGear = AppropriateGear;
		}
	
		if ( this.EngineRPM <= this.MinEngineRPM ) 
		{
			AppropriateGear = CurrentGear;
		
			for ( int j = this.GearRatio.Length-1; j >= 0; j -- ) {
				if ( this.FrontLeftWheel.rpm * this.GearRatio[j] > this.MinEngineRPM ) {
					AppropriateGear = j;
					break;
				}
			}
		
		this.CurrentGear = AppropriateGear;
		}	
	}
}
