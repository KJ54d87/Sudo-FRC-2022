package frc.robot;

import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.util.Units;

public final class Constants {

    // Logging Period
    /**
     * Every subsystem will log one for every 20 update periods
     */
    public static final int DEFAULT_PERIODS_PER_LOG = 20;
    // Input Constants
    /**
     * This is the max time in seconds that a driver can let go of a button that is supposed to be held and still have it count as
     * holding the button
     */
    public static final double MAX_TIME_NOT_HELD_SEC = .100;

    /**
     * Time required in seconds for a button that is supposed to be held down to activate
     */
    public static final double HELD_BUTTON_TIME_THRESHOLD_SEC = 1;

    //Limelight
    //Calibrate using https://www.desmos.com/calculator/n2dsvzsyhk
    public static final double CAMERA_TARGET_HEIGHT_OFFSET = 0; //TODO: CHANGE
    public static final double CAMERA_Y_ANGLE = 0; //TODO: CHANGE

    // Vision Manager
    public static final int VISION_MANAGER_PERIOD = 1000 / 22; //22Hz
    public static final double SHOOT_TIME_PER_BALL = 0.2; // For Auto

    /**
     * Max speed of the robot while shooter (m/s)
     */
    public static final double MAX_SHOOT_SPEED = 0.1;
    /**
     * Relative position of the limelight from the center of the robot.
     */
    public static final Translation2d LIMELIGHT_CENTER_OFFSET = new Translation2d(-0.684, 0); //TODO: CHANGE
    public static final double VISION_MANAGER_DISTANCE_THRESHOLD_SQUARED = Math.pow(1.0, 2); //TODO: CHANGE

    //Drive Constants
    public static final int DRIVE_PERIOD = 20; // TODO: APPEND UNITS

    public static final int DRIVE_LEFT_FRONT_ID = 11;
    public static final int DRIVE_LEFT_BACK_ID = 12;
    public static final int DRIVE_RIGHT_FRONT_ID = 13;
    public static final int DRIVE_RIGHT_BACK_ID = 14;

    public static final int DRIVE_LEFT_FRONT_SWERVE_ID = 15;
    public static final int DRIVE_LEFT_BACK_SWERVE_ID = 16;
    public static final int DRIVE_RIGHT_FRONT_SWERVE_ID = 17;
    public static final int DRIVE_RIGHT_BACK_SWERVE_ID = 18;

    public static final int CAN_LEFT_FRONT_ID = 19;
    public static final int CAN_LEFT_BACK_ID = 20;
    public static final int CAN_RIGHT_FRONT_ID = 21;
    public static final int CAN_RIGHT_BACK_ID = 22;

    public static final double SWERVE_INCHES_PER_ROTATION = 12.5 * 0.976;
    public static final double SWERVE_METER_PER_ROTATION = Units.inchesToMeters(SWERVE_INCHES_PER_ROTATION);
    public static final double SWERVE_DRIVE_P = 0.1;
    public static final double SWERVE_DRIVE_D = 0.00;
    public static final double SWERVE_DRIVE_I = 0.00;
    public static final double SWERVE_DRIVE_F = 0.00;
    public static final double SWERVE_DRIVE_INTEGRAL_ZONE = 0.00;
    public static final double SWERVE_DRIVE_RAMP_RATE = 0.1;

    public static final int SWERVE_MOTOR_PID_TIMEOUT_MS = 50;

    /**
     * Feed forward constants for the drivetrain.
     * <p>
     * 0 -> Left Front
     * <p>
     * 1 -> Left Back
     * <p>
     * 2 -> Right Front
     * <p>
     * 3 -> Right Back
     */
    public static final SimpleMotorFeedforward[] DRIVE_FEEDFORWARD = {
            new SimpleMotorFeedforward(0.63898, 0.0093357, 0.0014518),
            new SimpleMotorFeedforward(0.63898, 0.0093357, 0.0014518),
            new SimpleMotorFeedforward(0.63898, 0.0093357, 0.0014518),
            new SimpleMotorFeedforward(0.63898, 0.0093357, 0.0014518)};


