// Copyright 2019 FRC Team 3476 Code Orange

package frc.subsystem;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.SupplyCurrentLimitConfiguration;
import com.ctre.phoenix.sensors.CANCoder;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants;
import frc.utility.ControllerDriveInputs;
import frc.utility.Timer;
import frc.utility.controllers.LazyTalonFX;
import frc.utility.wpimodified.HolonomicDriveController;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


public final class Drive extends AbstractSubsystem {

    public enum DriveState {
        TELEOP, TURN, HOLD, DONE, RAMSETE
    }

    public boolean useRelativeEncoderPosition = false;

    private static @NotNull Drive instance = new Drive();

    public static @NotNull Drive getInstance() {
        return instance;
    }

    private final @NotNull PIDController turnPID;
    private DriveState driveState;
    Rotation2d wantedHeading = new Rotation2d();
    boolean rotateAuto = false;

    public boolean useFieldRelative = true;

    private boolean isAiming = false;

    private double lastLoopTime = 0;

    private final SwerveDriveKinematics swerveKinematics = new SwerveDriveKinematics(Constants.SWERVE_LEFT_FRONT_LOCATION,
            Constants.SWERVE_LEFT_BACK_LOCATION, Constants.SWERVE_RIGHT_FRONT_LOCATION, Constants.SWERVE_RIGHT_BACK_LOCATION);
    /**
     * Motors that turn the wheels around. Uses Falcon500s
     */
    final LazyTalonFX[] swerveMotors = new LazyTalonFX[4];

    /**
     * Motors that are driving the robot around and causing it to move
     */
    final LazyTalonFX[] swerveDriveMotors = new LazyTalonFX[4];

    /**
     * Absolute Encoders for the motors that turn the wheel
     */

    final CANCoder[] swerveCanCoders = new CANCoder[4];

    private Drive() {
        super(Constants.DRIVE_PERIOD);

        final LazyTalonFX leftFrontTalon, leftBackTalon, rightFrontTalon, rightBackTalon;
        final CANCoder leftFrontCanCoder, leftBackCanCoder, rightFrontCanCoder, rightBackCanCoder;
        final LazyTalonFX leftFrontTalonSwerve, leftBackTalonSwerve, rightFrontTalonSwerve, rightBackTalonSwerve;
        // Swerve Drive Motors
        leftFrontTalon = new LazyTalonFX(Constants.DRIVE_LEFT_FRONT_ID);
        leftBackTalon = new LazyTalonFX(Constants.DRIVE_LEFT_BACK_ID);
        rightFrontTalon = new LazyTalonFX(Constants.DRIVE_RIGHT_FRONT_ID);
        rightBackTalon = new LazyTalonFX(Constants.DRIVE_RIGHT_BACK_ID);

        leftFrontTalon.setInverted(false);
        rightFrontTalon.setInverted(false);
        leftBackTalon.setInverted(false);
        rightBackTalon.setInverted(false);

        leftFrontTalonSwerve = new LazyTalonFX(Constants.DRIVE_LEFT_FRONT_SWERVE_ID);
        leftBackTalonSwerve = new LazyTalonFX(Constants.DRIVE_LEFT_BACK_SWERVE_ID);
        rightFrontTalonSwerve = new LazyTalonFX(Constants.DRIVE_RIGHT_FRONT_SWERVE_ID);
        rightBackTalonSwerve = new LazyTalonFX(Constants.DRIVE_RIGHT_BACK_SWERVE_ID);

        leftFrontCanCoder = new CANCoder(Constants.CAN_LEFT_FRONT_ID);
        leftBackCanCoder = new CANCoder(Constants.CAN_LEFT_BACK_ID);
        rightFrontCanCoder = new CANCoder(Constants.CAN_RIGHT_FRONT_ID);
        rightBackCanCoder = new CANCoder(Constants.CAN_RIGHT_BACK_ID);

        swerveMotors[0] = leftFrontTalonSwerve;
        swerveMotors[1] = leftBackTalonSwerve;
        swerveMotors[2] = rightFrontTalonSwerve;
        swerveMotors[3] = rightBackTalonSwerve;

        swerveDriveMotors[0] = leftFrontTalon;
        swerveDriveMotors[1] = leftBackTalon;
        swerveDriveMotors[2] = rightFrontTalon;
        swerveDriveMotors[3] = rightBackTalon;

        swerveCanCoders[0] = leftFrontCanCoder;
        swerveCanCoders[1] = leftBackCanCoder;
        swerveCanCoders[2] = rightFrontCanCoder;
        swerveCanCoders[3] = rightBackCanCoder;

        for (int i = 0; i < 4; i++) {
            // Sets swerveMotors PID
            swerveMotors[i].config_kP(0, Constants.SWERVE_DRIVE_P, Constants.SWERVE_MOTOR_PID_TIMEOUT_MS);
            swerveMotors[i].config_kD(0, Constants.SWERVE_DRIVE_D, Constants.SWERVE_MOTOR_PID_TIMEOUT_MS);
            swerveMotors[i].config_kI(0, Constants.SWERVE_DRIVE_I, Constants.SWERVE_MOTOR_PID_TIMEOUT_MS);
            swerveMotors[i].config_kF(0, Constants.SWERVE_DRIVE_F, Constants.SWERVE_MOTOR_PID_TIMEOUT_MS);
            swerveMotors[i].config_IntegralZone(0, Constants.SWERVE_DRIVE_INTEGRAL_ZONE);

            // Sets current limits for motors
            swerveMotors[i].configSupplyCurrentLimit(new SupplyCurrentLimitConfiguration(true,
                    Constants.SWERVE_MOTOR_CURRENT_LIMIT, Constants.SWERVE_MOTOR_CURRENT_LIMIT, 0));

            swerveDriveMotors[i].configSupplyCurrentLimit(new SupplyCurrentLimitConfiguration(true,
                    Constants.SWERVE_DRIVE_MOTOR_CURRENT_LIMIT, Constants.SWERVE_DRIVE_MOTOR_CURRENT_LIMIT, 0));

            swerveDriveMotors[i].configVoltageCompSaturation(Constants.SWERVE_DRIVE_VOLTAGE_LIMIT);

            // This makes motors brake when no RPM is set
            swerveDriveMotors[i].setNeutralMode(NeutralMode.Brake);
            swerveMotors[i].setNeutralMode(NeutralMode.Brake);
        }

        configMotors();
        driveState = DriveState.TELEOP;

        turnPID = new PIDController(0.02, 0.01, 0.00, 0.02); //P=1.0 OR 0.8
        turnPID.disableContinuousInput();
        turnPID.setSetpoint(0);
        configBrake();
    }

