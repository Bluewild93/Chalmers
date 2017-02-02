import TSim.*;
import java.util.concurrent.Semaphore;

public class MovingTrain implements Runnable {
	
	TSimInterface tsi;
	int trainId, trainSpeed;
	boolean isTrainMovingUpDirection;

	/* We are going to use 6 semaphores in total */
	static Semaphore commonLowerRail 	= new Semaphore(1, true);
	static Semaphore commonMiddleRail 	= new Semaphore(1, true);
	static Semaphore commonUpperRail 	= new Semaphore(1, true);
	
	/* Where both rails intersect with each other */
	static Semaphore intersection 		= new Semaphore(1, true);
	
	/* Represents both end stations*/
	static Semaphore lowerRail 			= new Semaphore(0, true);
	static Semaphore upperRail 			= new Semaphore(0, true);

	public MovingTrain(int trainId, int trainSpeed, boolean isTrainMovingUp) throws CommandException {
		this.trainId = trainId;
		this.trainSpeed = trainSpeed;
		this.isTrainMovingUpDirection = isTrainMovingUp;
		tsi = TSimInterface.getInstance();

		tsi.setSpeed(trainId, trainSpeed);
	}

	public void alterSwitchPos(boolean isSwitchToRight, int switchXPos, int switchYPos) throws CommandException {
		tsi.setSwitch(switchXPos, switchYPos, (isSwitchToRight) ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT);
	}
	
	public void alterSwitchPos(
		boolean isSwitchToRight, int switchXPos, int switchYPos, 
		Semaphore semaphoreToRelease, boolean releaseFromLeft) throws CommandException{
		
		alterSwitchPos(isSwitchToRight, switchXPos, switchYPos);
		if(isSwitchToRight) {
			if(releaseFromLeft)semaphoreToRelease.release();
		}
		else {
			if(!releaseFromLeft) semaphoreToRelease.release();
		}
	}

	public void waitUntilSemaphoreReleased(Semaphore semaphore) throws CommandException, InterruptedException {
		/* Stop the train. Set its speed to zero. */
		tsi.setSpeed(trainId, 0);
		
		/* increase the number of permits for the available semaphore */
		semaphore.acquire();
		
		/* set the speed back to original */
		tsi.setSpeed(trainId, trainSpeed);
	} 
	
	public void endStation() throws CommandException, InterruptedException {
		
		/* Change current direction of train */
		isTrainMovingUpDirection = !isTrainMovingUpDirection;
		
		/* Change the speed to look the opposite direction */
		trainSpeed = -trainSpeed;

		/* Stop the train by setting its speed to 0 */
		tsi.setSpeed(trainId, 0);
		
		/* Make it wait a lil'bit by setting the thread to sleep */
		Thread.sleep(1000 + (20 * Math.abs(trainSpeed)));
		
		/* Set its speed again after the wait, in the opposite direction */
		tsi.setSpeed(trainId, trainSpeed);
	}
	