    /**
     * What the module states should be in hold mode. The wheels will be put in an X pattern to prevent the robot from moving.
     * <p>
     * 0 -> Left Front
     * <p>
     * 1 -> Left Back
     * <p>
     * 2 -> Right Front
     * <p>
     * 3 -> Right Back
     */
    public static final SwerveModuleState[] HOLD_MODULE_STATES = {
            new SwerveModuleState(0, Rotation2d.fromDegrees(-45)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(-45)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(45)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(45))
    };
    /**
     * What the module states should be set to when we start climbing. All the wheels will face forward to make the robot easy to
     * push once it is disabled.
     * <p>
     * 0 -> Left Front
     * <p>
     * 1 -> Left Back
     * <p>
     * 2 -> Right Front
     * <p>
     * 3 -> Right Back
     */
    public static final SwerveModuleState[] SWERVE_MODULE_STATE_FORWARD = {
            new SwerveModuleState(0, Rotation2d.fromDegrees(90)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(90)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(90)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(90))
    };

    // 0.307975 is 12.125 in inches
    public static final Translation2d SWERVE_LEFT_FRONT_LOCATION = new Translation2d(0.307975, 0.307975);
    public static final Translation2d SWERVE_LEFT_BACK_LOCATION = new Translation2d(-0.307975, 0.307975);
    public static final Translation2d SWERVE_RIGHT_FRONT_LOCATION = new Translation2d(0.307975, -0.307975);
    public static final Translation2d SWERVE_RIGHT_BACK_LOCATION = new Translation2d(-0.307975, -0.307975);


    public static final double DRIVE_HIGH_SPEED_M = 7.26;
    @SuppressWarnings("unused") public static final double DRIVE_HIGH_SPEED_IN = Units.metersToInches(DRIVE_HIGH_SPEED_M);

    /**
     * Allowed Turn Error in degrees.
     */
    public static final double MAX_TURN_ERROR = 0.85;

    /**
     * Allowed Turn Error in degrees.
     */
    public static final double MAX_PID_STOP_SPEED = Math.toRadians(5.2);

    // 2048 sensor units per revolution
    public static final double FALCON_ENCODER_TICKS_PER_ROTATIONS = 2048;
    public static final double FALCON_ENCODER_TICKS_PER_100_MS_TO_RPM = 600 / 2048.0d;
    public static final double SWERVE_MOTOR_POSITION_CONVERSION_FACTOR = 1 / 12.8;

    public static final int SWERVE_MOTOR_CURRENT_LIMIT = 15;
    public static final int SWERVE_DRIVE_MOTOR_CURRENT_LIMIT = 15;
    public static final int SWERVE_DRIVE_VOLTAGE_LIMIT = 10;

    public static final double SWERVE_DRIVE_MOTOR_REDUCTION = 1 / 8.14;

    /**
     * Units are in Meters Per Second Squared Supposed to be 5
     */
    public static final double MAX_ACCELERATION = 20;

    /**
     * Units are in Radians per Second Squared
     */
    public static final double MAX_ANGULAR_ACCELERATION = Math.toRadians(360 * 9);

    //field/Vision Manager constants
    public static final Translation2d GOAL_POSITION = new Translation2d(8.25, 0);
    public static final double VISION_PREDICT_AHEAD_TIME = 0.5;
    /**
     * The distance to the center of the goal to the vision tape.
     */
    public static final double GOAL_RADIUS = 0.5; //TODO: get actual value

    public static final double GRAVITY = 9.80665;

    /**
     * Goal height in meters.
     */
    public static final double GOAL_HEIGHT = 2.64;
    /**
     * The height of the center of the Ejection point in meters.
     */
    public static final double SHOOTER_HEIGHT = 0.5; //TODO: Config

    public static final double MAX_SHOOTER_RPM = 5500;
    public static final double MAX_PREFER_SHOOTER_RPM = 4500;


    //Hopper Constants
    public static final int HOPPER_PERIOD = 200;
    public static final double HOPPER_SPEED = .5;
    public static final int HOPPER_MOTOR_ID = 30;


    // Shooter Constants

    public static final int SHOOTER_PERIOD_MS = 20;

    public static final int SHOOTER_WHEEL_CAN_MASTER_ID = 50; // TODO: Get actual CAN ID for all shooter components
    public static final int SHOOTER_WHEEL_CAN_SLAVE_ID = 51;
    public static final int FEEDER_WHEEL_CAN_ID = 52;
    public static final int HOOD_MOTOR_CAN_ID = 53;

    public static final int HOOD_HOME_SWITCH_DIO_ID = 6;

    // Shooter PID & Misc
    // TODO: Configure PID for all shooter motors and current limits

    public static final double SHOOTER_P = 5.0e-1;
    public static final double SHOOTER_I = 0;
    public static final double SHOOTER_D = 0;
    public static final double SHOOTER_F = 0;
    public static final double SHOOTER_I_ZONE = 0;