    // Returns position of swerve motors
    private double getSwervePosition(int motorNum) {
        return swerveMotors[motorNum].getSelectedSensorPosition() / Constants.FALCON_UNIT_CONVERSION_FOR_RELATIVE_ENCODER_POSITION *
                Constants.SWERVE_MOTOR_POSITION_CONVERSION_FACTOR;
    }

    private double getSwerveDrivePosition(int motorNum) {
        return swerveDriveMotors[motorNum].getSelectedSensorPosition() / Constants.FALCON_UNIT_CONVERSION_FOR_RELATIVE_ENCODER_POSITION;
    }

    // Returns velocity in RPM
    private double getSwerveDriveVelocity(int motorNum) {
        return swerveDriveMotors[motorNum].getSelectedSensorVelocity() * Constants.FALCON_UNIT_CONVERSION_FOR_RELATIVE_ENCODER_VELOCITY;
    }

    public @NotNull SwerveDriveKinematics getSwerveDriveKinematics() {
        return swerveKinematics;
    }

    public void setDriveState(DriveState driveState) {
        this.driveState = driveState;
    }

    private void configBrake() {
        //TODO
    }

    private void configCoast() {
        //TODO
    }

    private void configAuto() {

    }

    synchronized public void setTeleop() {
        driveState = DriveState.TELEOP;
    }

    synchronized public SwerveModuleState @NotNull [] getSwerveModuleStates() {
        SwerveModuleState[] swerveModuleState = new SwerveModuleState[4];
        for (int i = 0; i < 4; i++) {
            SwerveModuleState moduleState = new SwerveModuleState(
                    (getSwerveDriveVelocity(i) / 60d) * Constants.SWERVE_METER_PER_ROTATION,
                    Rotation2d.fromDegrees(getWheelRotation(i)));
            swerveModuleState[i] = moduleState;
        }
        return swerveModuleState;
    }

    public void startHold() {
        configBrake();
        driveState = DriveState.HOLD;
    }

    public void endHold() {
        driveState = DriveState.TELEOP;
    }

    public void doHold() {
        setSwerveModuleStates(Constants.HOLD_MODULE_STATES);
    }

