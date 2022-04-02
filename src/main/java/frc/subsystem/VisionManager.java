package frc.subsystem;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.trajectory.TrapezoidProfile.State;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.drive.Vector2d;
import frc.robot.Constants;
import frc.subsystem.BlinkinLED.BlinkinLedMode;
import frc.subsystem.BlinkinLED.LedStatus;
import frc.subsystem.Drive.DriveState;
import frc.subsystem.Hopper.HopperState;
import frc.utility.ControllerDriveInputs;
import frc.utility.Limelight;
import frc.utility.Limelight.LedMode;
import frc.utility.MathUtil;
import frc.utility.Timer;
import frc.utility.geometry.MutableTranslation2d;
import frc.utility.shooter.visionlookup.ShooterConfig;
import frc.utility.shooter.visionlookup.VisionLookUpTable;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static frc.robot.Constants.GOAL_POSITION;
import static frc.robot.Constants.MAX_SHOOT_SPEED;
import static frc.utility.geometry.GeometryUtils.angleOf;

public final class VisionManager extends AbstractSubsystem {
    private static final ReentrantReadWriteLock VISION_MANGER_INSTANCE_LOCK = new ReentrantReadWriteLock();
    private static VisionManager instance;

    private final @NotNull RobotTracker robotTracker = RobotTracker.getInstance();
    private final @NotNull Limelight limelight = Limelight.getInstance();
    private final @NotNull Drive drive = Drive.getInstance();
    private final @NotNull Shooter shooter = Shooter.getInstance();
    private final @NotNull BlinkinLED blinkinLED = BlinkinLED.getInstance();

    public final VisionLookUpTable visionLookUpTable = VisionLookUpTable.getInstance();

    public void setShooterConfig(ShooterConfig shooterConfig) {
        visionLookUpTable.setShooterConfig(shooterConfig);
    }

    private VisionManager() {
        super(Constants.VISION_MANAGER_PERIOD, 4);
    }

    public static @NotNull VisionManager getInstance() {
        VISION_MANGER_INSTANCE_LOCK.readLock().lock();
        try {
            if (instance != null) {
                return instance;
            }
        } finally {
            VISION_MANGER_INSTANCE_LOCK.readLock().unlock();
        }

        VISION_MANGER_INSTANCE_LOCK.writeLock().lock();
        try {
            return Objects.requireNonNullElseGet(instance, () -> instance = new VisionManager());
        } finally {
            VISION_MANGER_INSTANCE_LOCK.writeLock().unlock();
        }
    }

    @Override
    public void selfTest() {

    }

    @Override
    public void logData() {
        logData("Distance to Target", Units.metersToInches(getDistanceToTarget()));
        logData("Rotation Target", getAngleToTarget().getDegrees());

        Vector3D correctVector = limelight.getCorrectTargetVector();
        logData("New Distance", Math.hypot(correctVector.getX(), correctVector.getZ()));

        logData("Old Distance ", limelight.getDistance() + Constants.GOAL_RADIUS_IN + 23);

        Vector2d targetPx = limelight.getTargetPosInCameraPixels();

        logData("py", targetPx.y);
        logData("px", targetPx.x);

//        Vector2d newRelGoalPos = limelight.getCorrectGoalPos();
//        logData("New Z", newRelGoalPos.x);
//        logData("New X", newRelGoalPos.y);

        logData("Allow Shooting Robot Speed", drive.getSpeedSquared() < Constants.MAX_SHOOT_SPEED_SQUARED);
        logData("Is Robot Allowed Shoot Tilt",
                Math.abs(robotTracker.getGyro().getRoll()) < 3 && Math.abs(robotTracker.getGyro().getPitch()) < 3);

        Translation2d robotVelocity = getRobotVel();
        Translation2d relativeRobotTranslation = getRelativeGoalTranslation();
        logData("Relative Robot Translation X", relativeRobotTranslation.getX());
        logData("Relative Robot Translation Y", relativeRobotTranslation.getY());

        logData("Vision Robot Velocity X", robotVelocity.getX());
        logData("Vision Robot Velocity Y", robotVelocity.getY());

        double timeFromLastShoot = Timer.getFPGATimestamp() - shooter.getLastShotTime();
        double shooterLookAheadTime = 0.15 - timeFromLastShoot;
        if (shooterLookAheadTime < 0) {
            shooterLookAheadTime = 0.15;
        }

        double turnDelay = 0.00;

        Translation2d aimToPosition = getAdjustedTranslation(shooterLookAheadTime + turnDelay);

        Translation2d fieldCentricCords =
                RobotTracker.getInstance().getLastEstimatedPoseMeters().getTranslation().minus(aimToPosition);
        logData("Calculated Target X", fieldCentricCords.getX());
        logData("Calculated Target Y", fieldCentricCords.getY());

        double allowedTurnError = getAllowedTurnError(aimToPosition.getNorm());

        logData("Allowed Turn Error", allowedTurnError);
        logData("Is Robot Allowed Shoot Aiming",
                Math.abs((angleOf(getRelativeGoalTranslation())
                        .minus(robotTracker.getGyroAngle())).getRadians())
                        < allowedTurnError);

        logData("Acceleration", getAccel().getNorm());
    }