    public static final double SHOOTER_CURRENT_LIMIT = 40;
    public static final double SHOOTER_TRIGGER_THRESHOLD_CURRENT = 40;
    public static final double SHOOTER_TRIGGER_THRESHOLD_TIME = 0;

    /**
     * Allowed Angular Speed error (in RPM) when comparing speed reported by encoder to an expected speed
     */
    public static final double ALLOWED_SHOOTER_SPEED_ERROR_RPM = 200;

    /**
     * Conversion from Falcon Sensor Units / 100ms to RPM 2048 is Sensor Units Per Revolution 600 Converts From Time of 100ms to 1
     * minute
     */
    public static final double FALCON_UNIT_CONVERSION_FOR_RELATIVE_ENCODER = 600.0d / 2048.0d;

    public static final double SET_SHOOTER_SPEED_CONVERSION_FACTOR = (2048.0d / 600.0d) * 1;

    // Feeder wheel pidf is unused
    public static final double FEEDER_WHEEL_P = 0;
    public static final double FEEDER_WHEEL_I = 0;
    public static final double FEEDER_WHEEL_D = 0;
    public static final double FEEDER_WHEEL_F = 0;
    public static final double FEEDER_WHEEL_I_ZONE = 0;

    public static final double FEEDER_WHEEL_SPEED = 1.0;

    public static final double FEEDER_CURRENT_LIMIT = 30;
    public static final double FEEDER_TRIGGER_THRESHOLD_CURRENT = 30;
    public static final double FEEDER_TRIGGER_THRESHOLD_TIME = 0.1;

    /**
     * The time that the feeder must be on before it is allowed to turn off
     */
    public static final double FEEDER_CHANGE_STATE_DELAY_SEC = 0.1;

    public static final double HOOD_P = 0.1;
    public static final double HOOD_I = 0;
    public static final double HOOD_D = 0;
    public static final double HOOD_F = 0;
    public static final double HOOD_I_ZONE = 0;

    public static final double HOOD_MAX_OUTPUT = 0.5;

    public static final int HOOD_CURRENT_LIMIT_AMPS = 20;

    // Hood Constants

    /**
     * Offset for the absolute encoder on hood in order to make angle between 50 and 90
     */
    public static final double HOOD_ABSOLUTE_ENCODER_OFFSET = 0; // TODO: Find proper offset

    /**
     * Amount of degrees the hood turns per NEO550 rotation
     */
    public static final double HOOD_DEGREES_PER_MOTOR_ROTATION = 3.69230;

    /**
     * Maximum allowed time homing should take in seconds
     */
    public static final double MAX_HOMING_TIME_S = 2;

    /**
     * 30 Percent of motor power should be used when homing
     */
    public static final double HOMING_MOTOR_PERCENT_OUTPUT = 0.30;

    /**
     * Allowed error when comparing Hood angle to a desired angle Units are in rotations of the motor. 1 Rotation is 3.69230
     * degrees of the hood
     */
    public static final double ALLOWED_HOOD_ANGLE_ERROR = 0.2;

    /**
     * If hood speed is under this value, hood has stopped
     */
    public static final double HOOD_HAS_STOPPED_REFERENCE = 1.0e-3;

    // Shooter Blinkin LED Constants

    // ON mode
    /**
     * Color value from -1 to 1 to be used with Blinkin LED
     * <p>
     * LED color is Red Orange - Solid
     */
    public static final double LED_FLYWHEEL_APPROACHING_DESIRED_SPEED = 0.63;

    /**
     * Color value from -1 to 1 to be used with Blinkin LED
     * <p>
     * LED color is Yellow - Solid
     */
    public static final double LED_HOOD_APPROACHING_DESIRED_POSITION = 0.69;

    /**
     * Color value from -1 to 1 to be used with Blinkin LED
     * <p>
     * LED color is Lime - Solid
     */
    public static final double LED_SHOOTER_READY_TO_SHOOT = 0.73;

    // HOMING
    /**
     * Color value from -1 to 1 to be used with Blinkin LED
     * <p>
     * Color is Blue - Strobe light
     */
    public static final double LED_HOOD_HOMING_IN_PROGRESS = -0.09;

    // TEST
    /**
     * Color value from -1 to 1 to be used with Blinkin
     * <p>
     * LED Color is Gold - Strobe light
     */
    public static final double LED_TEST_IN_PROGRESS = -0.07;

    // Shooter Test Constants