    public void swerveDrive(@NotNull ControllerDriveInputs inputs) {
        synchronized (this) {
            driveState = DriveState.TELEOP;
        }

        ChassisSpeeds chassisSpeeds = new ChassisSpeeds(Constants.DRIVE_HIGH_SPEED_M * inputs.getX(),
                Constants.DRIVE_HIGH_SPEED_M * inputs.getY(),
                inputs.getRotation() * 2);
        swerveDrive(chassisSpeeds);
    }

    public void swerveDriveFieldRelative(@NotNull ControllerDriveInputs inputs) {
        synchronized (this) {
            driveState = DriveState.TELEOP;
        }

        ChassisSpeeds chassisSpeeds = ChassisSpeeds.fromFieldRelativeSpeeds(Constants.DRIVE_HIGH_SPEED_M * inputs.getX(),
                Constants.DRIVE_HIGH_SPEED_M * inputs.getY(),
                inputs.getRotation() * 6,
                RobotTracker.getInstance().getGyroAngle());

        if (chassisSpeeds.vxMetersPerSecond == 0 && chassisSpeeds.vyMetersPerSecond == 0 && chassisSpeeds.omegaRadiansPerSecond == 0) {
            // We're not moving, so put the robot in a hold pose to prevent us from moving when pushed
            doHold();
        } else {
            swerveDrive(chassisSpeeds);
        }
    }

    double doubleMod(double x, double y) {
        // x mod y behaving the same way as Math.floorMod but with doubles
        return (x - Math.floor(x / y) * y);
    }

    public void swerveDrive(ChassisSpeeds chassisSpeeds) {
        swerveDrive(chassisSpeeds, 0);
    }

    public void swerveDrive(ChassisSpeeds chassisSpeeds, double acceleration) {

        // Limits max velocity change
        chassisSpeeds = limitAcceleration(chassisSpeeds);

        SmartDashboard.putNumber("Drive Command X Velocity", chassisSpeeds.vxMetersPerSecond);
        SmartDashboard.putNumber("Drive Command Y Velocity", chassisSpeeds.vyMetersPerSecond);
        SmartDashboard.putNumber("Drive Command Rotation", chassisSpeeds.omegaRadiansPerSecond);

        SwerveModuleState[] moduleStates = swerveKinematics.toSwerveModuleStates(chassisSpeeds);

        boolean rotate = chassisSpeeds.vxMetersPerSecond != 0 ||
                chassisSpeeds.vyMetersPerSecond != 0 ||
                chassisSpeeds.omegaRadiansPerSecond != 0;

        SwerveDriveKinematics.desaturateWheelSpeeds(moduleStates, Constants.DRIVE_HIGH_SPEED_M);
        setSwerveModuleStates(moduleStates, rotate, acceleration);
    }

    public void setSwerveModuleStates(SwerveModuleState[] states) {
        setSwerveModuleStates(states, true, 0);
    }

    public void setSwerveModuleStates(SwerveModuleState[] moduleStates, boolean rotate, double acceleration) {
        for (int i = 0; i < 4; i++) {
            //            SwerveModuleState targetState = SwerveModuleState.optimize(moduleStates[i],
            //                    Rotation2d.fromDegrees(getWheelRotation(i)));
            // TODO: flip the acceleration if we flip the module
            SwerveModuleState targetState = moduleStates[i];
            double targetAngle = targetState.angle.getDegrees();
            double currentAngle = getWheelRotation(i); //swerveEncoders[i].getPosition();

            double angleDiff = doubleMod((targetAngle - currentAngle) + 180, 360) - 180;

            if (Math.abs(angleDiff) < 5 || !rotate) {
                swerveMotors[i].set(ControlMode.Velocity, 0);
            } else {
                swerveMotors[i].set(ControlMode.Position, getSwervePosition(i) + angleDiff);
            }

            double speedModifier = 1; //= 1 - (OrangeUtility.coercedNormalize(Math.abs(angleDiff), 5, 180, 0, 180) / 180);

            setMotorSpeed(i, targetState.speedMetersPerSecond * speedModifier, acceleration);

            SmartDashboard.putNumber("Swerve Motor " + i + " Speed Modifier", speedModifier);
            SmartDashboard.putNumber("Swerve Motor " + i + " Target Position", getSwervePosition(i) + angleDiff);
            SmartDashboard.putNumber("Swerve Motor " + i + " Error", angleDiff);
        }
    }


