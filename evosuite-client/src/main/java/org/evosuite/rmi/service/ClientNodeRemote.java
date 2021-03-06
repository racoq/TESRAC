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
package org.evosuite.rmi.service;

import org.evosuite.ga.Chromosome;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

/**
 * Client Node view in the master process.
 * 
 * @author arcuri
 * 
 */

public interface ClientNodeRemote extends Remote {

	public void startNewSearch(String tDir, String rDir) throws RemoteException;

	public void cancelCurrentSearch() throws RemoteException;

	/**
	 * 
	 * @param timeoutInMs  maximum amount of time we can wait for the client to finish
	 * @return <code>true</code> if client is finished
	 * @throws RemoteException
	 * @throws InterruptedException
	 */
	public boolean waitUntilFinished(long timeoutInMs) throws RemoteException,
	        InterruptedException;

	public void doCoverageAnalysis() throws RemoteException;
	
	public void doDependencyAnalysis(String fileName) throws RemoteException;

	public void printClassStatistics() throws RemoteException;
	
	public void immigrate(Set<? extends Chromosome> migrants) throws RemoteException;

    public void collectBestSolutions(Set<? extends Chromosome> solutions) throws RemoteException;
}
