// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.auton.TemplateAuto;
import frc.auton.guiauto.NetworkAuto;
import frc.subsystem.BlinkinLED;
import frc.subsystem.Drive;
import frc.subsystem.RobotTracker;
import frc.utility.Controller;
import frc.utility.ControllerDriveInputs;
import frc.utility.Limelight;

/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
    //GUI
    NetworkTableInstance instance = NetworkTableInstance.getDefault();
    NetworkTable autoDataTable = instance.getTable("autodata");
    NetworkTableEntry autoPath = autoDataTable.getEntry("autoPath");
  
    NetworkTable position = autoDataTable.getSubTable("position");
    NetworkTableEntry xPos = position.getEntry("x");
    NetworkTableEntry yPos = position.getEntry("y");
    NetworkTableEntry enabled = autoDataTable.getEntry("enabled");
    NetworkTableEntry pathProcessingStatusEntry = autoDataTable.getEntry("processing");
    NetworkTableEntry pathProcessingStatusIdEntry = autoDataTable.getEntry("processingid");
    
    NetworkAuto networkAuto;
    String lastAutoPath = null;
    
    ExecutorService deserializerExecutor = Executors.newSingleThreadExecutor();
    
    //Auto
    TemplateAuto selectedAuto; 
    Thread autoThread;
    private static final String kDefaultAuto = "Default";
    private static final String kCustomAuto = "My Auto";
    private String m_autoSelected;
    private final SendableChooser<String> m_chooser = new SendableChooser<>();

    //Subsystems
    private final RobotTracker robotTracker = RobotTracker.getInstance();
    private final Drive drive = Drive.getInstance();
    private final BlinkinLED blinkinLED = BlinkinLED.getInstance();
    private final Limelight limelight = Limelight.getInstance();

    //Inputs
    private final Controller xbox = new Controller(0);
	private final Controller stick = new Controller(1);
    private final Controller buttonPanel = new Controller(2);

    
    //Control loop states
    boolean limelightTakeSnapshots;
    
    /**
     * This function is run when the robot is first started up and should be used for any
     * initialization code.
     */
    @Override
    public void robotInit() {
        m_chooser.setDefaultOption("Default Auto", kDefaultAuto);
        m_chooser.addOption("My Auto", kCustomAuto);
        SmartDashboard.putData("Auto choices", m_chooser);

        drive.calculateOffsets(); 
        startSubsystems(); 
    }

    /**
     * This function is called every robot packet, no matter the mode. Use this for items like
     * diagnostics that you want ran during disabled, autonomous, teleoperated and test.
     *
     * <p>This runs after the mode specific periodic functions, but before LiveWindow and
     * SmartDashboard integrated updating.
     */
    @Override
    public void robotPeriodic() {
        if (isEnabled()) {
            //Get data from the robot tracker and upload it to the robot tracker (Units must be in meters)
            xPos.setDouble(robotTracker.getPoseMeters().getX());
            yPos.setDouble(robotTracker.getPoseMeters().getY());
        }
        
        //Listen changes in the network auto
        if (autoPath.getString(null) != null && !autoPath.getString(null).equals(lastAutoPath)) {
            lastAutoPath = autoPath.getString(null);
            deserializerExecutor.execute(() -> { //Start deserializing on another thread
                System.out.println("start parsing autonomous");
                //Set networktable entries for the gui notifications
                pathProcessingStatusEntry.setDouble(1);
                pathProcessingStatusIdEntry.setDouble(pathProcessingStatusIdEntry.getDouble(0) + 1);

                networkAuto = new NetworkAuto(); //Create the auto object which will start deserializing the json and the auto ready to be run
                System.out.println("done parsing autonomous");
                //Set networktable entries for the gui notifications
                pathProcessingStatusEntry.setDouble(2);
                pathProcessingStatusIdEntry.setDouble(pathProcessingStatusIdEntry.getDouble(0) + 1);
            });
        }

        //TODO: Debug why this does not work
        if (buttonPanel.getRisingEdge(9)) {
			limelightTakeSnapshots = !limelightTakeSnapshots;
			limelight.takeSnapshots(limelightTakeSnapshots);
			System.out.println("limelight taking snapshots " + limelightTakeSnapshots);
		}
    }

    /**
     * This autonomous (along with the chooser code above) shows how to select between different
     * autonomous modes using the dashboard. The sendable chooser code works with the Java
     * SmartDashboard. If you prefer the LabVIEW Dashboard, remove all of the chooser code and
     * uncomment the getString line to get the auto name from the text box below the Gyro
     *
     * <p>You can add additional auto modes by adding additional comparisons to the switch structure
     * below with additional strings. If using the SendableChooser make sure to add them to the
     * chooser code above as well.
     */
    @Override
    public void autonomousInit() {
        enabled.setBoolean(true);
        startSubsystems();

        if(networkAuto == null){
            System.out.println("Using normal autos");
            //TODO put autos here
        } else {
            System.out.println("Using autos from network tables");
            selectedAuto = networkAuto;
        }
        //Since autonomous objects can be reused they need to be reset them before we can reuse them again 
        selectedAuto.reset();

        //We then create a new thread to run the auto and run it
        autoThread = new Thread(selectedAuto);
        autoThread.start();
    }

    /** This function is called periodically during autonomous. */
    @Override
    public void autonomousPeriodic() {}

    /** This function is called once when teleop is enabled. */
    @Override
    public void teleopInit() {
        killAuto();
        enabled.setBoolean(true);
        startSubsystems();
        drive.calculateOffsets();
    }

    /** This function is called periodically during operator control. */
    @Override
    public void teleopPeriodic() {
        xbox.update();
        stick.update();
        buttonPanel.update();
        if(xbox.getRawButton(3)){
            //Increase the deadzone so that we drive straight
            drive.swerveDriveFieldRelative(new ControllerDriveInputs(xbox.getRawAxis(0), -xbox.getRawAxis(1),  -xbox.getRawAxis(4))
                    .applyDeadZone(0.2, 0.2, 0.2, 0.2).squareInputs());
        } else {
            drive.swerveDriveFieldRelative(new ControllerDriveInputs(xbox.getRawAxis(0), -xbox.getRawAxis(1),  -xbox.getRawAxis(4))
                    .applyDeadZone(0.05, 0.05, 0.2, 0.2).squareInputs());
        }

    }

    /** This function is called once when the robot is disabled. */
    @Override
    public void disabledInit() {
        killAuto();
        enabled.setBoolean(true);
    }

    /** This function is called periodically when disabled. */
    @Override
    public void disabledPeriodic() {}

    /** This function is called once when test mode is enabled. */
    @Override
    public void testInit() {
        startSubsystems();
        drive.calculateOffsets();
    }

    /** This function is called periodically during test mode. */
    @Override
    public void testPeriodic() {}

    private void startSubsystems()
	{
		robotTracker.start();
		drive.start();
    }
    
    public synchronized void killAuto() {
		if(selectedAuto != null) {
			selectedAuto.killSwitch();
		}

		if(selectedAuto != null) {
			//auto.interrupt();
			//while(!auto.isInterrupted());
			while(autoThread.getState() != Thread.State.TERMINATED);
	 
			drive.stopMovement();
			drive.setTeleop();
		}
	}
}
