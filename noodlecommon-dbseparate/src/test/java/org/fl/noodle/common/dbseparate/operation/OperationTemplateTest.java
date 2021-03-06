package org.fl.noodle.common.dbseparate.operation;

import static org.junit.Assert.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.fl.noodle.common.dbseparate.operation.OperationCallback;
import org.fl.noodle.common.dbseparate.operation.OperationCallbackExtend;
import org.fl.noodle.common.dbseparate.operation.OperationException;
import org.fl.noodle.common.dbseparate.operation.OperationTemplate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = {
		"classpath:org/fl/noodle/common/dbseparate/operation/noodlecommon-operation.xml"
})
public class OperationTemplateTest extends AbstractJUnit4SpringContextTests {

	@Autowired
	OperationTemplate operationTemplate;
	
	@Autowired
	DataSource dataSource;
	
	@Test
	public void testExecute() throws Exception {
		
		int result = this.operationTemplate.execute(new OperationCallback<Integer>() {
			public Integer executeAction() throws OperationException, Exception {
				PreparedStatement preparedStatement = dataSource.getConnection().prepareStatement("insert into dbseparate_test (name) values (?)");
				preparedStatement.setString(1, "你好");
				return preparedStatement.executeUpdate();
			}
		});
		assertTrue(result > 0);
		
		result = this.operationTemplate.execute(new OperationCallbackExtend<Integer>() {
			public Integer executeAction() throws OperationException, Exception {
				System.out.println("NoodleServiceCallbackExtend -> executeAction...");
				PreparedStatement preparedStatement = dataSource.getConnection().prepareStatement("insert into dbseparate_test (name) values (?)");
				preparedStatement.setString(1, "你好");
				return preparedStatement.executeUpdate();
			}

			@Override
			public boolean beforeExecuteActionCheck() {
				System.out.println("NoodleServiceCallbackExtend -> beforeExecuteActionCheck...");
				return true;
			}

			@Override
			public void beforeExecuteAction() {
				System.out.println("NoodleServiceCallbackExtend -> beforeExecuteAction...");	
			}

			@Override
			public boolean beforeExecuteActionCheckInTransaction() {
				System.out.println("NoodleServiceCallbackExtend -> beforeExecuteActionCheckInTransaction...");	
				return true;
			}

			@Override
			public void beforeExecuteActionInTransaction() {	
				System.out.println("NoodleServiceCallbackExtend -> beforeExecuteActionInTransaction...");
			}

			@Override
			public void afterExecuteActionInTransaction(boolean isSuccess, Integer result, Exception e) {				
				System.out.println("NoodleServiceCallbackExtend -> afterExecuteActionInTransaction...");
			}

			@Override
			public void afterExecuteAction(boolean isSuccess, Integer result, Exception e) {
				System.out.println("NoodleServiceCallbackExtend -> afterExecuteAction...");		
			}
		});
		assertTrue(result > 0);
	}

	@Test
	public void testExecuteWithoutTransaction() throws Exception {
		List<TestModel> testModelList = this.operationTemplate.executeWithoutTransaction(new OperationCallback<List<TestModel>>() {
			public List<TestModel> executeAction() throws OperationException, Exception {
				PreparedStatement preparedStatement = dataSource.getConnection().prepareStatement("select * from dbseparate_test");
				ResultSet resultSet = preparedStatement.executeQuery();
				List<TestModel> testModelList = new ArrayList<TestModel>();
				while (resultSet.next()) {
					TestModel testModel = new TestModel();
					testModel.setId(resultSet.getInt(1));
					testModel.setName(resultSet.getString(2));
					testModelList.add(testModel);
				}
				return testModelList;
			}
		});
		assertNotNull(testModelList);
		for (TestModel testModel : testModelList) {
			System.out.println(testModel);
		}
		
		testModelList = this.operationTemplate.executeWithoutTransaction(new OperationCallbackExtend<List<TestModel>>() {
			public List<TestModel> executeAction() throws OperationException, Exception {
				PreparedStatement preparedStatement = dataSource.getConnection().prepareStatement("select * from dbseparate_test");
				ResultSet resultSet = preparedStatement.executeQuery();
				List<TestModel> testModelList = new ArrayList<TestModel>();
				while (resultSet.next()) {
					TestModel testModel = new TestModel();
					testModel.setId(resultSet.getInt(1));
					testModel.setName(resultSet.getString(2));
					testModelList.add(testModel);
				}
				return testModelList;
			}

			@Override
			public boolean beforeExecuteActionCheck() {
				System.out.println("NoodleServiceCallbackExtend -> beforeExecuteActionCheck...");
				return true;
			}

			@Override
			public void beforeExecuteAction() {
				System.out.println("NoodleServiceCallbackExtend -> beforeExecuteAction...");
			}

			@Override
			public boolean beforeExecuteActionCheckInTransaction() {
				System.out.println("NoodleServiceCallbackExtend -> beforeExecuteActionCheckInTransaction...");
				return true;
			}

			@Override
			public void beforeExecuteActionInTransaction() {
				System.out.println("NoodleServiceCallbackExtend -> beforeExecuteActionInTransaction...");
			}

			@Override
			public void afterExecuteActionInTransaction(boolean isSuccess, List<TestModel> result, Exception e) {
				System.out.println("NoodleServiceCallbackExtend -> afterExecuteActionInTransaction...");
			}

			@Override
			public void afterExecuteAction(boolean isSuccess, List<TestModel> result, Exception e) {
				System.out.println("NoodleServiceCallbackExtend -> afterExecuteAction...");
			}
		});
		assertNotNull(testModelList);
	}
	
	class TestModel {
		
		private Integer id;
		private String name;
		
		public Integer getId() {
			return id;
		}
		public void setId(Integer id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public String toString() {
			return (new StringBuilder())
					.append("[id=")
					.append(id)
					.append(", name=")
					.append(name)
					.append("]")
					.toString();
		}
	}
}
