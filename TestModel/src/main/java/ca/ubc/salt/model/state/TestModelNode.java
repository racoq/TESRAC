package ca.ubc.salt.model.state;

import java.util.HashMap;
import java.util.Map;

public class TestModelNode implements Comparable<TestModelNode>
{
    public static TestModelNode curStart;
    public Map<TestModelNode, TestModelNode> parent = new HashMap<TestModelNode, TestModelNode>();
    public Map<TestModelNode, Long> distFrom = new HashMap<TestModelNode, Long>();
    @Override
    public int compareTo(TestModelNode o)
    {
	// TODO Auto-generated method stub
	return this.distFrom.get(curStart).compareTo(o.distFrom.get(curStart));
    }
}
