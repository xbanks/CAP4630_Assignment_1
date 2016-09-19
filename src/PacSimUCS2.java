import org.jetbrains.annotations.NotNull;
import pacsim.*;

import java.awt.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Assignment 1 for CAP 4630
 *
 * @author Xavier Banks
 */
public class PacSimUCS2 implements PacAction
{
    private PriorityQueue<State> fringe;
    private HashSet<State> expandedNodes;
    private Queue<PacFace> solution;
    private int nodesExpanded;
    private final int NODE_THING = 1000;
    private final int GOAL = 0;

    PacSimUCS2( String filename )
    {
        // Start the simulation
        PacSim sim = new PacSim( filename );
        sim.init( this );
    }

    public static void main( String[] args )
    {
        new PacSimUCS2( args[0] );
    }

    @Override
    public PacFace action( Object o )
    {
        // If the solution has already been found, then just run it.
        if( solution != null ) {
            return solution.poll();
        }

        PacCell[][] grid = ( PacCell[][] ) o;
        PacmanCell pacman = PacUtils.findPacman( grid );
        Point startingPoint = pacman.getLoc();
        int startingFoodCount = PacUtils.numFood( grid );

        // Add the initial state to the fringe.
        fringe.add( new State( 0, startingFoodCount, new LinkedList<>(), startingPoint, new HashSet<>() ) );

        while( !fringe.isEmpty() ) {
            State currentState = fringe.poll();

            // This is our goal state, where there is no food left
            if( currentState.foodLeft == GOAL ) {
                solution = currentState.path;
                System.out.printf( "\nNodes Expanded: %d fringe size: %d\n", nodesExpanded, fringe.size() );

                // Return the first movement to start the path
                return solution.poll();
            }

            // Cycle through all of the possible movements from the current state
            for( PacFace face : PacFace.values() ) {

                // The state will only be created if it's a valid move, i.e. if the cell is either empty, or a food cell
                State nextState = State.StateTransition( currentState, face, grid );

                // Make sure to only add a new state to the fringe, if that state hasn't already been added
                if( nextState != null && !expandedNodes.contains( nextState ) ) {
                    expandedNodes.add( nextState );
                    fringe.offer( nextState );
                }
            }

            // Increment the nodes expanded after the current state has completed it's expansion
            nodesExpanded++;
            if( nodesExpanded % NODE_THING == 0 ) {
                System.out.printf( "Nodes Expanded: %d fringe size: %d\n", nodesExpanded, fringe.size() );
            }
        }

        return null;
    }

    // Initialize all needed variables for starting the simulation
    @Override
    public void init()
    {
        if( fringe != null )
            fringe.clear();
        fringe = new PriorityQueue<>();
        expandedNodes = new HashSet<>();
        solution = null;
        nodesExpanded = 0;
    }
}

/**
 * The State
 */
class State implements Comparable
{

    // Current cost of the state
    int cost;

    // Food left in the grid by then end of this path
    int foodLeft;

    // The actions to be taken to complete the path
    Queue<PacFace> path;

    // This is the point that pacman will end up in at the end of this state
    Point endPoint;

    // The set of food cell points that have been consumed by this path
    HashSet<Point> foodConsumed;

    /**
     * Constructor
     *
     * @param cost
     * @param foodLeft
     * @param path
     * @param endPoint
     * @param foodConsumed
     */
    State( int cost, int foodLeft, Queue<PacFace> path, Point endPoint, HashSet<Point> foodConsumed )
    {
        this.cost = cost;
        this.foodLeft = foodLeft;
        this.path = new LinkedList<>( path );
        this.endPoint = endPoint;
        this.foodConsumed = new HashSet<>( foodConsumed );
    }

    /**
     * The transition function, taking a current state, and building the next state based on the given face direction
     *
     * @param from The current state being transitioned from
     * @param face The face that is used to make the transition
     * @param grid The GridCell
     *
     * @return The next state
     */
    static State StateTransition( State from, PacFace face, PacCell[][] grid )
    {
        PacCell nextCell = PacUtils.neighbor( face, from.endPoint, grid );

        // This state can only be created if the move is a valid one
        if( !UCSUtils.isMovableTo( nextCell, grid ) )
            return null;

        // Initialize the next state, while incrementing the cost since we've moved one position
        State nextState = new State( from.cost + 1, from.foodLeft, from.path, nextCell.getLoc(), from.foodConsumed );

        // Add the current face to the next state's path
        nextState.path.add( face );

        // If the cell that pacman moves onto has food on it, update the next state's food count and set
        // But only if this food cell hasn't already been eaten
        if( PacUtils.food( nextCell.getX(), nextCell.getY(), grid ) && !nextState.foodConsumed.contains( nextCell.getLoc() ) ) {
            nextState.foodConsumed.add( nextCell.getLoc() );
            nextState.foodLeft -= 1;
        }

        return nextState;
    }

    // This hashCode function encodes the state, to ensure that we don't enter the same state twice
    // This is the main optimization
    @Override
    public int hashCode()
    {
        int cumHash = foodConsumed.hashCode();
        cumHash += foodConsumed.stream().mapToInt( Point::hashCode ).sum();
        return endPoint.hashCode() + foodLeft + cumHash;
    }

    // Make sure that the states are compared by their encoded values
    @Override
    public boolean equals( Object obj )
    {
        return this.hashCode() == obj.hashCode();
    }

    // This compares by cost instead of hashCodes
    // This one is used for the priority ordering of the states
    @Override
    public int compareTo( @NotNull Object o )
    {
        State that = ( State ) o;
        return this.cost - that.cost;
    }
}


class UCSUtils
{
    /**
     * Checks to see if PacMan can move to this state
     *
     * @param cell  The Cell that PacMan wants to move to
     * @param cells The Grid we're checking with
     *
     * @return Whether or not PacMan can move to this cell without dying
     */
    public static boolean isMovableTo( PacCell cell, PacCell[][] cells )
    {
        if( cell instanceof WallCell )
            return false;

        if( cell.getLoc() == PacUtils.findPacman( cells ).getLoc() )
            return true;

        return true; //|| PacUtils.unoccupied( cell.getX(), cell.getY(), cells ) ;
    }
}