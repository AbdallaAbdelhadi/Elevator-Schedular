package GUI;

import System.Config;
import Types.ElevatorDirection;
import Types.ElevatorState;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * GUI for elevators.
 * @author Abdalla Abdelhadi, Yousef Yassin, Ibrahim Almalki, Zakariyya Almalki
 */
public class SystemGUI {
    private JFrame frame;
    private JPanel mainPanel;

    private int floors;
    private int elevators;

    // upArrow, door, downArrow
    private JPanel[][][] floorPanels;

    //color of the active elevator
    public static final Border RED_LINE_BORDER = BorderFactory.createLineBorder(Color.red, 5);
    
    //Possible colors for the door states
    public static final Color DOOR_CLOSED = Color.darkGray;
    public static final Color DOOR_OPEN = Color.white;
    
    //Possible colors for the arrow button states
    public static final Color BUTTON_OFF = Color.gray;
    public static final Color BUTTON_ON = Color.yellow;
    
    //Possible colors for the error states
    public static final Color DOOR_JAM_BORDER = new Color(253, 107, 0);
    public static final Color STUCK_FLOOR_BORDER = new Color(18, 29, 219);
    public static final Color TERMINATED_BORDER = new Color(72, 42, 151);

    //number of rows to add to make space for the legend
    public static final int META_ROWS = 5;

    /**
     * SystemGUI constructor 
     * @param floors, the number of floors in system.
     * @param elevators, the number of elevators in system.
     */
    public SystemGUI(int floors, int elevators){
        this.floors = floors;
        this.elevators = elevators;

        this.floorPanels = new JPanel[this.floors][this.elevators][3];

        this.frame = new JFrame("Elevator Simulation");
        this.frame.setSize(1600,1000);
        this.frame.setResizable(false);

        this.mainPanel = new JPanel();
        this.mainPanel.setLayout(new GridLayout(this.floors + META_ROWS,this.elevators + 1, 0, 0));
    }

    /**
     * getter for the floor panels
     * @return JPanel[][][], the floor panels.
     */
    public JPanel[][][] getFloorPanels() {
        return this.floorPanels;
    }

    /**
     * creates the elevators, arrows, and doors
     * @param panel, panel to config.
     * @param y - the position of the panel on the mainPanel
     * @return JPanel, the configured panel.
     */
    private JPanel[] configFloorPanel (JPanel panel, int y){
        panel.setLayout(new GridLayout());

        JPanel door = new JPanel();
        door.setBackground(Color.darkGray);

        JPanel upArrow = new JPanel();
        upArrow.setBackground(Color.gray);
        JLabel up = new JLabel("↑");
        up.setBackground(new Color(1,1,1, 0));
        upArrow.add(up);


        JPanel downArrow = new JPanel();
        downArrow.setBackground(Color.gray);
        JLabel down = new JLabel("↓");
        down.setBackground(new Color(1,1,1, 0));
        downArrow.add(down);

        if(this.getFloorNum(y)==1) {
            door.setBorder(RED_LINE_BORDER);
            door.setBackground(Color.white);

            upArrow.setBackground(Color.yellow);
        }


        JPanel emptyRightPanel = new JPanel();
        emptyRightPanel.setBackground(new Color(235,222,207));

        JPanel emptyLeftPanel = new JPanel();
        emptyLeftPanel.setBackground(new Color(235,222,207));

        panel.add(emptyLeftPanel);
        panel.add(upArrow);
        panel.add(door);
        panel.add(downArrow);
        panel.add(emptyRightPanel);

        return new JPanel[] {
                upArrow,
                door,
                downArrow
        };
    }

    /**
     * get the number of floor, 1 indexed.
     * @param floor, the 0 indexed floor.
     * @return int, the 1 indexed floor.
     */
    public int getFloorNum(int floor){
        return this.floors - floor;
    }

    /**
     * creates the board for the elevators, floor numbers, and legend
     */
    private void initializeOutline() {
        for (int i = 0; i < this.floors; i++) {
            for (int j = 0; j < this.elevators + 1; j++) {
                JPanel panel = new JPanel();

                if (j != 0){
                    this.floorPanels[i][j-1] = configFloorPanel(panel, i);
                }
                else{
                    JLabel label = new JLabel("Floor: " + this.getFloorNum(i));
                    label.setBackground(new Color(186, 152, 134, 0));

                    panel.add(label);
                }

                this.mainPanel.add(panel);
            }
        }

        JLabel[] arr = new JLabel[21];

        arr[0]= new JLabel("<html><h2>&nbsp; Door Color Legend: <h2/><html/>");

        arr[5] = new JLabel("<html><ul><li> Red: Active </li></ul></html>");
        arr[5].setForeground(Color.RED);

        arr[10] = new JLabel("<html><ul><li> Orange: Door Jam </li></ul></html>");
        arr[10].setForeground((new Color(252, 130, 0)));

        arr[15] = new JLabel("<html><ul><li> Blue: Stuck </li></ul></html>");
        arr[15].setForeground(Color.BLUE);

        arr[20] = new JLabel("<html><ul><li> Purple: Terminated </li></ul></html>");
        arr[20].setForeground((new Color(72, 42, 151)));

        //add empty panels under the elevators
        JLabel label;
        for (JLabel j : arr){
            label =j;
            if (label == null){
                label = new JLabel();
            }
            this.mainPanel.add(label);
        }
    }