    /**
     * Shooter Speed in RPM that is used for TEST
     */
    public static final double SHOOTER_TEST_SPEED_RPM = 3000;

    /**
     * How long an individual test of a component should go on for in miliseconds
     */
    public static final long TEST_TIME_MS = 5000;


    //Climber Constants
    public static final int CLIMBER_PERIOD = 50;
    public static final int CLIMBER_MOTOR_ID = 25;
    public static final int CLIMBER_MOTOR_2_ID = 25;

    public static final double CLIMBER_MOTOR_KF = 0.0;
    public static final double CLIMBER_MOTOR_KP = 0.1;
    public static final double CLIMBER_MOTOR_KI = 0.0;
    public static final double CLIMBER_MOTOR_KD = 0.0;
    public static final double CLIMBER_MOTOR_IZONE = 10;
    public static final double CLIMBER_MOTOR_MAX_IACCUMULATOR = 0.1;
    public static final double CLIMBER_MOTOR_MAX_OUTPUT = 1.0;
    public static final int CLIMBER_MOTOR_MAX_ERROR = 5;

    public static final int CLIMBER_CURRENT_LIMIT = 30;

    public static final int ELEVATOR_ARM_CONTACT_SWITCH_A_DIO_CHANNEL = 0;
    public static final int ELEVATOR_ARM_CONTACT_SWITCH_B_DIO_CHANNEL = 1;

    public static final int PIVOTING_ARM_CONTACT_SWITCH_A_DIO_CHANNEL = 2;
    public static final int PIVOTING_ARM_CONTACT_SWITCH_B_DIO_CHANNEL = 3;

    public static final int PIVOTING_ARM_LATCHED_SWITCH_A_DIO_CHANNEL = 4;
    public static final int PIVOTING_ARM_LATCHED_SWITCH_B_DIO_CHANNEL = 5;

    public static final int LATCH_SOLENOID_ID = 1;
    public static final int PIVOT_SOLENOID_ID = 2;
    public static final int BRAKE_SOLENOID_ID = 3;

    /**
     * The height to go to once the drivers request the climber to deploy
     */
    public static final double CLIMBER_DEPLOY_HEIGHT = 10000;

    /**
     * If the elevator arm is below this height and going down, the climb will abort
     */
    public static final double MIN_CLIMBER_ELEVATOR_HEIGHT = 50;

    /**
     * If the elevator arm is above this height and going down, the climb will abort
     */
    public static final double MAX_CLIMBER_ELEVATOR_HEIGHT = 12000;

    /**
     * How long it takes for the pivot pneumatic to pivot open (become pivoted) (in seconds)
     */
    public static final double ARM_PIVOT_DURATION = 0.5;

    /**
     * How long it takes for the pivot pneumatic to close (become inline) (in seconds)
     */
    public static final double ARM_UNPIVOT_DURATION = 0.5;

    /**
     * How long it takes for the latch pneumatic on the pivot arm to unlatch (in seconds)
     */
    public static final double PIVOT_ARM_UNLATCH_DURATION = 0.5;

    /**
     * Amount (relative) to move the climber arm up to unlatch the elevator arm.
     */
    public static final double CLIMBER_ELEVATOR_UNLATCH_AMOUNT = 100;

    /**
     * The max safe height for the elevator arm during the swinging part of the climb
     */
    public static final double CLIMBER_ELEVATOR_MAX_SAFE_HEIGHT = 10000;

    /**
     * The height the elevator arm should be at when the climber is doing the final extension to hit the bar
     */
    public static final double MAX_CLIMBER_EXTENSION = 11000;

    //Robot Tracker
    public static final double DRIVE_VELOCITY_MEASUREMENT_LATENCY = 0.0025;
    public static final int ROBOT_TRACKER_PERIOD = 10;

    // Intake Constants TODO: Need To Set
    public static final int INTAKE_PERIOD = 50;
    public static final int SOLENOID_CHANNEL = 0;
    public static final int INTAKE_MOTOR_DEVICE_ID = 40;
    public static final double INTAKE_MOTOR_SPEED = -1.0;
    public static final double INTAKE_OPEN_TIME = 0.3;
    public static final int HOOD_ABSOLUTE_ENCODER_CAN_ID = 1; // Todo

    // Networking and Logging
    public static final int WEB_DASHBOARD_PORT = 5802; //Limelight uses port 5800 & 5801

    public static final int WEB_DASHBOARD_SEND_PERIOD_MS = 200;
}
