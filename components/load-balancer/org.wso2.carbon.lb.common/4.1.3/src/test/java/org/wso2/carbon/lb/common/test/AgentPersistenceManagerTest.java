//package org.wso2.carbon.lb.common.test;
//
//import java.sql.SQLException;
//
//import org.testng.Assert;
//import org.testng.annotations.Test;
//import org.testng.annotations.BeforeMethod;
//import org.testng.annotations.AfterMethod;
//import org.testng.annotations.DataProvider;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.AfterClass;
//import org.testng.annotations.BeforeTest;
//import org.testng.annotations.AfterTest;
//import org.testng.annotations.BeforeSuite;
//import org.testng.annotations.AfterSuite;
//import org.wso2.carbon.lb.common.dto.Bridge;
//import org.wso2.carbon.lb.common.dto.HostMachine;
//import org.wso2.carbon.lb.common.dto.Zone;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.wso2.carbon.lb.common.persistence.AgentPersistenceManager;
//
///**
// * This test class is there to test the database access methods. There is a testng.xml to run these
// * tests as a suit. It's located at test/resources. Currently it is disabled by commenting the class
// * element at the xml file. Mysql driver dependancy at pom.xml is also disabled and it should be
// * uncommented to support database activities.
// */
//public class AgentPersistenceManagerTest {
//	
//	private static final Log log = LogFactory.getLog(AgentPersistenceManagerTest.class);;
//	
////  @Test(dataProvider = "dp")
////  public void f(Integer n, String s) {
////  }
//  @BeforeMethod
//  public void beforeMethod() {
//  }
//
//  @AfterMethod
//  public void afterMethod() {
//  }
//
//
//  @DataProvider
//  public Object[][] dataProviderForDAO() {
//  
//	  String epr = "epr4";
//	  String zoneName = "zone1";
//      Bridge[] bridges = new Bridge[3];
//      bridges[0] = new Bridge();
//      bridges[1] = new Bridge();
//      bridges[2] = new Bridge();
//
//      bridges[0].setBridgeIp("168.192.1.0");
//      bridges[0].setAvailable(true);
//      bridges[0].setCurrentCountIps(0);
//      bridges[0].setMaximumCountIps(100);
//      bridges[0].setNetGateway("net_gateway");
//      bridges[0].setNetMask("net_mask");
//      bridges[0].setHostMachine(epr);
//
//      bridges[1].setBridgeIp("168.192.2.0");
//      bridges[1].setAvailable(true);
//      bridges[1].setCurrentCountIps(0);
//      bridges[1].setMaximumCountIps(100);
//      bridges[1].setNetGateway("net_gateway");
//      bridges[1].setNetMask("net_mask");
//      bridges[1].setHostMachine(epr);
//
//      bridges[2].setBridgeIp("168.192.3.0");
//      bridges[2].setAvailable(true);
//      bridges[2].setCurrentCountIps(0);
//      bridges[2].setMaximumCountIps(100);
//      bridges[2].setNetGateway("net_gateway");
//      bridges[2].setNetMask("net_mask");
//      bridges[2].setHostMachine(epr);
//
//
//      HostMachine hostMachine = new HostMachine();
//      hostMachine.setAvailable(true);
//      hostMachine.setContainerRoot("ContainerRoot");
//      hostMachine.setIp("ip");
//      hostMachine.setZone(zoneName);
//      hostMachine.setBridges(bridges);
//      hostMachine.setEpr(epr);
//      
//      String[] domains = new String[2];
//      domains[0] = "domian1";
//      domains[1] = "domain2";
//      
//      
//    return new Object[][] {
//      new Object[] { hostMachine, domains },
//    };
//  }
//  @BeforeClass
//  public void beforeClass() {
//  }
//
//  @AfterClass
//  public void afterClass() {
//  }
//
//  @BeforeTest
//  public void beforeTest() {
//  }
//
//  @AfterTest
//  public void afterTest() {
//  }
//
//  @BeforeSuite
//  public void beforeSuite() {
//  }
//
//  @AfterSuite
//  public void afterSuite() {
//  }
//
//
////  @Test
////  public void addContainer() {
////    throw new RuntimeException("Test not implemented");
////  }
//  
//  @Test(dataProvider="dataProviderForDAO")
//  public void addZone(HostMachine hostMachine, String[] domains)  {
//	  AgentPersistenceManager agentPersistenceManager = AgentPersistenceManager.getPersistenceManager();
//	  boolean isAdded = false;
//	  boolean zoneExists = false;
//      try {
//    	  zoneExists = agentPersistenceManager.isZoneExist(hostMachine.getZone());
//		if (!zoneExists) {
//		      String msg = "Zone does not exists ";
//		      System.out.println(msg);
//		      Zone zone = new Zone();
//		      zone.setName(hostMachine.getZone());
//		      zone.setAvailable(true);
//		      isAdded = agentPersistenceManager.addZone(zone, domains);
//		  } else {
//		      String msg = "Zone exist";
//		      System.out.println(msg);
//		  }
//	} catch (SQLException e) {
//		  String msg = "Test failure while adding zone";
//		  log.error(msg);
//	}
//      System.out.println("zone added " + isAdded);
//      if(!zoneExists){
//    	  Assert.assertEquals(isAdded, true);
//      }else{
//    	  Assert.assertEquals(isAdded, false );
//      }
//  }
//  
//  @Test(dataProvider="dataProviderForDAO")
//  public void addHostMachine(HostMachine hostMachine, String[] domains)  {
//
//
//
//	  AgentPersistenceManager agentPersistenceManager = AgentPersistenceManager.getPersistenceManager();
//      boolean isAdded = false;
//	  try {
//		  isAdded = agentPersistenceManager.addHostMachine(hostMachine, domains);
//	  } catch (SQLException e) {
//		  String msg = "Test failure while adding host machine";
//		  log.error(msg);
//	  }
//      System.out.println(" HM added " + isAdded);
//      Assert.assertEquals(isAdded, true);
//
//  }
//
//    @Test(dataProvider="dataProviderForDAO")
//    public void deleteHostMachine(HostMachine hostMachine) {
//        AgentPersistenceManager agentPersistenceManager = AgentPersistenceManager.getPersistenceManager();
//        boolean isDeleted = false;
//
//        try {
//            isDeleted = agentPersistenceManager.deleteHostMachine(hostMachine.getEpr());
//        } catch (SQLException e) {
//            String msg = "Test failure while deleting host machine";
//            log.error(msg);
//        }
//
//    }
////
////  @Test
////  public void addInstance() {
////    throw new RuntimeException("Test not implemented");
////  }
////
//  
////
////  @Test
////  public void changeContainerState() {
////    throw new RuntimeException("Test not implemented");
////  }
////
////  @Test
////  public void changeHostMachineAvailability() {
////    throw new RuntimeException("Test not implemented");
////  }
////
////  @Test
////  public void deleteContainer() {
////    throw new RuntimeException("Test not implemented");
////  }
//
////
////  @Test
////  public void getPersistenceManager() {
////    throw new RuntimeException("Test not implemented");
////  }
////
////  @Test
////  public void isDomainExist() {
////    throw new RuntimeException("Test not implemented");
////  }
////
////  @Test
////  public void isHostMachineExist() {
////    throw new RuntimeException("Test not implemented");
////  }
////
////  @Test
////  public void isHostMachinesAvailableInDomain() {
////    throw new RuntimeException("Test not implemented");
////  }
////
////  @Test
////  public void isZoneExist() {
////    throw new RuntimeException("Test not implemented");
////  }
////
////  @Test
////  public void retrieveAgentToContainerRootMap() {
////    throw new RuntimeException("Test not implemented");
////  }
////
////  @Test
////  public void retrieveAvailableContainerInformation() {
////    throw new RuntimeException("Test not implemented");
////  }
////
////  @Test
////  public void retrieveContainerIdToAgentMap() {
////    throw new RuntimeException("Test not implemented");
////  }
////
////  @Test
////  public void retrieveDomainToInstanceIdsMap() {
////    throw new RuntimeException("Test not implemented");
////  }
////
////  @Test
////  public void retrieveInstanceIdToAdapterMap() {
////    throw new RuntimeException("Test not implemented");
////  }
////
////  @Test
////  public void updateDomainConfigs() {
////    throw new RuntimeException("Test not implemented");
////  }
//}