    /**
     * Puts limit on desired velocity, so it can be achieved with a reasonable acceleration
     * <p>
     * Converts ChassisSpeeds to Translation2d Computes difference between desired and actual velocities Converts from cartesian
     * to polar coordinate system Checks if velocity change exceeds MAX limit Gets limited velocity vector difference in cartesian
     * coordinate system Computes limited velocity Converts to format compatible with serveDrive
     *
     * @param commandedVelocity Desired velocity
     * @return Velocity that can be achieved within the iteration period
     */
    @NotNull ChassisSpeeds limitAcceleration(@NotNull ChassisSpeeds commandedVelocity) {

        double maxVelocityChange = getMaxAllowedVelocityChange();

        // Sets the last call of the method to the current time
        lastLoopTime = Timer.getFPGATimestamp();

        ChassisSpeeds actualVelocity = RobotTracker.getInstance().getLatencyCompedChassisSpeeds();

        // Converts ChassisSpeeds to Translation2d
        Translation2d actualVelocityVector = new Translation2d(actualVelocity.vxMetersPerSecond,
                actualVelocity.vyMetersPerSecond);
        Translation2d commandedVelocityVector = new Translation2d(commandedVelocity.vxMetersPerSecond,
                commandedVelocity.vyMetersPerSecond);

        // Computing difference between desired and actual velocities
        Translation2d velocityVectorChange = commandedVelocityVector.minus(actualVelocityVector);

        // Convert from cartesian to polar coordinate system
        double velocityChangeMagnitudeSquared = (velocityVectorChange.getX() * velocityVectorChange.getX()) +
                (velocityVectorChange.getY() * velocityVectorChange.getY());
        double velocityDiffAngle = Math.atan2(velocityVectorChange.getY(), velocityVectorChange.getX()); // remove

        ChassisSpeeds limitedVelocity = commandedVelocity;

        // Check if velocity change exceeds MAX limit
        if (velocityChangeMagnitudeSquared > maxVelocityChange * maxVelocityChange) {

            // Get limited velocity vector difference in cartesian coordinate system
            Translation2d limitedVelocityVectorChange =
                    new Translation2d(Math.cos(velocityDiffAngle) * maxVelocityChange,
                            Math.sin(velocityDiffAngle) * maxVelocityChange);

            // Compute limited velocity
            Translation2d limitedVelocityVector = limitedVelocityVectorChange.plus(actualVelocityVector);

            // Convert to format compatible with serveDrive
            limitedVelocity = new ChassisSpeeds(limitedVelocityVector.getX(), limitedVelocityVector.getY(),
                    commandedVelocity.omegaRadiansPerSecond);

        }

        return limitedVelocity;
    }

    /**
     * Gets the MAX change in velocity that can occur over the iteration period
     *
     * @return Maximum value that the velocity can change within the iteration period
     */
    double getMaxAllowedVelocityChange() {
        // Gets the iteration period by subtracting the current time with the last time accelLimit was called
        // If iteration period is greater than allowed amount, iteration period = 50 ms
        double accelLimitPeriod;
        if ((Timer.getFPGATimestamp() - lastLoopTime) > 0.150) {
            accelLimitPeriod = 0.050;
        } else {
            accelLimitPeriod = (Timer.getFPGATimestamp() - lastLoopTime);
        }

        // Multiplies by MAX_ACCELERATION to find the velocity over that period
        return Constants.MAX_ACCELERATION * (accelLimitPeriod);
    }


    /**
     * Sets the motor voltage
     *
     * @param module       The module to set the voltage on
     * @param velocity     The target velocity
     * @param acceleration The acceleration to use
     */
    public void setMotorSpeed(int module, double velocity, double acceleration) {
        double ffv = Constants.DRIVE_FEEDFORWARD[module].calculate(velocity, acceleration);
        // Converts ffv voltage to percent output and sets it to motor
        swerveDriveMotors[module].set(ControlMode.PercentOutput, ffv / Constants.SWERVE_DRIVE_VOLTAGE_LIMIT);
        SmartDashboard.putNumber("Out Volts " + module, ffv);
        //swerveDriveMotors[module].setVoltage(10 * velocity/Constants.SWERVE_METER_PER_ROTATION);
    }

    private void configMotors() {
        //TODO

    }

    /**
     * {@link Drive#getSpeedSquared()} is faster if you don't need the square root
     *
     * @return The robot speed
     */
    public double getSpeed() {
        ChassisSpeeds robotState = RobotTracker.getInstance().getLatencyCompedChassisSpeeds();
        return Math.sqrt(Math.pow(robotState.vxMetersPerSecond, 2) + Math.pow(robotState.vyMetersPerSecond, 2));
    }

