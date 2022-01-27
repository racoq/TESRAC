package evaluation;

import java.io.Serializable;

public class TimeResult implements Serializable
{
    long time;
    String testClassName;
    String testCaseName;
    String status;
    
    public TimeResult(String testClassName, String testCaseName, String status, long time)
    {
	this.testClassName = testClassName;
	this.testCaseName = testCaseName;
	this.time = time;
	this.status = status;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }

    public String getTestClassName()
    {
        return testClassName;
    }

    public void setTestClassName(String testClassName)
    {
        this.testClassName = testClassName;
    }

    public String getTestCaseName()
    {
        return testCaseName;
    }

    public void setTestCaseName(String testCaseName)
    {
        this.testCaseName = testCaseName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
    
}
