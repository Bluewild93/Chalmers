import TSim.CommandException;

public class Lab1 {

	public Lab1(Integer speed1, Integer speed2) {

		try {
			new Thread(new MovingTrain(1, speed1, false)).start();
			new Thread(new MovingTrain(2, speed2, true)).start();
		} catch (CommandException e) {	e.getMessage();}
	}
}