    /**
     * Should be a bit faster than {@link Drive#getSpeed()}
     *
     * @return The robot speed squared
     */
    public double getSpeedSquared() {
        ChassisSpeeds robotState = RobotTracker.getInstance().getLatencyCompedChassisSpeeds();
        return Math.pow(robotState.vxMetersPerSecond, 2) + Math.pow(robotState.vyMetersPerSecond, 2);
    }

    double autoStartTime;
    private final HolonomicDriveController controller = new HolonomicDriveController(
            new PIDController(1.5, 0, 0),
            new PIDController(1.5, 0, 0),
            new ProfiledPIDController(5, 0, 0, new TrapezoidProfile.Constraints(6, 5)));

    {
        controller.setTolerance(new Pose2d(0.5, 0.5, Rotation2d.fromDegrees(10))); //TODO: Tune
    }


    public synchronized void setAutoPath(Trajectory trajectory) {
        driveState = DriveState.RAMSETE;
        this.currentAutoTrajectory = trajectory;
        autoStartTime = Timer.getFPGATimestamp();
        configAuto();
        configCoast();
    }

    Trajectory currentAutoTrajectory;
    Rotation2d autoTargetHeading;

    private void updateRamsete() {
        Trajectory.State goal = currentAutoTrajectory.sample(Timer.getFPGATimestamp() - autoStartTime);
        Trajectory.State trackerPose = currentAutoTrajectory.sample(
                Timer.getFPGATimestamp() - autoStartTime - Constants.SPARK_VELOCITY_MEASUREMENT_LATENCY);

        ChassisSpeeds adjustedSpeeds = controller.calculate(RobotTracker.getInstance().getLastEstimatedPoseMeters(), goal,
                trackerPose, autoTargetHeading);
        swerveDrive(adjustedSpeeds, goal.accelerationMetersPerSecondSq);
        if (controller.atReference() && (Timer.getFPGATimestamp() - autoStartTime) >= currentAutoTrajectory.getTotalTimeSeconds()) {
            driveState = DriveState.DONE;
            stopMovement();
        }
    }

    public synchronized void setAutoRotation(@NotNull Rotation2d rotation) {
        autoTargetHeading = rotation;
        System.out.println("new rotation" + rotation.getDegrees());
    }

    public double getAutoElapsedTime() {
        return Timer.getFPGATimestamp() - autoStartTime;
    }

    @Override
    public void update() {
        DriveState snapDriveState;
        synchronized (this) {
            snapDriveState = driveState;
        }

        checkGyroConnection();

        switch (snapDriveState) {
            case TURN:
                updateTurn();
                break;
            case HOLD:
                doHold();
                break;
            case RAMSETE:
                updateRamsete();
                break;
        }

    }

    synchronized public boolean isAiming() {
        return isAiming;
    }

    public void setRotation(Rotation2d angle) {
        synchronized (this) {
            wantedHeading = angle;
            driveState = DriveState.TURN;
            rotateAuto = true;
            isAiming = !isTurningDone();
            configBrake();
        }
    }


    public synchronized boolean isTurningDone() {
        double error = wantedHeading.rotateBy(RobotTracker.getInstance().getGyroAngle()).getDegrees();
        double curSpeed = Math.toDegrees(RobotTracker.getInstance().getLatencyCompedChassisSpeeds().omegaRadiansPerSecond);
        return (Math.abs(error) < Constants.MAX_TURN_ERROR) && curSpeed < Constants.MAX_PID_STOP_SPEED;
    }

    double turnMinSpeed = 0;

    /**
     * Default method when the x and y velocity and the target heading are not passed
     */
    private void updateTurn() {
        updateTurn(0, 0, wantedHeading);
    }

