package evaluation;


import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(ExtendedRunner.class)
public class StopWatchParentTestClass
{
    @Rule
    public MyJUnitStopWatch stopwatch = new MyJUnitStopWatch();
}