    public void shootAndMove(ControllerDriveInputs controllerDriveInputs, boolean useFieldRelative) {

        double timeFromLastShoot = Timer.getFPGATimestamp() - shooter.getLastShotTime();
        double shooterLookAheadTime = 0.15 - timeFromLastShoot;
        boolean justShot = false;
        if (shooterLookAheadTime < 0) {
            shooterLookAheadTime = 0.15;
            justShot = true;
        }

        double turnDelay = 0.0;

        Translation2d aimToPosition = getAdjustedTranslation(shooterLookAheadTime + turnDelay);
        double targetAngle = angleOf(aimToPosition).getRadians();

        // Get the angle that will be used in the future to calculate the end velocity of the turn
        Translation2d futureAimToPosition = getAdjustedTranslation(shooterLookAheadTime + turnDelay + 0.1);
        double futureTargetAngle = angleOf(futureAimToPosition).getRadians();

        drive.updateTurn(controllerDriveInputs,
                new State(targetAngle, (futureTargetAngle - targetAngle) * 10),
                useFieldRelative,
                0);


        Translation2d aimChecksPosition = getAdjustedTranslation(shooterLookAheadTime);
        updateShooterState(aimChecksPosition.getNorm());

        tryToShoot(aimChecksPosition, (futureTargetAngle - targetAngle) * 10, false);
    }


    public void autoTurnRobotToTarget(ControllerDriveInputs controllerDriveInputs, boolean fieldRelative) {
        Optional<Translation2d> visionTranslation = getVisionTranslation();
        Translation2d relativeGoalPos;
        if (visionTranslation.isPresent()) {
            relativeGoalPos = visionTranslation.get().minus(GOAL_POSITION);
        } else {
            relativeGoalPos = getRelativeGoalTranslation();
        }
        drive.updateTurn(controllerDriveInputs, angleOf(relativeGoalPos), fieldRelative, getAllowedTurnError());
        updateShooterState(relativeGoalPos.getNorm());
        tryToShoot(relativeGoalPos, 0, true);
    }

    private void tryToShoot(Translation2d aimToPosition, double targetAngularSpeed, boolean doSpeedCheck) {
        //@formatter:off
        if (Math.abs((angleOf(aimToPosition).minus(robotTracker.getGyroAngle())).getRadians())
                    < getAllowedTurnError(aimToPosition.getNorm())
                && Math.abs(robotTracker.getLatencyCompedChassisSpeeds().omegaRadiansPerSecond - targetAngularSpeed)
                    < Math.toRadians(8)
                && getAccel().getNorm() < 1
                && (drive.getSpeedSquared() < Constants.MAX_SHOOT_SPEED_SQUARED || !doSpeedCheck)
                && Math.abs(robotTracker.getGyro().getRoll()) < 3 && Math.abs(robotTracker.getGyro().getPitch()) < 3) {
            //@formatter:on
            shooter.setFiring(true);
            if (shooter.isFiring()) {
                if (!checksPassedLastTime && lastPrintTime + 0.5 < Timer.getFPGATimestamp()) {
                    lastPrintTime = Timer.getFPGATimestamp();
                    checksPassedLastTime = true;
                    System.out.println(
                            "Shooting at " + (150 - DriverStation.getMatchTime()) + " "
                                    + Units.metersToInches(aimToPosition.getNorm()));
                }
            } else {
                lastChecksFailedTime = Timer.getFPGATimestamp();
                checksPassedLastTime = false;
            }
        } else {
            checksPassedLastTime = false;
            lastChecksFailedTime = Timer.getFPGATimestamp();
        }

        Hopper.getInstance().setHopperState(HopperState.ON);

        logData("Is Shooter Firing", shooter.isFiring());

        logData("Last Shooter Checks Failed Time", Timer.getFPGATimestamp() - lastChecksFailedTime);
    }