    /**
     * This method takes in x and y velocity as well as the target heading to calculate how much the robot needs to turn in order
     * to face a target
     * <p>
     * xVelocity and yVelocity are in m/s
     *
     * @param xVelocity
     * @param yVelocity
     * @param targetHeading
     */
    private void updateTurn(double xVelocity, double yVelocity, @NotNull Rotation2d targetHeading) {
        double error = targetHeading.rotateBy(RobotTracker.getInstance().getGyroAngle()).getDegrees();
        double pidDeltaSpeed = turnPID.calculate(error);
        double curSpeed = Math.toDegrees(RobotTracker.getInstance().getLatencyCompedChassisSpeeds().omegaRadiansPerSecond);
        double deltaSpeed = Math.copySign(Math.max(Math.abs(pidDeltaSpeed), turnMinSpeed), pidDeltaSpeed);


        if ((Math.abs(error) < Constants.MAX_TURN_ERROR) && curSpeed < Constants.MAX_PID_STOP_SPEED) {
            swerveDrive(new ChassisSpeeds(xVelocity, yVelocity, Math.toRadians(0)));
            isAiming = false;

            if (rotateAuto) {
                synchronized (this) {
                    configBrake();
                    driveState = DriveState.DONE;
                }
            }

        } else {
            isAiming = true;
            swerveDrive(new ChassisSpeeds(0, 0, Math.toRadians(deltaSpeed)));

            if (curSpeed < 0.5) {
                //Updates every 20ms
                turnMinSpeed = Math.min(turnMinSpeed + 0.1, 6);
            } else {
                turnMinSpeed = 2;
            }
            SmartDashboard.putNumber("Turn Error", error);
            SmartDashboard.putNumber("Turn Actual Speed", curSpeed);
            SmartDashboard.putNumber("Turn PID Command", pidDeltaSpeed);
            SmartDashboard.putNumber("Turn Speed Command", deltaSpeed);
            SmartDashboard.putNumber("Turn Min Speed", turnMinSpeed);
        }
    }

    synchronized public void stopMovement() {
        swerveDrive(new ChassisSpeeds(0, 0, 0));
    }

    synchronized public boolean isFinished() {
        return driveState == DriveState.DONE || driveState == DriveState.TELEOP;
    }

    @Override
    public void selfTest() {

    }

    @Override
    public void logData() {
        for (int i = 0; i < 4; i++) {
            double relPos = getSwervePosition(i) % 360;
            if (relPos < 0) relPos += 360;
            logData("Swerve Motor " + i + " Relative Position", relPos);
            logData("Swerve Motor " + i + " Absolute Position", getWheelRotation(i));
            logData("Drive Motor " + i + " Velocity", getSwerveDriveVelocity(i) / 60.0d);
            logData("Drive Motor " + i + " Current", swerveDriveMotors[i].getStatorCurrent());
            logData("Swerve Motor " + i + " Current", swerveMotors[i].getStatorCurrent());
        }
    }


    /**
     * Returns the angle/position of the requested encoder module
     *
     * @param moduleNumber the module to set
     * @return angle in degrees of the module
     */
    public double getWheelRotation(int moduleNumber) {
        if (useRelativeEncoderPosition) {
            double relPos = getSwervePosition(moduleNumber) % 360;
            if (relPos < 0) relPos += 360;
            return relPos;
        } else {
            return swerveCanCoders[moduleNumber].getPosition();
        }
    }

    /**
     * Returns the angle/position of the modules
     *
     * @return and array of the four angles
     */
    public double[] getWheelRotations() {
        double[] positions = new double[4];
        for (int i = 0; i < 4; i++) {
            if (useRelativeEncoderPosition) {
                double relPos = getSwervePosition(i) % 360;
                if (relPos < 0) relPos += 360;
                positions[i] = relPos;
            } else {
                positions[i] = swerveCanCoders[i].getPosition();
            }
        }
        return positions;
    }


    @Contract(pure = true)
    public double[] getModuleSpeeds() {
        double[] speeds = new double[4];

        for (int i = 0; i < 4; i++) {
            speeds[i] = (getSwerveDriveVelocity(i) / 60.0d) * Constants.SWERVE_METER_PER_ROTATION;
        }
        return speeds;
    }

    /**
     * Takes in the robot's current position, and uses that to turn the robot towards the goal.
     *
     * @param currentPos the current position of the robot in meters.
     */
    public void fallbackAim(Translation2d currentPos) {
        Translation2d diff = Constants.GOAL_POSITION.minus(currentPos);
        Rotation2d rotation = new Rotation2d(diff.getX(), diff.getY());
        setRotation(rotation);
    }

    /**
     * Checks if gyro is connected. If disconnected, switches to robot-centric drive for the rest of the match. Reports error to
     * driver station when this happens.
     */
    public void checkGyroConnection() {
        if (!RobotTracker.getInstance().getGyro().isConnected()) {
            if (useFieldRelative) {
                useFieldRelative = false;
                DriverStation.reportError("Gyro disconnected, switching to non field relative drive for rest of match", false);
            }
        }
    }

    /**
     * Closing of Shooter motors is not supported.
     */
    @Override
    public void close() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Can not close this object");
    }
}
