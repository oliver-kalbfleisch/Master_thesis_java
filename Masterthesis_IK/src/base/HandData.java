package base;

import au.edu.federation.utils.Vec3f;

/**
 * 
 * @author oliver 
 * Utility Class for storing tracking data
 */
public class HandData {

	private Vec3f calibrationHandOrignPos;
	private Vec3f currentHandPos;
	private Vec3f[] calibrationTrackerOrignPos;
	private int numTrackers;
	private Vec3f[] currentTrackerPos;

	public HandData() {
		this.numTrackers = 6;
		this.calibrationHandOrignPos = new Vec3f();
		this.currentHandPos = new Vec3f();
		this.currentTrackerPos = new Vec3f[numTrackers];
		this.calibrationTrackerOrignPos = new Vec3f[numTrackers];
		for (int i = 0; i < calibrationTrackerOrignPos.length; i++) {
			calibrationTrackerOrignPos[i] = new Vec3f();
			currentTrackerPos[i] = new Vec3f();
		}
	}

	/**
	 * Function calculates position data in relation to initialy calibrated origin
	 * position
	 * 
	 * @param fingerNum
	 *            number of finger to update (thumb = 0)
	 * @param updatePos
	 *            the new position from the trackng data
	 * @return the new calculated 3d postion
	 */
	public Vec3f updateCurrentFingerPos(int fingerNum, Vec3f updatePos) {
		Vec3f resultingPos = updatePos.minus(calibrationTrackerOrignPos[fingerNum]);
		currentTrackerPos[fingerNum] = resultingPos;

		return resultingPos;

	}
	/**
	 * 
	 * @param positionData
	 */

	public void calibrateHandOriginPosition(Vec3f[] positionData)
	{
		Vec3f result= new Vec3f(0.0f,0.0f,0.0f);
		
		for(int i=0;i< positionData.length;i++)
		{
			result.plus(positionData[i]);
		}
		calibrationHandOrignPos= result.dividedBy(positionData.length);
		System.out.println("Hand Origin positon calibrated: "+ calibrationHandOrignPos.toString());
	}
	/**
	 * 
	 * @param fingerNum
	 */
	public void calibrateFingerOriginPos(int fingerNum,Vec3f[]positionData)
	{
		Vec3f result= new Vec3f(0.0f,0.0f,0.0f);
		
		for(int i=0;i< positionData.length;i++)
		{
			result=result.plus(positionData[i]);
		}
		calibrationTrackerOrignPos[fingerNum]= result.dividedBy(positionData.length);
		System.out.println("Finger offset for finger n."+fingerNum+" calibrated: "+calibrationTrackerOrignPos[fingerNum].toString());
	}

	public Vec3f getCalibrationHandOrignPos() {
		return calibrationHandOrignPos;
	}

	public void setCalibrationHandOrignPos(Vec3f calibrationHandOrignPos) {
		this.calibrationHandOrignPos = calibrationHandOrignPos;
	}

	public Vec3f[] getCalibrationFingerOrignPos() {
		return calibrationTrackerOrignPos;
	}

	public void setCalibrationFingerOrignPos(Vec3f[] calibrationFingerOrignPos) {
		this.calibrationTrackerOrignPos = calibrationFingerOrignPos;
	}

	public int getNumTrackedFingers() {
		return numTrackers;
	}

	public void setNumTrackedFingers(int numTrackedFingers) {
		this.numTrackers = numTrackedFingers;
	}

	public Vec3f[] getCurrentFingerPos() {
		return currentTrackerPos;
	}
	//TODO  Adapt to new Finger Calibration procedure
	public void setCurrentFingerPos(Vec3f[] currentFingerPos) {
		this.currentTrackerPos = currentFingerPos;
	}

}