    /**
     * Updates the shooter state based on the distance to the target
     */
    public void updateShooterState() {
        updateShooterState(getDistanceToTarget());
    }

    /**
     * Updates the shooter state based on the distance to the target
     *
     * @param distanceToTarget the distance to the target (in meters)
     */
    public void updateShooterState(double distanceToTarget) {
        logData("Shooter Distance to Target", Units.metersToInches(distanceToTarget));
        shooter.set(visionLookUpTable.getShooterPreset(Units.metersToInches(distanceToTarget)));
    }

    /**
     * @return the current robot velocity from the robot tracker
     */
    @Contract(pure = true)
    public Translation2d getRobotVel() {
        Rotation2d rotation2d = robotTracker.getGyroAngle();
        ChassisSpeeds chassisSpeeds = drive.getSwerveDriveKinematics().toChassisSpeeds(drive.getSwerveModuleStates());
        return new Translation2d(chassisSpeeds.vxMetersPerSecond, chassisSpeeds.vyMetersPerSecond).rotateBy(rotation2d);
    }

    /**
     * @return the current translation of the robot based on the vision data. Will only give correct results if the limelight can
     * see the target
     */
    @Contract(pure = true)
    private @NotNull Optional<Translation2d> getVisionTranslation() {
        if (!limelight.isTargetVisible()) return Optional.empty();

        Rotation2d currentGyroAngle = getLatencyCompedLimelightRotation();

        Vector3D offsetVector = limelight.getCorrectTargetVector();
        double angleOffset = Math.atan2(offsetVector.getX(), offsetVector.getZ());


        double distanceToTarget = Units.inchesToMeters(Math.hypot(offsetVector.getX(), offsetVector.getZ()));

        double angleToTarget = currentGyroAngle.getRadians() - angleOffset;
        return Optional.of(new Translation2d(distanceToTarget * Math.cos(angleToTarget),
                distanceToTarget * Math.sin(angleToTarget))
                .plus(Constants.GOAL_POSITION));
    }

    /**
     * @return current relative translation of the robot based on the robot tracker
     */
    private Translation2d getRelativeGoalTranslation() {
        return robotTracker.getLatencyCompedPoseMeters().getTranslation()
                .plus(robotPositionOffset)
                .minus(Constants.GOAL_POSITION);
    }

    /**
     * Adds a vision update to the robot tracker even if the calculated pose is too far from the expected pose.
     * <p>
     * You need to call {@link #forceVisionOn(Object)} before calling this method.
     */
    public void forceUpdatePose() {
        Optional<Translation2d> visionTranslation = getVisionTranslation();
        visionTranslation.ifPresent(
                translation2d -> {
                    robotTracker.addVisionMeasurement(
                            translation2d,
                            getLimelightTime());
                    robotPositionOffset = new Translation2d();
                }
        );
    }

    /**
     * @return the angle the robot needs to face to point towards the target
     */
    public Rotation2d getAngleToTarget() {
        return angleOf(getRelativeGoalTranslation());
    }

    /**
     * @return Distance to the target in meters
     */
    public double getDistanceToTarget() {
        return getRelativeGoalTranslation().getNorm();
    }


    double lastChecksFailedTime = 0;
    double lastPrintTime = 0;
    boolean checksPassedLastTime = false;
    private @NotNull Translation2d robotPositionOffset = new Translation2d(0, 0);

    /**
     * {@code Math.tan(Constants.GOAL_RADIUS / getDistanceToTarget())}
     *
     * @return The allowed turn error in radians
     */
    private double getAllowedTurnError() {
        return getAllowedTurnError(getDistanceToTarget());
    }

    /**
     * {@code Math.tan(Constants.GOAL_RADIUS / getDistanceToTarget())}
     *
     * @return The allowed turn error in radians
     */
    private double getAllowedTurnError(double distance) {
        return Math.tan((Constants.GOAL_RADIUS * 0.6) / distance);
    }

    @Contract(pure = true)
    public @NotNull Rotation2d getLatencyCompedLimelightRotation() {
        return robotTracker.getGyroRotation(getLimelightTime());
    }

    /**
     * @return the time of the last vision update in seconds
     */
    @Contract(pure = true)
    private double getLimelightTime() {
        double limelightTime = Timer.getFPGATimestamp(); //- (limelight.getLatency() / 1000.0) - (11.0 / 1000);
        logData("Limelight Latency", (limelight.getLatency() / 1000) + (11.0 / 1000));
        return limelightTime;
    }

