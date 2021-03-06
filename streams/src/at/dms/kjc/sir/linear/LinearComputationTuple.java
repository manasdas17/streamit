package at.dms.kjc.sir.linear;

/** 
 * A LinearComputationTuple represents tuples of (input position, coefficent)
 * (eg the terms that are calculated to produce linear output.)
 * The input position is defined as follows: input position 0 is the input
 * generated by peek(0); input position 1 is the input generated by peek(1) and
 * so on. The primary use of a tuple is to determine redundant computation 
 * across filter executions, and the correspondence of position to peek
 * expressions in subsequent firings of the work function
 * as explained in LinearRedundancy.
 */
public class LinearComputationTuple {
    private int position;
    private ComplexNumber coefficient;

    /** Make a new tuple with the specified input position and coefficient. **/
    LinearComputationTuple(int inputPosition,
                           ComplexNumber computationCoefficient) {
        this.position = inputPosition;
        this.coefficient = computationCoefficient;
    }
    
    /////////////////////
    /// Accessors   
    /////////////////////
    /** Return the coefficient of this tuple. **/
    public ComplexNumber getCoefficient() {
        // complex numbers are immutable, so no problem with sharing.
        return this.coefficient;
    }
    /** Returns the input position of the data that this tuple uses. **/
    public int getPosition() {
        return this.position;
    }
    
    /** Two tuples are equal if their position and coefficient are equal. **/
    public boolean equals(Object o) {
        if (!(o instanceof LinearComputationTuple)) {
            return false;
        }
        LinearComputationTuple other = (LinearComputationTuple) o;
        return ((this.position == other.position)
                && (this.coefficient.equals(other.coefficient)));
    }
    
    /**
     * Reimplement hashcode so that if two tuples are equal, their
     * hashcodes are also equal.
     **/
    public int hashCode() {
        return this.position + (int)this.coefficient.getReal();
    }
    /** Pretty print. **/
    public String toString() {
        return ("<" + this.position + "," + this.coefficient + ">");
    }
}
