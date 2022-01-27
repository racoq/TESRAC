/**
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.symbolic.vm.math;

import org.evosuite.symbolic.expr.Operator;
import org.evosuite.symbolic.expr.fp.RealUnaryExpression;
import org.evosuite.symbolic.expr.fp.RealValue;
import org.evosuite.symbolic.vm.SymbolicFunction;
import org.evosuite.symbolic.vm.SymbolicEnvironment;

public abstract class SIGNUM {

	private static final String SIGNUM = "signum";

	public final static class SIGNUM_D extends SymbolicFunction {

		public SIGNUM_D(SymbolicEnvironment env) {
			super(env, Types.JAVA_LANG_MATH, SIGNUM, Types.D2D_DESCRIPTOR);
		}

		@Override
		public Object executeFunction() {
			double res = this.getConcDoubleRetVal();
			RealValue realExpression = this.getSymbRealArgument(0);
			RealValue signumExpr;
			if (realExpression.containsSymbolicVariable()) {
				Operator op = Operator.SIGNUM;
				signumExpr = new RealUnaryExpression(realExpression, op, res);
			} else {
				signumExpr = this.getSymbRealRetVal();
			}
			return signumExpr;
		}

	}

	public final static class SIGNUM_F extends SymbolicFunction {

		public SIGNUM_F(SymbolicEnvironment env) {
			super(env, Types.JAVA_LANG_MATH, SIGNUM, Types.F2F_DESCRIPTOR);
		}

		@Override
		public Object executeFunction() {
			float res = this.getConcFloatRetVal();
			RealValue realExpression = this.getSymbRealArgument(0);
			RealValue signumExpr;
			if (realExpression.containsSymbolicVariable()) {
				Operator op = Operator.SIGNUM;
				signumExpr = new RealUnaryExpression(realExpression, op,
						(double) res);
			} else {
				signumExpr = this.getSymbRealRetVal();
			}
			return signumExpr;
		}

	}

}
