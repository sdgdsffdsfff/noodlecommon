package org.fengling.noodlecommon.dbrwseparate.aop.service;

import java.util.List;

public interface TestService {
	public List<String> queryData() throws Exception;
	public void insertData(String name) throws Exception;
}
