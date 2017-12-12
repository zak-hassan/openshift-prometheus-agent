package com.radanalytics.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.ImageSignatureBuilder;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;

public class PrometheusDeployer {
	private NamespacedOpenShiftClient client;
	private String NAMESPACE;

	public PrometheusDeployer(String namespace) {
		OpenShiftConfig config = new OpenShiftConfigBuilder().withMasterUrl("https://127.0.0.1:8443")
				.withTrustCerts(true).withUsername("developer").withPassword("developer").build();

		this.client = new DefaultOpenShiftClient(config);
		this.NAMESPACE = namespace;
	}

	public void getAllServices() {
		client.services().inNamespace(NAMESPACE).list().getItems()
		.forEach(service -> 	System.out.println(service));
	}

	public void getLables() {
		client.services().inNamespace(NAMESPACE).list().getItems()
				.forEach(service -> System.out.println(service.getMetadata().getLabels()));
	}

	public List<String> getServiceByLabel(String labelName) {
		List<String> list = new ArrayList<>();
		client.services().inNamespace(NAMESPACE).list().getItems()
				.forEach(service ->{ 
					Map<String, String> labels=service.getMetadata().getLabels();
					try {
						if(service.getMetadata().getLabels().containsKey("metrics"))
							list.add(service.getMetadata().getName());	
					} catch (Exception e) {

					}
				});
		return list;
	}

	public void createConfigMap(String name, List<String> services) {
		  ConfigMap map = buildConfigMap(name, services);
		  client.configMaps().createOrReplace(map);
		  System.out.println("Created new configMap");
	  }

	private ConfigMap buildConfigMap(String name, List<String> services) {
		String promcfg = "\n" + 
				  	  "    global:\n" + 
					  "      scrape_interval:     5s\n" + 
					  "      evaluation_interval: 5s\n" + 
					  "\n" + 
					  "    scrape_configs:\n" + 
					  "      - job_name: 'prometheus'\n" + 
					  "        static_configs:\n" + 
					  "          - targets: ['localhost:9090']\n" ;
		  for (String svc : services) {
			 promcfg+= "      - job_name: '"+svc+"'\n" + 
					   "        static_configs:\n" + 
					   "          - targets: ['"+svc+":7777']\n" ; 
		}
		
		ConfigMap map=new ConfigMapBuilder().withNewMetadata().withName(name)
				  .endMetadata()
				  .addToData("prometheus.yml",
						  promcfg)
				  .build();
		return map;
	}
	
	public void createPrometheusDeployment() {
 
		// Creating prometheus service
		client.services().inNamespace(NAMESPACE).createOrReplace(new ServiceBuilder()
				.withNewMetadata()
				.withName("prom-service")
				.endMetadata()
				.withNewSpec()
				.addNewPort().withNewTargetPort(9090)
				.withPort(9090)
				.withProtocol("TCP")
				.withName("9090-tcp")
				.endPort()
				.withSelector(new HashMap<String,String>() {{
					put("app", "prometheus");
					 
				}})
				.endSpec()
				.build());
		
		 client.configMaps().inNamespace(NAMESPACE).withName("prometheus-cfg");

		//TODO: Mount configmap in pvc
		Deployment dc= new DeploymentBuilder()
				 .withNewMetadata()
		          .withName("prometheus-deploy")
		          .endMetadata()
		          .withNewSpec()
		          .withReplicas(1)
		          .withNewTemplate()
		          .withNewMetadata()
		          .addToLabels("app", "prometheus")
		          .endMetadata()
		          .withNewSpec()
		          .addNewVolume()
		          	.withName("prometheus-store")
		          	.withNewPersistentVolumeClaim()
		          	.withClaimName("prometheus-store")
		          	.endPersistentVolumeClaim()
		          .endVolume()
		          .addNewVolume()
		          	.withName("prom-config")
		          	.withConfigMap(new ConfigMapVolumeSourceBuilder().withName("prometheus-cfg").build())
		          .endVolume()
		          .addNewContainer()
		          .withName("prometheus")
		          .withImage("docker.io/prom/prometheus")
		          .addNewVolumeMount("/etc/prometheus","prom-config", false, null)
		          .addNewVolumeMount("/prometheus/", "prometheus-store", false, null)
		          .addNewPort()
		          .withContainerPort(9090)
		          .endPort()
		          .endContainer()
		          .endSpec()
		          .endTemplate()
		          .endSpec()
		          .build();
		
        Deployment deployment = client.extensions().deployments().inNamespace(NAMESPACE).create(dc);

		//client.deployments().createOrReplace(dc);
		System.out.println("Created Deployment: "+deployment);
	}

}