    private final Set<Object> forceVisionOn = new HashSet<>(5);

    /**
     * Forces the vision system to be on.
     *
     * @param source The source of the call. Used to keep track of what is calling this method. Only once all sources are removed
     *               will vision be turned off.
     */
    public void forceVisionOn(Object source) {
        synchronized (forceVisionOn) {
            forceVisionOn.add(source);
        }
        limelight.setLedMode(LedMode.ON);
    }

    /**
     * Removes a source from the list of sources that are forcing vision on. Will turn vision off if the sources list is empty.
     *
     * @param source The source to remove.
     */
    public void unForceVisionOn(Object source) {
        synchronized (forceVisionOn) {
            forceVisionOn.remove(source);
            if (forceVisionOn.isEmpty()) {
                limelight.setLedMode(LedMode.OFF);
            }
        }
    }

    public boolean isVisionForcedOn() {
        return !forceVisionOn.isEmpty();
    }

    private final Object updateLoopSource = new Object();

    private final LedStatus limelightUsingVisionStatus = new LedStatus(BlinkinLedMode.SOLID_LAWN_GREEN, 1);
    private final LedStatus limelightNotConnectedStatus = new LedStatus(BlinkinLedMode.SOLID_RED, 100);
    private final LedStatus limelightTooFarFromExpectedStatus = new LedStatus(BlinkinLedMode.SOLID_ORANGE, 100);
    private final LedStatus limelightNotVisibleStatus = new LedStatus(BlinkinLedMode.SOLID_RED_ORANGE, 100);

    @Override
    public void update() {
        robotPositionOffset = new Translation2d();
        Translation2d relativeGoalPos = getRelativeGoalTranslation();

        double angleToTarget = Math.atan2(relativeGoalPos.getY(), relativeGoalPos.getX());

        if (!limelight.isTargetVisible()) {
            blinkinLED.setStatus(limelightNotConnectedStatus);
        }

        if (Math.abs(new Rotation2d(angleToTarget).minus(robotTracker.getGyroAngle()).getRadians()) < Math.toRadians(50)) {
            forceVisionOn(updateLoopSource);
        } else {
            unForceVisionOn(updateLoopSource);
        }

        logData("Angle To Target", angleToTarget);


        Optional<Translation2d> robotTranslationOptional = getVisionTranslation();
        if (robotTranslationOptional.isPresent()) {
            Translation2d robotTranslation = robotTranslationOptional.get();

            Pose2d visionPose = new Pose2d(robotTranslation, getLatencyCompedLimelightRotation());
            logData("Vision Pose X", visionPose.getX());
            logData("Vision Pose Y", visionPose.getY());
            logData("Vision Pose Angle", visionPose.getRotation().getRadians());
            logData("Vision Pose Time", getLimelightTime());

            Translation2d trackerTranslation = robotTracker.getLatencyCompedPoseMeters().getTranslation();

            logData("Tracker Translation X", trackerTranslation.getX());
            logData("Tracker Translation Y", trackerTranslation.getY());

            if (MathUtil.dist2(robotTracker.getLatencyCompedPoseMeters().getTranslation(),
                    robotTranslation) < Constants.VISION_MANAGER_DISTANCE_THRESHOLD_SQUARED) {
                if (limelight.areCornersTouchingEdge()) {
                    logData("Using Vision Info", "Corners touching edge");
                } else {
                    if (!DriverStation.isAutonomous()) {
                        robotTracker.addVisionMeasurement(robotTranslation,
                                getLimelightTime());
                    }
                    robotPositionOffset = new Translation2d();
                    logData("Using Vision Info", "Using Vision Info");
                    blinkinLED.setStatus(limelightUsingVisionStatus);
                }
            } else {
                logData("Using Vision Info", "Position is too far from expected");
                blinkinLED.setStatus(limelightTooFarFromExpectedStatus);
            }
        } else {
            logData("Using Vision Info", "No target visible");
            blinkinLED.setStatus(limelightNotVisibleStatus);
        }
    }


    private static final ControllerDriveInputs CONTROLLER_DRIVE_NO_MOVEMENT = new ControllerDriveInputs(0, 0, 0);