	public void waitUntilReSwitched(
		Semaphore semaphore, int switchXPos, int switchYPos, 
		boolean switchToLeft, Semaphore semaphoreToRelease, boolean Right_left) throws CommandException, InterruptedException {
		
		waitUntilSemaphoreReleased(semaphore);
		alterSwitchPos(switchToLeft, switchXPos, switchYPos);
		
		/* increase the permits available for the semaphore */
		semaphoreToRelease.release();
	}
	
	
	public void run() {
		tsi = TSimInterface.getInstance();
		int xPosition, yPosition;
		final boolean isTrainFromRight = false, isTrainFromLeft = true;

		for(;;) {
			try{

				/* Check the sensor active status. If a train is just passing it or has passed over it. */
				SensorEvent e 			= tsi.getSensor(trainId);
				boolean sensorStatus 	= e.getStatus() == 1;
				
				/* Get the X and Y position of the sensor on the Frame */
				xPosition = e.getXpos();
				yPosition = e.getYpos();

				/* When the sensor is activated. i.e A train is just passing over it */
				if(sensorStatus) {
					switch(xPosition){
						case 1:
							if(isTrainMovingUpDirection) alterSwitchPos(commonMiddleRail.tryAcquire(), 4,9);
							else alterSwitchPos(lowerRail.tryAcquire(), 3,11);
							break;
						case 4:
							if(isTrainMovingUpDirection && yPosition == 13){
								if(!commonLowerRail.tryAcquire()) waitUntilSemaphoreReleased(commonLowerRail);
								tsi.setSwitch(3,11, TSimInterface.SWITCH_RIGHT);
							}
							break;
						case 6:
							if(yPosition == 6) {
								if(!isTrainMovingUpDirection) {
									if(!intersection.tryAcquire())
										waitUntilSemaphoreReleased(intersection);
								}
							}
							else {
								if(isTrainMovingUpDirection){
									if(commonLowerRail.tryAcquire()) alterSwitchPos(yPosition == 11, 3, 11, lowerRail, isTrainFromLeft);
									else waitUntilReSwitched(commonLowerRail, 3, 11, yPosition == 11, lowerRail, isTrainFromLeft);
								}
							}	 
							break;
						case 7:
							if(!isTrainMovingUpDirection && (yPosition >7)) {
								if(commonLowerRail.tryAcquire()) alterSwitchPos(yPosition == 9, 4, 9, commonMiddleRail, isTrainFromLeft);
								else waitUntilReSwitched(commonLowerRail, 4, 9, yPosition == 9, commonMiddleRail, isTrainFromLeft);
							}
							break;
						case 9:
							if(!isTrainMovingUpDirection && yPosition == 5) 
								if(!intersection.tryAcquire()) waitUntilSemaphoreReleased(intersection);
							break;
						case 10:
							if(isTrainMovingUpDirection && !intersection.tryAcquire()) waitUntilSemaphoreReleased(intersection);
							break;
						case 11:
							if(isTrainMovingUpDirection && !intersection.tryAcquire()) waitUntilSemaphoreReleased(intersection);
							break;
						case 12:
							if(isTrainMovingUpDirection) {
								if(commonUpperRail.tryAcquire()) alterSwitchPos(yPosition == 10, 15, 9, commonMiddleRail, isTrainFromRight);
								else waitUntilReSwitched(commonUpperRail, 15, 9, yPosition == 10, commonMiddleRail, isTrainFromRight);
							}
							break;
						case 14:
							if(yPosition != 9) {
								if(!isTrainMovingUpDirection) {
									if(commonUpperRail.tryAcquire()) alterSwitchPos(yPosition == 8, 17, 7, upperRail, isTrainFromRight);
									else waitUntilReSwitched(commonUpperRail, 17,7, yPosition == 8, upperRail, isTrainFromRight);
								}
							}
							break;
						case 15: 
							if(isTrainMovingUpDirection && (yPosition == 3 || yPosition == 5)) endStation();
							else if(!isTrainMovingUpDirection && ((yPosition == 11) || yPosition == 13)) endStation();
							break;
						case 19:
							if(isTrainMovingUpDirection) alterSwitchPos(!upperRail.tryAcquire(), 17,7);							
							else alterSwitchPos(!commonMiddleRail.tryAcquire(), 15,9);
							break;
					}
				}
				else {
					switch(xPosition) {
						case 3:
							if(!isTrainMovingUpDirection && yPosition == 12) commonLowerRail.release();
							break;
						case 4: 
							if(isTrainMovingUpDirection && yPosition == 10) commonLowerRail.release();
							else if(!isTrainMovingUpDirection && yPosition == 11) commonLowerRail.release();
							break;
						case 5:
						 	if(isTrainMovingUpDirection && yPosition == 9) commonLowerRail.release();
						 	break;
						 case 7:
						 	if(isTrainMovingUpDirection && yPosition == 7) intersection.release();
						 	break;
						 case 8:
						 	if(isTrainMovingUpDirection && yPosition == 6) intersection.release();
						 	else if(!isTrainMovingUpDirection && yPosition == 8) intersection.release();
						 	break;
						 case 9:
						 	if(!isTrainMovingUpDirection && yPosition == 7) intersection.release();
						 	break;
						 case 14:
						 	if(!isTrainMovingUpDirection && yPosition == 9) commonUpperRail.release();
						 	break;
						 case 15:
 						 	if(!isTrainMovingUpDirection && yPosition == 10) commonUpperRail.release();
 						 	break;
						 case 16:
						 	if(isTrainMovingUpDirection && yPosition == 7) commonUpperRail.release();
						 	break;
						 case 17:
						 	if(isTrainMovingUpDirection && yPosition == 8) commonUpperRail.release();
						 	break;
					}
				}
			}catch(CommandException | InterruptedException e) { System.out.println(e.getMessage()); }
		}
	}
}	