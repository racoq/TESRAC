package evaluation;

import org.junit.internal.runners.statements.Fail;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

public class ExtendedRunner extends BlockJUnit4ClassRunner {

	public static final int TIMES = 100;
	public static boolean skipIgnored = true;

	public ExtendedRunner(Class<?> klass) throws InitializationError {
		super(klass);
	}

//	@Override
//	protected Description describeChild(FrameworkMethod method) {
//		// if (method.getAnnotation(Repeat.class) != null &&
//		// method.getAnnotation(Ignore.class) == null) {
//		return describeRepeatTest(method);
//		// }
//		// return super.describeChild(method);
//	}
//
//	private Description describeRepeatTest(FrameworkMethod method) {
//		// int times = method.getAnnotation(Repeat.class).value();
//
//		int times = TIMES;
//		Description description = Description.createSuiteDescription(testName(method) + " [" + times + " times]",
//				method.getAnnotations());
//
//		for (int i = 1; i <= times; i++) {
//			description.addChild(Description.createTestDescription(getTestClass().getJavaClass(),
//					"[" + i + "] " + testName(method)));
//		}
//		return description;
//	}

	@Override
	protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
		Description description = describeChild(method);
		if (skipIgnored && isIgnored(method)) {
			notifier.fireTestIgnored(description);
		} else {
			Statement statement;
			try {
				statement = methodBlock(method);
			} catch (Throwable ex) {
				statement = new Fail(ex);
			}
			
			for (int i = 0; i < TIMES; i++) {
				runLeaf(statement, description, notifier);
			}
		}
	}

}