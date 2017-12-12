package com.radanalytics.monitoring;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;

public class App 
{
    public static void main( String[] args )
    {
        PrometheusDeployer prom= new PrometheusDeployer("testing");
//        System.out.println("~~~~~~~~~~Services~~~~~");	
//        prom.getAllServices();
//        System.out.println("~~~~~~~~~~LABELS~~~~~");	
//        prom.getLables();
//      System.out.println(prom.getServiceByLabel("metrics"));
        prom.createConfigMap("prometheus-cfg",prom.getServiceByLabel("metrics"));
        prom.createPrometheusDeployment();
    }
}
