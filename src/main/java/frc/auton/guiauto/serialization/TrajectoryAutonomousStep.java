package frc.auton.guiauto.serialization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.Trajectory.State;
import frc.auton.TemplateAuto;
import frc.auton.guiauto.serialization.command.SendableScript;
import frc.subsystem.Drive;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TrajectoryAutonomousStep extends AbstractAutonomousStep {
    private final @NotNull Trajectory trajectory;
    private final List<TimedRotation> rotations;

    @JsonCreator
    public TrajectoryAutonomousStep(@JsonProperty(required = true, value = "states") List<State> states,
                                    @JsonProperty(required = true, value = "rotations") List<TimedRotation> rotations) {
        this.trajectory = new Trajectory(states);
        this.rotations = rotations;
    }

    public @NotNull Trajectory getTrajectory() {
        return trajectory;
    }

    public List<TimedRotation> getRotations() {
        return rotations;
    }

    @Override
    public void execute(@NotNull TemplateAuto templateAuto,
                        @NotNull List<SendableScript> scriptsToExecuteByTime,
                        @NotNull List<SendableScript> scriptsToExecuteByPercent) {
        //Sort the lists to make sure they are sorted by time
        Collections.sort(scriptsToExecuteByTime);
        Collections.sort(scriptsToExecuteByPercent);
        //This part of the code will likely need to be customized. This takes the trajectory (output TrajectoryGenerator
        // .generateTrajectory()) and sends it to our drive class to be executed.
        //If this is not how your autonomous code work you can change the implementation to fit your needs.
        //You just need to ensure that this thread will be blocked until the path is finished being driven.
        if (!templateAuto.isDead()) { //Check that the auto is not dead
            Drive.getInstance().setAutoRotation(rotations.get(0).rotation);
            Drive.getInstance().setAutoPath(trajectory); //Send the auto to our drive class to be executed
            int rotationIndex = 1; // Start at the second rotation (the first is the starting rotation)
            while (!Drive.getInstance().isFinished()) {// Wait till the auto is done
                if (templateAuto.isDead()) {
                    return;  // exit early if it is killed
                } else {
                    if (rotationIndex < rotations.size() &&
                            Drive.getInstance().getAutoElapsedTime() > rotations.get(rotationIndex).time) {
                        // We've passed the time for the next rotation
                        Drive.getInstance().setAutoRotation(rotations.get(rotationIndex).rotation); //Set the rotation
                        rotationIndex++; // Increment the rotation index
                    }

                    if (!scriptsToExecuteByTime.isEmpty() &&
                            scriptsToExecuteByTime.get(0).getDelay() <= Drive.getInstance().getAutoElapsedTime()) {
                        // We have a script to execute, and it is time to execute it
                        scriptsToExecuteByTime.get(0).execute(templateAuto);
                        scriptsToExecuteByTime.remove(0);
                    }

                    if (!scriptsToExecuteByPercent.isEmpty() && scriptsToExecuteByPercent.get(0).getDelay() <=
                            (Drive.getInstance().getAutoElapsedTime() / trajectory.getTotalTimeSeconds())) {
                        // We have a script to execute, and it is time to execute it
                        scriptsToExecuteByPercent.get(0).execute(templateAuto);
                        scriptsToExecuteByPercent.remove(0);
                    }

                    try {
                        //noinspection BusyWait
                        Thread.sleep(1);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            Drive.getInstance().stopMovement();

            //Execute any remain scripts
            for (SendableScript sendableScript : scriptsToExecuteByTime) {
                sendableScript.execute(templateAuto);
            }
            for (SendableScript sendableScript : scriptsToExecuteByPercent) {
                sendableScript.execute(templateAuto);
            }

            if (rotationIndex < rotations.size()) {
                Drive.getInstance().setAutoRotation(rotations.get(rotations.size() - 1).rotation);
                Drive.getInstance().setDriveState(Drive.DriveState.RAMSETE);
            }
            scriptsToExecuteByTime.clear();
            scriptsToExecuteByPercent.clear();
        }
        Drive.getInstance().stopMovement();
    }
}