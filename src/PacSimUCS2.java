import org.jetbrains.annotations.NotNull;
import pacsim.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * Created by Xavier on 9/18/2016.
 */
public class PacSimUCS2 implements PacAction {


    private PriorityQueue<State> fringe;
    private HashSet<State> expandedNodes;
    private Queue<PacFace> solution;
    private int nodesExpanded;
    private final int NODE_THING = 1000;

    PacSimUCS2( String filename ) {
        PacSim sim = new PacSim( filename );
        sim.init(this);
    }

    public static void main(String[] args) {
        new PacSimUCS2( args[0] );
    }

    @Override
    public PacFace action(Object o) {

        if( solution != null ) {
            return solution.poll();
        }

        PacCell[][] grid    = (PacCell[][]) o ;
        PacmanCell  pacman  = PacUtils.findPacman( grid );
        Point startingPoint = pacman.getLoc();
        int startingFoodCount = PacUtils.numFood( grid );

        fringe.add( new State( 0, startingFoodCount, new LinkedList<>(), startingPoint, new HashSet<>() ) );

        while( !fringe.isEmpty() ) {
            State currentState = fringe.poll();

            if( currentState.foodLeft == 0 ) {
                solution = currentState.path;
                System.out.printf( "\nNodes Expanded: %d fringe size: %d\n", nodesExpanded, fringe.size() );
                return solution.poll();
            }

            for(PacFace face : PacFace.values()) {

                State nextState = State.StateTransition( currentState, face, grid );
                if( nextState != null && !expandedNodes.contains(nextState) ) {
                    expandedNodes.add( nextState );
                    fringe.offer( nextState );
                }
            }

            nodesExpanded++;
            if( nodesExpanded % NODE_THING == 0 ) {
                System.out.printf( "Nodes Expanded: %d fringe size: %d\n", nodesExpanded, fringe.size() );
            }
        }

        return null;
    }

    @Override
    public void init() {
        if( fringe != null )
            fringe.clear();
        fringe = new PriorityQueue<>();
        expandedNodes = new HashSet<>();
        solution = null;
        nodesExpanded = 0;
    }
}

class State implements Comparable {
    int cost;
    int foodLeft;
    Queue<PacFace> path;
    Point endPoint;
    HashSet<Point> foodConsumed;

    State( int c, int f , Queue<PacFace> p, Point e, HashSet<Point> fc )
    {
        cost = c;
        foodLeft = f;
        path = new LinkedList<>(p);
        endPoint = e;
        foodConsumed = new HashSet<>(fc);
    }

    static State StateTransition( State from, PacFace face, PacCell[][] grid ) {
        PacCell nextCell = PacUtils.neighbor( face, from.endPoint, grid );

        if( !UCSUtils.isMovableTo( nextCell, grid ) )
            return null;

        State nextState = new State( from.cost + 1, from.foodLeft, from.path, nextCell.getLoc(), from.foodConsumed );
        nextState.path.add( face );

        if( PacUtils.food( nextCell.getX(), nextCell.getY(), grid ) && !nextState.foodConsumed.contains(nextCell.getLoc()) ) {
            nextState.foodConsumed.add(nextCell.getLoc());
            nextState.foodLeft -= 1;
        }

        return nextState;
    }

    // This is the main optimization
    @Override
    public int hashCode() {
        int cumHash = foodConsumed.hashCode();
        cumHash += foodConsumed.stream().mapToInt(Point::hashCode).sum();
        return endPoint.hashCode() + foodLeft + cumHash;
    }

    @Override
    public boolean equals(Object obj) {
        return this.hashCode() == obj.hashCode();
    }

    // This compares by cost instead of hashCodes
    @Override
    public int compareTo(@NotNull Object o) {
        State that = (State) o;
        return this.cost - that.cost;
    }
}

class UCSUtils {
    public static boolean isMovableTo( PacCell cell, PacCell[][] cells )
    {
        if( cell instanceof WallCell )
            return false;

        if( cell.getLoc() == PacUtils.findPacman(cells).getLoc() )
            return true;

        return true; //|| PacUtils.unoccupied( cell.getX(), cell.getY(), cells ) ;
    }
}