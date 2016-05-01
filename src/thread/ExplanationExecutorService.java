/*******************************************************************************
 * Copyright 2016 by the Department of Computer Science (University of Genova and University of Oxford)
 * 
 *    This file is part of LogMapC an extension of LogMap matcher for conservativity principle.
 * 
 *    LogMapC is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMapC is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMapC.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExplanationExecutorService extends ThreadPoolExecutor {

	public ExplanationExecutorService(int nCore){
        super(nCore, nCore,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
	}
	
    public ExplanationExecutorService(int corePoolSize, int maximumPoolSize, 
    		long keepAliveTime, TimeUnit unit, BlockingQueue workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }
 
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    protected RunnableFuture newTaskFor(Callable c) {
    	return new ExplanationFuture((AbstractExplanationThread) c);
//        if (callable instanceof IdentifiableCallable) {
//            return ((IdentifiableCallable) callable).newTask();
//        } else {
//            return super.newTaskFor(callable); // A regular Callable, delegate to parent
//        }
    }
}
