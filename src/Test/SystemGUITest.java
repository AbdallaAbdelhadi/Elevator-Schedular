package Test;
import GUI.SystemGUI;

import Types.ElevatorDirection;
import Types.ElevatorState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class SystemGUITest {
    private static SystemGUI gui;
    private static final int FLOORS = 22;
    private static final int ELEVATORS = 4;

    /**
     * Initializes test harness state
     * before each unit test.
     */
    @BeforeEach
    public void init() {
        gui = new SystemGUI(FLOORS, ELEVATORS);
        this.gui.init();
    }

    /**
     * Tests correct handling for
     * deactivating an elevator.
     */
    @Test
    public void testDeactivateElevator() {
        this.gui.deactivateElevator(2, 2);

        JPanel[] floorPanel = this.gui.getFloorPanels()[this.gui.getFloorNum(2)][2];
        JPanel upArrow = floorPanel[0];
        JPanel door = floorPanel[1];
        JPanel downArrow = floorPanel[2];

        assertTrue (upArrow.getBackground() == this.gui.BUTTON_OFF);
        assertTrue (door.getBackground() == this.gui.DOOR_CLOSED);
        assertTrue(((EmptyBorder) door.getBorder()).getClass().toString().equals("class javax.swing.border.EmptyBorder"));
        assertTrue(downArrow.getBackground() == this.gui.BUTTON_OFF);
    }

    /**
     * Tests correct handling for
     * updating position of an elevator.
     */
    @Test
    public void testUpdateElevatorPosition() {
        this.gui.updateElevatorPosition(2, 2);

        JPanel[] floorPanel = this.gui.getFloorPanels()[this.gui.getFloorNum(2)][2];
        JPanel upArrow = floorPanel[0];
        JPanel door = floorPanel[1];
        JPanel downArrow = floorPanel[2];

        assertTrue (upArrow.getBackground() == this.gui.BUTTON_OFF);
        assertTrue(downArrow.getBackground() == this.gui.BUTTON_OFF);

        assertTrue (door.getBackground() == this.gui.DOOR_CLOSED);
        assertTrue(((LineBorder) door.getBorder()).getLineColor() == Color.red);
    }

    /**
     * Tests correct handling for
     * activating an elevator.
     */
    @Test
    public void testActivateElevator() {
        this.gui.activateElevator(2, 2, ElevatorDirection.UP);

        JPanel[] floorPanel = this.gui.getFloorPanels()[this.gui.getFloorNum(2)][2];
        JPanel upArrow = floorPanel[0];
        JPanel door = floorPanel[1];
        JPanel downArrow = floorPanel[2];

        assertTrue (upArrow.getBackground() == this.gui.BUTTON_ON);
        assertTrue(downArrow.getBackground() == this.gui.BUTTON_OFF);

        assertTrue (door.getBackground() == this.gui.DOOR_OPEN);
        assertTrue(((LineBorder) door.getBorder()).getLineColor() == Color.red);

        this.gui.deactivateElevator(2, 2);
        this.gui.activateElevator(2, 2, ElevatorDirection.DOWN);
        assertTrue (upArrow.getBackground() == this.gui.BUTTON_OFF);
        assertTrue(downArrow.getBackground() == this.gui.BUTTON_ON);

        this.gui.deactivateElevator(2, 2);
        this.gui.activateElevator(2, 2, ElevatorDirection.INACTIVE);
        assertTrue (upArrow.getBackground() == this.gui.BUTTON_OFF);
        assertTrue(downArrow.getBackground() == this.gui.BUTTON_OFF);
    }

    /**
     * Tests correct handling for
     * setting error for an elevator.
     */
    @Test
    public void testSetElevatorError() {
        this.gui.setElevatorError(2, 2, ElevatorState.DOOR_JAM);

        JPanel[] floorPanel = this.gui.getFloorPanels()[this.gui.getFloorNum(2)][2];
        JPanel upArrow = floorPanel[0];
        JPanel door = floorPanel[1];
        JPanel downArrow = floorPanel[2];

        assertTrue (upArrow.getBackground() == this.gui.BUTTON_OFF);
        assertTrue(downArrow.getBackground() == this.gui.BUTTON_OFF);
        assertTrue (door.getBackground() == this.gui.DOOR_CLOSED);
        assertTrue(((LineBorder) door.getBorder()).getLineColor() == this.gui.DOOR_JAM_BORDER);

        this.gui.setElevatorError(2, 2, ElevatorState.STUCK_FLOOR);
        assertTrue (door.getBackground() == this.gui.DOOR_CLOSED);
        assertTrue(((LineBorder) door.getBorder()).getLineColor() == this.gui.STUCK_FLOOR_BORDER);

        this.gui.setElevatorError(2, 2, ElevatorState.TERMINATED);
        assertTrue (door.getBackground() == this.gui.DOOR_CLOSED);
        assertTrue(((LineBorder) door.getBorder()).getLineColor() == this.gui.TERMINATED_BORDER);
    }
}
