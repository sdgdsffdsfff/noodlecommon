package org.fengling.noodlecommon.dbrwseparate.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fengling.noodlecommon.dbrwseparate.datasource.DataSourceSwitch;
import org.fengling.noodlecommon.dbrwseparate.datasource.DataSourceType;
import org.fengling.noodlecommon.dbrwseparate.loadbalancer.LoadBalancerManager;

public class ChangeSingleMethodInterceptor implements MethodInterceptor {

	private final Log logger = LogFactory.getLog(ChangeSingleMethodInterceptor.class);
	
	private LoadBalancerManager loadBalancerManager;
	
	private DataSourceType dataSourceType = DataSourceType.MASTER;
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		
		if (loadBalancerManager.checkIsAliveDataSource(dataSourceType)) {
			try {
				DataSourceSwitch.setDataSourceType(dataSourceType);
				return invocation.proceed();
			} catch (Throwable e) {
				if (logger.isErrorEnabled()) {
					logger.error("invoke -> " + dataSourceType + " invoke -> Exception: " + e);
				}
				throw e;
			}
		} else {
			dataSourceType = dataSourceType == DataSourceType.MASTER ? DataSourceType.SALVE_1 : DataSourceType.MASTER;
			try {
				DataSourceSwitch.setDataSourceType(dataSourceType);
				return invocation.proceed();
			} catch (Throwable e) {
				if (logger.isErrorEnabled()) {
					logger.error("invoke -> " + dataSourceType + " invoke -> Exception: " + e);
				}
				throw e;
			}
		}
	}
	
	public void setLoadBalancerManager(LoadBalancerManager loadBalancerManager) {
		this.loadBalancerManager = loadBalancerManager;
	}
}