    /**
     * For auto use only
     */
    @SuppressWarnings({"unused", "BusyWait"})
    public void shootBalls(double numBalls) throws InterruptedException {
        forceVisionOn(this);
        if (drive.driveState == DriveState.RAMSETE) {
            drive.setAutoAiming(true);
        } else {
            autoTurnRobotToTarget(CONTROLLER_DRIVE_NO_MOVEMENT, true);
        }
        updateShooterState();

        while ((drive.isAiming() || !shooter.isHoodAtTargetAngle() || !shooter.isShooterAtTargetSpeed() || drive.getSpeedSquared() > MAX_SHOOT_SPEED)) {
            if (drive.driveState == DriveState.RAMSETE) {
                drive.setAutoAiming(true);
            } else {
                autoTurnRobotToTarget(CONTROLLER_DRIVE_NO_MOVEMENT, true);
            }
            updateShooterState();
            Thread.sleep(10); // Will exit if interrupted
        }
        double shootUntilTime = Timer.getFPGATimestamp() + (numBalls * Constants.SHOOT_TIME_PER_BALL);

        shooter.setFiring(true);
        while (Timer.getFPGATimestamp() < shootUntilTime) {
            if (drive.driveState == DriveState.RAMSETE) {
                drive.setAutoAiming(true);
            } else {
                autoTurnRobotToTarget(CONTROLLER_DRIVE_NO_MOVEMENT, true);
            }
            updateShooterState();
            Thread.sleep(10); // Will exit if interrupted
        }
        unForceVisionOn(this);
        shooter.setFiring(false);
        shooter.setSpeed(0);
        drive.setAutoAiming(false);
    }


    @Override
    public void close() throws Exception {

    }


    //------- Math Methods -------

    /**
     * @return the current position of the robot based on a translation and some time. It adds the current velocity * time to the
     * translation.
     */
    @Contract(pure = true)
    private Translation2d predictFutureTranslation(double predictAheadTime, Translation2d currentTranslation,
                                                   Translation2d currentVelocity, Translation2d currentAcceleration) {
        return currentTranslation
                .plus(currentVelocity.times(predictAheadTime))
                .plus(currentTranslation.times(0.5 * predictAheadTime * predictAheadTime));
    }

    /**
     * Calculates a "fake" target position that the robot should aim to based on the current position and the current velocity. If
     * the robot shoots to this "fake" position, it will shoot the ball into the actual target.
     *
     * @param relativeGoalTranslation The relative position of the target from the robot
     * @param robotVelocity           The velocity of the robot
     * @return The position of the "fake" target
     */
    @Contract(pure = true)
    @NotNull Translation2d getVelocityAdjustedRelativeTranslation(
            @NotNull Translation2d relativeGoalTranslation, @NotNull Translation2d robotVelocity) {

        MutableTranslation2d fakeGoalPos = new MutableTranslation2d(relativeGoalTranslation);

        double relGoalX = relativeGoalTranslation.getX();
        double relGoalY = relativeGoalTranslation.getY();

        double velX = robotVelocity.getX();
        double velY = robotVelocity.getY();

        for (int i = 0; i < 40; i++) {
            //System.out.println("Iteration: " + i + " Fake Goal Pos: " + fakeGoalPos);
            double tof = getTimeOfFlight(fakeGoalPos);

            fakeGoalPos.set(
                    relGoalX + (velX * tof),
                    relGoalY + (velY * tof)
            );
        }
        return fakeGoalPos.getTranslation2d();
    }

    /**
     * @param translation2d The position of the target
     * @return the time of flight to the target
     */
    double getTimeOfFlight(Translation2d translation2d) {
        double distance = Units.metersToInches(translation2d.getNorm());

        double timeOfFlightFrames;
        if (distance < 120) {
            timeOfFlightFrames = ((0.02 / 30) * (distance - 113)) + (22.0 / 30);
        } else {
            timeOfFlightFrames = ((0.071 / 30) * (distance - 113)) + (22.0 / 30);
        }

        //timeOfFlightFrames = 0.000227991 * (distance * distance) - 0.0255545 * (distance) + 31.9542;
        return timeOfFlightFrames;
    }

    /**
     * @param predictAheadTime The amount of time ahead of the robot that the robot should predict
     * @return A translation that compensates for the robot's velocity. This is used to calculate the "fake" target position
     */
    @NotNull
    private Translation2d getAdjustedTranslation(double predictAheadTime) {
        Translation2d relativeRobotTranslation = getRelativeGoalTranslation();
        Translation2d robotVelocity = getRobotVel();
        return getVelocityAdjustedRelativeTranslation(
                predictFutureTranslation(predictAheadTime, relativeRobotTranslation, robotVelocity, getAccel()),
                robotVelocity.plus(getAccel().times(predictAheadTime))
        );
    }


    private Translation2d getAccel() {
        return robotTracker.getAcceleration();
    }
}
