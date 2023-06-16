package datawave.data.normalizer.regex;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents an integer range parsed from a regex repetition that specified a range, e.g. {@code {3,}} or {@code {3,10}}.
 */
public class IntegerRangeNode extends Node {
    
    private int start;
    private Integer end;
    
    public IntegerRangeNode() {
        
    }
    
    public IntegerRangeNode(int start, Integer end) {
        this.start = start;
        this.end = end;
    }
    
    public Integer getStart() {
        return start;
    }
    
    public void setStart(int start) {
        this.start = start;
    }
    
    public Integer getEnd() {
        return end;
    }
    
    public void setEnd(Integer end) {
        this.end = end;
    }
    
    public boolean isEndBounded() {
        return end != null;
    }
    
    @Override
    public NodeType getType() {
        return NodeType.INTEGER_RANGE;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitIntegerRange(this, data);
    }
    
    @Override
    public IntegerRangeNode shallowCopy() {
        return new IntegerRangeNode(this.start, this.end);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IntegerRangeNode that = (IntegerRangeNode) o;
        return Objects.equals(start, that.start) && Objects.equals(end, that.end);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
    
    @Override
    public String toString() {
        return new StringJoiner(", ", IntegerRangeNode.class.getSimpleName() + "[", "]").add("start=" + start).add("end=" + end).toString();
    }
}
