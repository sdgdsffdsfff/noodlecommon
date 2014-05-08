package org.fengling.noodlecommon.dbrwseparate.loadbalancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LoadBalancerManager {
	
	private final Log logger = LogFactory.getLog(LoadBalancerManager.class);
		
	private CopyOnWriteArrayList<DataSourceModel> dataSourcesList = new CopyOnWriteArrayList<DataSourceModel>();
	private CopyOnWriteArrayList<DataSourceModel> deadSourcesList = new CopyOnWriteArrayList<DataSourceModel>();

	private CopyOnWriteArrayList<DataSourceModel> dataSourcesSelectList = new CopyOnWriteArrayList<DataSourceModel>();

	private Map<String, Integer> weightMap = new ConcurrentHashMap<String, Integer>();
	
	private int totalFailureCount = 3;
	private int totalRiseCount = 2;
	private long interTime = 2000;
	private String detectingSql = null;
	
	
	private Map<String, DataSource> initDataSourceMap;
	private Map<String, Integer> initWeightMap;

	public void init() {
		
		if (initWeightMap != null) {			
			for (Map.Entry<String, Integer> entry : initWeightMap.entrySet()) {
				weightMap.put(entry.getKey(), entry.getValue());
			}
		}

		for (Map.Entry<String, DataSource> entry : initDataSourceMap.entrySet()) {
			DataSourceModel dataSourceModel = new DataSourceModel();
			dataSourceModel.setDataSource(entry.getValue());
			dataSourceModel.setDataSourceType(entry.getKey());
			dataSourcesList.add(dataSourceModel);
			addDataSourcesSelectList(dataSourceModel);
		}
		

		Thread dataSourceBeatAlive = new Thread(new HeartBeatAliveTasksScan());
		dataSourceBeatAlive.setDaemon(true);
		dataSourceBeatAlive.setName("dataSourceHeartBeatAlive");
		dataSourceBeatAlive.start();

		Thread dataSourceDeadHeartBeat = new Thread(new HeartBeatDeadTasksScan());
		dataSourceDeadHeartBeat.setDaemon(true);
		dataSourceDeadHeartBeat.setName("dataSourceDeadHeartBeat");
		dataSourceDeadHeartBeat.start();
	}
	
	private DataSourceModel getAliveDataSourceFromList(List<?> dataSourcesList) {
		
		DataSourceModel dataSourceModel = null;
		
		int dataSourcesListSize = 0;
		
		while ((dataSourcesListSize = dataSourcesList.size()) > 0) {
			
			int index = (int) Math.round(Math.random() * (dataSourcesListSize - 1));
			try {
				dataSourceModel = (DataSourceModel) dataSourcesList.get(index);
				break;
			} catch (ArrayIndexOutOfBoundsException e) {
				continue;
			}
		}
		
		return dataSourceModel;
	}
	
	public DataSourceModel getAliveDataSource() {
		return getAliveDataSourceFromList(dataSourcesSelectList);
	}
	
	public DataSourceModel getOtherAliveDataSource(List<DataSourceModel> DataSourceModelList) {
		
		List<Object> otherDataSourcesSelectList = new LinkedList<Object>();
		Collections.addAll(otherDataSourcesSelectList, dataSourcesSelectList.toArray());
		for (DataSourceModel dataSourceModelIt : DataSourceModelList) {
			while(otherDataSourcesSelectList.remove(dataSourceModelIt));
		}
		
		return getAliveDataSourceFromList(otherDataSourcesSelectList);
	}
	
	private void addDataSourcesSelectList(DataSourceModel dataSourceModel) {
		if (weightMap.containsKey(dataSourceModel.getDataSourceType())) {
			int count = weightMap.get(dataSourceModel.getDataSourceType());
			if (count > 0) {
				List<DataSourceModel> dataSourceModelList = new ArrayList<DataSourceModel>(count);
				for (int i=0; i<count; i++) {
					dataSourceModelList.add(dataSourceModel);
				}
				dataSourcesSelectList.addAll(dataSourceModelList);
			} else {
				dataSourcesSelectList.add(dataSourceModel);
			}
		} else {
			dataSourcesSelectList.add(dataSourceModel);
		}
	}
	
	private void removeDataSourcesSelectList(DataSourceModel dataSourceModel) {
		if (weightMap.containsKey(dataSourceModel.getDataSourceType())) {
			int count = weightMap.get(dataSourceModel.getDataSourceType());
			if (count > 0) {
				List<DataSourceModel> dataSourceModelList = new ArrayList<DataSourceModel>(count);
				for (int i=0; i<count; i++) {
					dataSourceModelList.add(dataSourceModel);
				}
				dataSourcesSelectList.removeAll(dataSourceModelList);
			} else {
				dataSourcesSelectList.remove(dataSourceModel);
			}
		} else {
			dataSourcesSelectList.remove(dataSourceModel);
		}
	}
	
	class HeartBeatAliveTasksScan implements Runnable {

		public void run() {
			while (true) {
				
				for (DataSourceModel dataSourceModel : dataSourcesList) {
					boolean status = DataSourceCheckAlive.CheckAliveDataSource(dataSourceModel.getDataSource(), detectingSql);
					AtomicInteger failureCount = dataSourceModel.getFailureCount();
					if (!status) {
						if (failureCount.incrementAndGet() > totalFailureCount) {
							removeDataSourcesSelectList(dataSourceModel);
							dataSourcesList.remove(dataSourceModel);
							failureCount.set(0);
							deadSourcesList.add(dataSourceModel);
							if (logger.isErrorEnabled()) {
								logger.error("HeartBeatAliveTasksScan -> run -> DataSource dead and remove dataSourcesMap, DataSource: " + dataSourceModel.getDataSourceType());								
							}
						} 
					} else {
						failureCount.set(0);
					}
				}

				try {
					Thread.sleep(interTime);
				} catch (InterruptedException e) {
					if (logger.isErrorEnabled()) {						
						logger.error("HeartBeatAliveTasksScan -> run -> HeartBeatAliveTasksScan InterruptedException, Exception: " + e);
					}
				}
			}
		}
	}

	class HeartBeatDeadTasksScan implements Runnable {

		public void run() {
			while (true) {
				
				for (DataSourceModel dataSourceModel : deadSourcesList) {
					boolean status = DataSourceCheckAlive.CheckAliveDataSource(dataSourceModel.getDataSource(), detectingSql);
					AtomicInteger riseCount = dataSourceModel.getRiseCount();
					if (status) {
						if (riseCount.incrementAndGet() >= totalRiseCount) {
							deadSourcesList.remove(dataSourceModel);
							riseCount.set(0);
							dataSourcesList.add(dataSourceModel);
							addDataSourcesSelectList(dataSourceModel);
							if (logger.isInfoEnabled()) {
								logger.info("HeartBeatAliveTasksScan -> run -> DataSource alive and remove deadSourcesMap, DataSource: " + dataSourceModel.getDataSourceType());								
							}
						}
					} else {
						riseCount.set(0);
					}
				}
				
				try {
					Thread.sleep(interTime);
				} catch (InterruptedException e) {
					if (logger.isErrorEnabled()) {						
						logger.error("HeartBeatAliveTasksScan -> run -> HeartBeatDeadTasksScan InterruptedException, Exception: " + e);
					}
				}
			}
		}
	}

	public int getTotalFailureCount() {
		return totalFailureCount;
	}

	public void setTotalFailureCount(int totalFailureCount) {
		this.totalFailureCount = totalFailureCount;
	}

	public int getTotalRiseCount() {
		return totalRiseCount;
	}

	public void setTotalRiseCount(int totalRiseCount) {
		this.totalRiseCount = totalRiseCount;
	}

	public String getDetectingSql() {
		return detectingSql;
	}

	public void setDetectingSql(String detectingSql) {
		this.detectingSql = detectingSql;
	}

	public long getInterTime() {
		return interTime;
	}

	public void setInterTime(long interTime) {
		this.interTime = interTime;
	}

	public Map<String, DataSource> getInitDataSourceMap() {
		return initDataSourceMap;
	}

	public void setInitDataSourceMap(Map<String, DataSource> initDataSourceMap) {
		this.initDataSourceMap = initDataSourceMap;
	}

	public Map<String, Integer> getInitWeightMap() {
		return initWeightMap;
	}

	public void setInitWeightMap(Map<String, Integer> initWeightMap) {
		this.initWeightMap = initWeightMap;
	}
}