    /**
     * adds the mainpanel to the frame
     */
    public void init() {
        initializeOutline();

        this.frame.add(this.mainPanel);
    }

    /**
     * displays the gui on the console
     */
    public void display(){
        this.init();
        this.frame.setVisible(true);
    }

    /**
     * Deactivates the elevator when a floor has been jammed
     * @param elevator, elevator to deactivate.
     * @param floor, floor on which elevator is.
     */
    public void deactivateElevator(int elevator, int floor){
        JPanel[] floorPanel = this.floorPanels[this.getFloorNum(floor)][elevator];

        floorPanel[0].setBackground(Color.gray);
        floorPanel[1].setBackground(Color.darkGray);
        floorPanel[1].setBorder(BorderFactory.createEmptyBorder());
        floorPanel[2].setBackground(Color.gray);
        this.frame.repaint();
    }

    /**
     * updates the position of the elevator by highlighting the active elevator door red
     * @param elevator, elevator to update.
     * @param floor, floor on which elevator is to update.
     */
    public void updateElevatorPosition(int elevator, int floor){
        JPanel[] floorPanel = this.floorPanels[this.getFloorNum(floor)][elevator];

        floorPanel[1].setBorder(RED_LINE_BORDER);
    }

    /**
     * activates the elevator that has people riding it 
     * by turning on the arrows, opening doors, and highlighting moving doors 
     * 
     * @param elevator, elevator to activate.
     * @param floor, floor on which elevator is to update.
     * @param direction, direction elevator is travelling.
     */
    public void activateElevator(int elevator, int floor, ElevatorDirection direction){
//        if(direction == ElevatorDirection.INACTIVE){
//            throw new IllegalArgumentException("Got Elevator state INACTIVE");
//        }

        this.updateElevatorPosition(elevator, floor);

        JPanel[] floorPanel = this.floorPanels[this.getFloorNum(floor)][elevator];

        if (direction == ElevatorDirection.UP){
            floorPanel[0].setBackground(Color.yellow);
        }
        else if (direction == ElevatorDirection.DOWN){
            floorPanel[2].setBackground(Color.yellow);
        }
        else{
            floorPanel[0].setBackground(Color.gray);
            floorPanel[2].setBackground(Color.gray);
        }

        floorPanel[1].setBackground(Color.white);
        this.frame.repaint();
    }

    /**
     * Sets the border of the door depending on the ElevatorState
     * @param elevator, the elevator to set error for.
     * @param floor, the floor on which elevator is.
     * @param state, the state associated with the error.
     */
    public void setElevatorError (int elevator, int floor, ElevatorState state){
        JPanel[] floorPanel = this.floorPanels[this.getFloorNum(floor)][elevator];

        if(state == ElevatorState.DOOR_JAM){
            floorPanel[1].setBorder(BorderFactory.createLineBorder(DOOR_JAM_BORDER, 5));
        }
        else if(state == ElevatorState.STUCK_FLOOR){
            floorPanel[1].setBorder(BorderFactory.createLineBorder(STUCK_FLOOR_BORDER, 5));
        }
        else if(state == ElevatorState.TERMINATED){
            floorPanel[1].setBorder(BorderFactory.createLineBorder(TERMINATED_BORDER, 5));
            floorPanel[1].setBackground(Color.darkGray);
            floorPanel[0].setBackground(Color.gray);
            floorPanel[2].setBackground(Color.gray);
        }
    }

    /**
     * Entry point used for testing.
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        SystemGUI g = new SystemGUI(Config.MAX_FLOOR, Config.NUMBER_ELEVATORS);

        g.display();

        Thread.sleep(2000);
        g.deactivateElevator(1,1);

        g.updateElevatorPosition(1,2);

        Thread.sleep(2000);
        g.deactivateElevator(1,2);

        g.activateElevator(1,3,ElevatorDirection.DOWN);

        Thread.sleep(2000);
        g.setElevatorError(1,3,ElevatorState.TERMINATED);

//        Thread.sleep(2000);
//        g.activateElevator(1,3,ElevatorDirection.INACTIVE);

    }
}
