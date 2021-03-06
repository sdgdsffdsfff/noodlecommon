package org.fl.noodle.common.distributedlock.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fl.noodle.common.distributedlock.api.LockChangeHandler;

public class LockChangeHandlerTest implements LockChangeHandler {

	private final static Log logger = LogFactory.getLog(LockChangeHandlerTest.class);
	
	private String lockName;
	
	@Override
	public void onMessageGetLock() {
		logger.info("Lock: " + lockName + " -> LockChangeHandler -> onMessageGetLock");
	}

	@Override
	public void onMessageLossLock() {
		logger.info("Lock: " + lockName + " -> LockChangeHandler -> onMessageLossLock");
	}

	@Override
	public void onMessageReleaseLock() {
		logger.info("Lock: " + lockName + " -> LockChangeHandler -> onMessageReleaseLock");
	}

	@Override
	public void onMessageStart() {
		logger.info("Lock: " + lockName + " -> LockChangeHandler -> onMessageStart");
	}

	@Override
	public void onMessageStop() {
		logger.info("Lock: " + lockName + " -> LockChangeHandler -> onMessageStop");
	}

	public void setLockName(String lockName) {
		this.lockName = lockName;
	}
}
