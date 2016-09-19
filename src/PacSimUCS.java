import pacsim.*;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by Xavier on 9/18/2016.
 * @author Xavier
 */
public class PacSimUCS implements PacAction {

    private PriorityQueue<UCSNode> fringe;
    private HashSet<UCSNode> states;
    private UCSNode solution;
    private int nodesExpanded;

    private PacSimUCS( String filename )
    {
        PacSim sim = new PacSim( filename );
        sim.init(this);
    }

    public static void main(String[] args) {
        if( args.length > 0 )
        {
            new PacSimUCS( args[0] );
        }
    }

    @Override
    public PacFace action(Object state) {
        // if the solution has already been made, then just run it.
        if( solution != null ) {
            PacFace n = solution.path.poll();
            System.out.println( n );
            return n;
        }

        PacCell[][] cells       = (PacCell[][]) state;
        PacmanCell pacmanCell   = PacUtils.findPacman( cells );

        int startingFoodCount   = PacUtils.numFood(cells);
        Point pacStartingLoc    = pacmanCell.getLoc();

        // Add the starting point to the fringe with zero cost.
        fringe.add( new UCSNode( 0, new LinkedList<>(), new HashSet<>(), pacStartingLoc, pacmanCell.getFace(), startingFoodCount ) );

        // Keep running until the fringe is either empty, or when the popped node is the goal
        while( !fringe.isEmpty() )
        {
            // Pull the node with the least cost off of the fringe
            UCSNode currentNode = fringe.poll();

            // once the goal node has been pulled from the fringe, this is when we stop
            if( currentNode.foodLeft == 0 )
            {
                solution = currentNode;
                return solution.path.poll();
            }

            // Check it's neighbors and add them to the fringe
            for( PacFace face : PacFace.values() )
            {
                UCSNode next = UCSNode.CreateNext( currentNode, face, cells );
                if( next != null && !states.contains(next) )
                {
                    states.add( next );
                    fringe.add( next );
                }
            }

            nodesExpanded++;
            if( nodesExpanded % 1000 == 0 )
            {
                System.out.println( "Nodes Expanded: " + nodesExpanded );
            }
        }

        return null;
    }

    @Override
    public void init() {
        fringe = new PriorityQueue<>( new UCSCompare() );
        states = new HashSet<>();
        solution = null;
        nodesExpanded = 0;
    }
}

class UCSNode {
    int cost;
    Queue<PacFace> path;
    Set<Point> foodConsumed;
    Point currentPoint;
    PacFace currentFace;
    int foodLeft;

    UCSNode( int c, Queue<PacFace> p, Set<Point> f, Point cp, PacFace cf, int fl )
    {
        cost = c;
        path = new LinkedList<>(p);
        foodConsumed = new HashSet<>(f);
        currentPoint = cp;
        currentFace = cf;
        foodLeft = fl;
    }

    private UCSNode( UCSNode that )
    {
        cost = that.cost;
        path = new LinkedList<>(that.path);
        foodConsumed = new HashSet<>(that.foodConsumed);
        currentPoint = that.currentPoint;
        currentFace = that.currentFace;
        foodLeft = that.foodLeft;
    }

    public static UCSNode CreateNext( UCSNode that, PacFace nextFace, PacCell[][] cells ) {
        PacCell neighborCell = PacUtils.neighbor( nextFace, that.currentPoint, cells );

        // Make a copy
        UCSNode next = new UCSNode( that );

        // increment the cost
        next.cost++;

        // Update the current point
        next.currentPoint = neighborCell.getLoc();

        // update the path
        next.path.add( nextFace );

        next.currentFace = nextFace;

        if ( !isFoodOrEmpty( neighborCell, cells ) ) {
            return null;
        }

        if( PacUtils.food( neighborCell.getX(), neighborCell.getY(), cells ) )
        {
            if( !next.foodConsumed.contains( neighborCell.getLoc() ) )
            {
                // Add the current point to the set of previously consumed food cells, so that we don't count them later
                // in the UCS process. Also decrement the amount of food left in this state
                next.foodConsumed.add( neighborCell.getLoc() );
                next.foodLeft--;
            }
        }

        return next;
    }

    private static boolean isFoodOrEmpty( PacCell cell, PacCell[][] cells )
    {
        if( cell instanceof WallCell )
            return false;

        return cell instanceof FoodCell
                || PacUtils.unoccupied( cell.getX(), cell.getY(), cells );
    }

    @Override
    public int hashCode() {
        return currentPoint.hashCode() + foodLeft;
    }

    @Override
    public boolean equals(Object obj) {
        UCSNode that = (UCSNode) obj;
        boolean samepoint = this.currentPoint == that.currentPoint;
        boolean samefood = this.foodLeft == that.foodLeft;
        return samepoint && samefood;
    }
}

class UCSCompare implements Comparator<UCSNode> {
    @Override
    public int compare(UCSNode o1, UCSNode o2) {
        return (o1.cost - o2.cost);
    }
}