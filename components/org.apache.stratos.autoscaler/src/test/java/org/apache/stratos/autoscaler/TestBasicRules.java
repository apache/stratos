//package org.apache.stratos.autoscaler;
//
//import static org.junit.Assert.assertEquals;
//
//import org.drools.KnowledgeBase;
//import org.drools.KnowledgeBaseFactory;
//import org.drools.builder.KnowledgeBuilder;
//import org.drools.builder.KnowledgeBuilderError;
//import org.drools.builder.KnowledgeBuilderErrors;
//import org.drools.builder.KnowledgeBuilderFactory;
//import org.drools.builder.ResourceType;
//import org.drools.io.ResourceFactory;
//import org.drools.logger.KnowledgeRuntimeLogger;
//import org.drools.logger.KnowledgeRuntimeLoggerFactory;
//import org.drools.runtime.StatefulKnowledgeSession;
//import org.junit.Before;
//import org.junit.Test;
//
//public class TestBasicRules {
//
// private KnowledgeBase kbase;
//
// @Before
// public void setup() {
//  KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
//  kbuilder.add(ResourceFactory.newClassPathResource("basic.drl"), ResourceType.DRL);
//  KnowledgeBuilderErrors errors = kbuilder.getErrors();
//  if (errors.size() > 0) {
//   for (KnowledgeBuilderError error: errors) {
//    System.err.println(error);
//   }
//   throw new IllegalArgumentException("Could not parse knowledge.");
//  }
//  kbase = KnowledgeBaseFactory.newKnowledgeBase();
//  kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
//
//
// }
//
// @Test
// public void testBasic() {
//
//  StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();
//  KnowledgeRuntimeLogger logger = KnowledgeRuntimeLoggerFactory.newFileLogger(ksession, "test");
//
//  RoolVO vo = new RoolVO();
//  vo.setStringValue("Learning to drool");
//  vo.setBooleanValue(true);
//  ksession.insert(vo);
//  ksession.fireAllRules();
//  for (Object o: ksession.getObjects()) {
//   if(o instanceof RoolVO) {
//    assertEquals("Done.", ((RoolVO) o).getStringValue());
//   }
//  }
//  logger.close();
//
// }
//
//}