package org.evosuite.assertion;

import org.evosuite.testcase.execution.CodeUnderTestException;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.Scope;
import org.evosuite.testcase.statements.*;
import org.evosuite.testcase.variable.VariableReference;

import java.lang.reflect.Array;

public class ArrayLengthObserver extends AssertionTraceObserver<ArrayLengthTraceEntry> {
    /** {@inheritDoc} */
    @Override
    public synchronized void afterStatement(Statement statement, Scope scope,
                                            Throwable exception) {
        // By default, no assertions are created for statements that threw exceptions
        if(exception != null)
            return;

        // No assertions are created for mock statements
        if(statement instanceof FunctionalMockStatement)
            return;

        visitReturnValue(statement, scope);
        visitDependencies(statement, scope);
    }

	/* (non-Javadoc)
	 * @see org.evosuite.assertion.AssertionTraceObserver#visit(org.evosuite.testcase.StatementInterface, org.evosuite.testcase.Scope, org.evosuite.testcase.VariableReference)
	 */
    /** {@inheritDoc} */
    @Override
    protected void visit(Statement statement, Scope scope, VariableReference var) {
        logger.debug("Checking array " + var);
        try {
            // Need only legal values
            if (var == null)
                return;

            // We don't need assertions on constant values
            if (statement instanceof PrimitiveStatement<?>)
                return;

            // We don't need assertions on array assignments
            if (statement instanceof AssignmentStatement)
                return;

            // We don't need assertions on array declarations
            if (statement instanceof ArrayStatement)
                return;

            Object object = var.getObject(scope);

            // We don't need to compare to null
            if (object == null)
                return;

            // We are only interested in arrays
            if (!object.getClass().isArray())
                return;

            if (var.getComponentClass() == null)
                return;

            // TODO: Could also add array lengths of all public fields that are arrays?
            int arrlength = Array.getLength(object);
            logger.debug("Observed length {} for statement {}", arrlength, statement.getCode());
            trace.addEntry(statement.getPosition(), var, new ArrayLengthTraceEntry(var, arrlength));

        } catch (CodeUnderTestException e) {
            logger.debug("", e);
        }
    }

    @Override
    public void testExecutionFinished(ExecutionResult r, Scope s) {
        // do nothing
    }
}
