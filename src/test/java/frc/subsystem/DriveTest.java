package frc.subsystem;

import com.revrobotics.REVPhysicsSim;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.system.plant.DCMotor;
import frc.robot.Constants;
import frc.utility.Timer;
import frc.utility.controllers.LazyCANSparkMax;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class DriveTest {
    public static final double DELTA = 1.0E-2;
    Drive drive;
    Random random = new Random();

    REVPhysicsSim sim;

    private double currentMaxVelocityChange = 0;

    @BeforeEach
    void setUp() {
        drive = Drive.getInstance();
        drive.kill();
        sim = new REVPhysicsSim();
        for (int i = 0; i < drive.swerveMotors.length; i++) {
            sim.addSparkMax(drive.swerveDriveMotors[i], DCMotor.getNEO(1));
            sim.addSparkMax(drive.swerveMotors[i], DCMotor.getNeo550(1));
        }
        sim.run();
    }

    @AfterEach
    void tearDown() throws Exception {
        drive.close();
    }

    @Test
    void testFeedforward() {
        for (int i = 0; i < 100; i++) {
            double randomX = random.nextDouble() * 4 - 2;
            double randomY = random.nextDouble() * 4 - 2;
            double expectedSpeed = Math.sqrt(randomX * randomX + randomY * randomY);
            drive.swerveDrive(new ChassisSpeeds(randomX, randomY, 0.0));
            for (int j = 0; j < drive.swerveMotors.length; j++) {
                assertEquals(Math.abs(Constants.DRIVE_FEEDFORWARD[j].calculate(expectedSpeed)),
                        Math.abs(drive.swerveDriveMotors[j].getSetVoltage()), DELTA);
            }
        }
    }

    @Test
    void testFallbackAim() {
        for (int i = 0; i < 10; i++) {
            double randomX = random.nextDouble() * 10 - 5;
            double randomY = random.nextDouble() * 10 - 5;
            drive.fallbackAim(new Translation2d(randomX, randomY));
            double expectedAngle = Math.atan2(Constants.GOAL_POSITION.getY() - randomY, Constants.GOAL_POSITION.getX() - randomX);

            assertEquals(expectedAngle, drive.wantedHeading.getRadians(), DELTA);
        }
    }

    @Test
    void testStopMovement() throws NoSuchFieldException, IllegalAccessException {
        Field currentRobotState = Drive.class.getDeclaredField("currentRobotState");
        currentRobotState.setAccessible(true);
        currentRobotState.set(drive, new ChassisSpeeds(0, 0, 0));

        Timer.setTime(50);
        drive.swerveDrive(new ChassisSpeeds(1, 1, 0.0));
        for (LazyCANSparkMax swerveDriveMotor : drive.swerveDriveMotors) {
            assertNotEquals(0.0, swerveDriveMotor.getSetVoltage());
        }
        Timer.setTime(50.05);
        drive.stopMovement();
        for (int i = 0; i < drive.swerveMotors.length; i++) {
            assertEquals(0.0, drive.swerveDriveMotors[i].getSetVoltage(), DELTA);
        }
    }

    @Test
    void testMaxAllowedVelocityChange() throws NoSuchFieldException, IllegalAccessException {
        double testPeriod = 0;
        // Reflection used to set currentRobotState with no velocity
        Field currentRobotState = Drive.class.getDeclaredField("currentRobotState");
        currentRobotState.setAccessible(true);
        currentRobotState.set(drive, new ChassisSpeeds(0, 0, 0));

        // Makes lastLoopTime accessable
        Field lastLoopTime = Drive.class.getDeclaredField("lastLoopTime");
        lastLoopTime.setAccessible(true);

        for(int i = 0; i < 10; i++) {
            testPeriod = Math.random() * 0.15;
            // Sets last loop time to 50 seconds in a match
            lastLoopTime.set(drive, 50);

            // Sets current time to 50 ms beyond last loop
            Timer.setTime(50.0 + testPeriod);
            currentMaxVelocityChange = drive.getMaxAllowedVelocityChange();
            assertEquals(Constants.MAX_ACCELERATION * testPeriod, currentMaxVelocityChange, DELTA);
        }
    }

    @Test
    void testMaxAllowedVelocityChangeAboveMaxTimeLimit() throws NoSuchFieldException, IllegalAccessException {
        // Reflection used to set currentRobotState with no velocity
        Field currentRobotState = Drive.class.getDeclaredField("currentRobotState");
        currentRobotState.setAccessible(true);
        currentRobotState.set(drive, new ChassisSpeeds(0, 0, 0));

        // Makes lastLoopTime accessible
        Field lastLoopTime = Drive.class.getDeclaredField("lastLoopTime");
        lastLoopTime.setAccessible(true);

        for(int i = 0; i < 10; i++) {
            double testPeriod = Math.random() + 0.15;
            // Sets last loop time to 50 seconds in a match
            lastLoopTime.set(drive, 50);

            // Sets current time to 50 ms beyond last loop
            Timer.setTime(50.0 + testPeriod);
            currentMaxVelocityChange = drive.getMaxAllowedVelocityChange();
            assertEquals(Constants.MAX_ACCELERATION * 0.05, currentMaxVelocityChange, DELTA);
        }
    }

    @Test
    void testLimitAcceleration() throws Exception {
        ChassisSpeeds commandedVelocity = null;
        ChassisSpeeds limitedVelocity = null;
        double velocity = 0;
        double actualVelocity = 0;

        Field currentRobotState = Drive.class.getDeclaredField("currentRobotState");
        currentRobotState.setAccessible(true);

        Field lastLoopTime = Drive.class.getDeclaredField("lastLoopTime");
        lastLoopTime.setAccessible(true);

        // Set time to 50 seconds

        lastLoopTime.set(drive, 50);

        Timer.setTime(50.1);
        // Set Current Velocity
        ChassisSpeeds currentRobotStateChassisSpeed = new ChassisSpeeds(20, 10, 3);
        currentRobotState.set(drive, currentRobotStateChassisSpeed);

        // Set Commanded Velocity
        commandedVelocity = new ChassisSpeeds(40, 20, 6);
        // call limitAcceleration
        limitedVelocity = drive.limitAcceleration(commandedVelocity);
        // Gets mag of limitedVelocity
        velocity = Math.sqrt((limitedVelocity.vxMetersPerSecond * limitedVelocity.vxMetersPerSecond)
                    + (limitedVelocity.vyMetersPerSecond * limitedVelocity.vyMetersPerSecond));

        // Gets mag of actualVelocity
        actualVelocity = Math.sqrt(
                (currentRobotStateChassisSpeed.vxMetersPerSecond * currentRobotStateChassisSpeed.vxMetersPerSecond)
                        + (currentRobotStateChassisSpeed.vyMetersPerSecond * currentRobotStateChassisSpeed.vyMetersPerSecond));
        // Check if acceleration is at maximum
        assertEquals(Constants.MAX_ACCELERATION * 0.1, Math.abs(velocity - actualVelocity), DELTA);
        assertEquals(limitedVelocity.omegaRadiansPerSecond, 6, DELTA);

    }

    @Test
    void testLimitAcceleration2() throws Exception {

        Field currentRobotState = Drive.class.getDeclaredField("currentRobotState");
        currentRobotState.setAccessible(true);

        Field lastLoopTime = Drive.class.getDeclaredField("lastLoopTime");
        lastLoopTime.setAccessible(true);

        // Set time to 50 seconds

        lastLoopTime.set(drive, 50);

        Timer.setTime(50.05);
        // Set Current Velocity
        ChassisSpeeds currentRobotStateChassisSpeed = new ChassisSpeeds(23.22, 11.05, -3.1);
        currentRobotState.set(drive, currentRobotStateChassisSpeed);

        // Set Commanded Velocity
        ChassisSpeeds commandedVelocity = new ChassisSpeeds(-62.3, -23.44, 2.05);
        // call limitAcceleration
        ChassisSpeeds limitedVelocity = drive.limitAcceleration(commandedVelocity);
        // Gets mag of limitedVelocity
        double velocity = Math.sqrt((limitedVelocity.vxMetersPerSecond * limitedVelocity.vxMetersPerSecond)
                + (limitedVelocity.vyMetersPerSecond * limitedVelocity.vyMetersPerSecond));

        // Gets mag of actualVelocity
        double actualVelocity = Math.sqrt(
                (currentRobotStateChassisSpeed.vxMetersPerSecond * currentRobotStateChassisSpeed.vxMetersPerSecond)
                        + (currentRobotStateChassisSpeed.vyMetersPerSecond * currentRobotStateChassisSpeed.vyMetersPerSecond));
        // Check if acceleration is at maximum
        assertEquals(-Constants.MAX_ACCELERATION * 0.05, velocity - actualVelocity, DELTA);
        assertEquals(2.05, limitedVelocity.omegaRadiansPerSecond, DELTA);
    }

    @Test
    void testLimitAcceleration3() throws Exception {

        Field currentRobotState = Drive.class.getDeclaredField("currentRobotState");
        currentRobotState.setAccessible(true);

        Field lastLoopTime = Drive.class.getDeclaredField("lastLoopTime");
        lastLoopTime.setAccessible(true);

        // Set time to 50 seconds

        lastLoopTime.set(drive, 50);

        Timer.setTime(50.2);
        // Set Current Velocity
        ChassisSpeeds currentRobotStateChassisSpeed = new ChassisSpeeds(-20, -10, 15);
        currentRobotState.set(drive, currentRobotStateChassisSpeed);

        // Set Commanded Velocity
        ChassisSpeeds commandedVelocity = new ChassisSpeeds(-14.4, 20, -15.5);
        // call limitAcceleration
        ChassisSpeeds limitedVelocity = drive.limitAcceleration(commandedVelocity);
        // Gets mag of limitedVelocity
        double velocity = Math.hypot(limitedVelocity.vxMetersPerSecond - currentRobotStateChassisSpeed.vxMetersPerSecond,
                limitedVelocity.vyMetersPerSecond - currentRobotStateChassisSpeed.vyMetersPerSecond);

        // Check if acceleration is at maximum
        assertEquals(Constants.MAX_ACCELERATION * 0.05, velocity, DELTA);
        assertEquals(-15.5, limitedVelocity.omegaRadiansPerSecond, DELTA);
    }
}
