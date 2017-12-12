

# oc cluster up
#
# oc new-project prometheus
#
# oc delete all --all
 oc create -f https://raw.githubusercontent.com/OpenShiftDemos/grafana-openshift/master/openshift-grafana-template.yaml
 oc new-app grafana-openshift
#
# 1. Setup Spark Cluster with Metrics enabled and services enabled.
# oc new-app -f spark-metrics-template.yaml
# //TODO: Create template that would do that with 4 workers and
#echo "sleep for 2 mins"
#sleep 1m

# 2. Setup prometheus.yml

PROM_SERVICES=$(oc get services |grep 9779 | awk '{print $1}' | xargs)
echo "" > prometheus.yml
cat <<'EOF' > prometheus.yml
global:
  scrape_interval:     5s
  evaluation_interval: 5s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
EOF

for word in $PROM_SERVICES
do
  echo "  - job_name: '$word'" >> prometheus.yml
  echo "    static_configs:">> prometheus.yml
  echo "      - targets: ['$word:9779']">> prometheus.yml
done

oc delete configmap prom-config-example && oc create configmap prom-config-example --from-file=prometheus.yml

 oc new-app prom/prometheus
 oc expose service prometheus

oc set volumes dc/prometheus --add --overwrite=true --name=config-volume --mount-path=/etc/prometheus/ -t configmap --configmap-name=prom-config-example
