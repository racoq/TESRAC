/**
 * Scaffolding file used to store all the setups needed to run 
 * tests automatically generated by EvoSuite
 * Sat Mar 14 15:09:02 GMT 2020
 */

package org.apache.xml.security.stax.ext;

import org.evosuite.runtime.annotation.EvoSuiteClassExclude;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.After;
import org.junit.AfterClass;
import org.evosuite.runtime.sandbox.Sandbox;
import org.evosuite.runtime.sandbox.Sandbox.SandboxMode;

@EvoSuiteClassExclude
public class XMLSec_ESTest_scaffolding {

  @org.junit.Rule 
  public org.evosuite.runtime.vnet.NonFunctionalRequirementRule nfr = new org.evosuite.runtime.vnet.NonFunctionalRequirementRule();

  private static final java.util.Properties defaultProperties = (java.util.Properties) java.lang.System.getProperties().clone(); 

  private org.evosuite.runtime.thread.ThreadStopper threadStopper =  new org.evosuite.runtime.thread.ThreadStopper (org.evosuite.runtime.thread.KillSwitchHandler.getInstance(), 3000);


  @BeforeClass 
  public static void initEvoSuiteFramework() { 
    org.evosuite.runtime.RuntimeSettings.className = "org.apache.xml.security.stax.ext.XMLSec"; 
    org.evosuite.runtime.GuiSupport.initialize(); 
    org.evosuite.runtime.RuntimeSettings.maxNumberOfThreads = 100; 
    org.evosuite.runtime.RuntimeSettings.maxNumberOfIterationsPerLoop = 10000; 
    org.evosuite.runtime.RuntimeSettings.mockSystemIn = true; 
    org.evosuite.runtime.RuntimeSettings.sandboxMode = org.evosuite.runtime.sandbox.Sandbox.SandboxMode.RECOMMENDED; 
    org.evosuite.runtime.sandbox.Sandbox.initializeSecurityManagerForSUT(); 
    org.evosuite.runtime.classhandling.JDKClassResetter.init();
    setSystemProperties();
    initializeClasses();
    org.evosuite.runtime.Runtime.getInstance().resetRuntime(); 
  } 

  @AfterClass 
  public static void clearEvoSuiteFramework(){ 
    Sandbox.resetDefaultSecurityManager(); 
    java.lang.System.setProperties((java.util.Properties) defaultProperties.clone()); 
  } 

  @Before 
  public void initTestCase(){ 
    threadStopper.storeCurrentThreads();
    threadStopper.startRecordingTime();
    org.evosuite.runtime.jvm.ShutdownHookHandler.getInstance().initHandler(); 
    org.evosuite.runtime.sandbox.Sandbox.goingToExecuteSUTCode(); 
    setSystemProperties(); 
    org.evosuite.runtime.GuiSupport.setHeadless(); 
    org.evosuite.runtime.Runtime.getInstance().resetRuntime(); 
    org.evosuite.runtime.agent.InstrumentingAgent.activate(); 
  } 

  @After 
  public void doneWithTestCase(){ 
    threadStopper.killAndJoinClientThreads();
    org.evosuite.runtime.jvm.ShutdownHookHandler.getInstance().safeExecuteAddedHooks(); 
    org.evosuite.runtime.classhandling.JDKClassResetter.reset(); 
    resetClasses(); 
    org.evosuite.runtime.sandbox.Sandbox.doneWithExecutingSUTCode(); 
    org.evosuite.runtime.agent.InstrumentingAgent.deactivate(); 
    org.evosuite.runtime.GuiSupport.restoreHeadlessMode(); 
  } 

  public static void setSystemProperties() {
 
    java.lang.System.setProperties((java.util.Properties) defaultProperties.clone()); 
  }

  private static void initializeClasses() {
    org.evosuite.runtime.classhandling.ClassStateSupport.initializeClasses(XMLSec_ESTest_scaffolding.class.getClassLoader() ,
      "org.apache.xml.security.algorithms.JCEMapper$Algorithm",
      "org.apache.xml.security.algorithms.JCEMapper",
      "org.apache.xml.security.configuration.PropertiesType",
      "org.apache.xml.security.stax.config.SecurityHeaderHandlerMapper",
      "org.apache.xml.security.stax.ext.XMLSec",
      "org.apache.xml.security.configuration.HandlerType",
      "org.apache.xml.security.stax.config.ConfigurationProperties",
      "org.apache.xml.security.utils.JavaUtils",
      "org.apache.xml.security.configuration.AlgorithmType",
      "org.apache.xml.security.configuration.ResourceResolversType",
      "org.apache.xml.security.configuration.ResolverType",
      "org.apache.xml.security.configuration.JCEAlgorithmMappingsType",
      "org.apache.xml.security.utils.ClassLoaderUtils",
      "org.apache.xml.security.stax.config.XIncludeHandler",
      "org.apache.xml.security.configuration.SecurityHeaderHandlersType",
      "org.apache.xml.security.stax.config.Init",
      "org.apache.xml.security.stax.config.JCEAlgorithmMapper",
      "org.apache.xml.security.configuration.TransformAlgorithmType",
      "org.apache.xml.security.utils.I18n",
      "org.apache.xml.security.stax.ext.XMLSecurityConfigurationException",
      "org.apache.xml.security.exceptions.XMLSecurityException",
      "org.apache.xml.security.configuration.ObjectFactory",
      "org.apache.xml.security.configuration.ConfigurationType",
      "org.apache.xml.security.configuration.PropertyType",
      "org.apache.xml.security.configuration.TransformAlgorithmsType",
      "org.apache.xml.security.configuration.InOutAttrType"
    );
  } 

  private static void resetClasses() {
    org.evosuite.runtime.classhandling.ClassResetter.getInstance().setClassLoader(XMLSec_ESTest_scaffolding.class.getClassLoader()); 

    org.evosuite.runtime.classhandling.ClassStateSupport.resetClasses(
      "org.apache.xml.security.utils.ClassLoaderUtils",
      "org.apache.xml.security.stax.config.Init",
      "org.apache.xml.security.configuration.InOutAttrType",
      "org.apache.xml.security.stax.config.XIncludeHandler",
      "org.apache.xml.security.configuration.ConfigurationType",
      "org.apache.xml.security.configuration.PropertiesType",
      "org.apache.xml.security.configuration.PropertyType",
      "org.apache.xml.security.configuration.SecurityHeaderHandlersType",
      "org.apache.xml.security.configuration.TransformAlgorithmsType",
      "org.apache.xml.security.configuration.TransformAlgorithmType",
      "org.apache.xml.security.configuration.JCEAlgorithmMappingsType",
      "org.apache.xml.security.configuration.AlgorithmType",
      "org.apache.xml.security.configuration.ResourceResolversType",
      "org.apache.xml.security.configuration.ResolverType",
      "org.apache.xml.security.stax.config.ConfigurationProperties",
      "org.apache.xml.security.stax.config.SecurityHeaderHandlerMapper",
      "org.apache.xml.security.algorithms.JCEMapper",
      "org.apache.xml.security.stax.config.JCEAlgorithmMapper",
      "org.apache.xml.security.algorithms.JCEMapper$Algorithm",
      "org.apache.xml.security.utils.JavaUtils",
      "org.apache.xml.security.utils.I18n",
      "org.apache.xml.security.exceptions.XMLSecurityException",
      "org.apache.xml.security.stax.ext.XMLSecurityConfigurationException",
      "org.apache.xml.security.stax.ext.XMLSec"
    );
  }
}
