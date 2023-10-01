# Kafka Kubernetes Demo

Spring Boot application used to demonstrate deployment to Kubernetes along with Kafka and Zookeeper.

This repo accompanies the following series of articles on Kafka on Kubernetes:

- [Kafka on Kubernetes - Part 1: Introduction to Kubernetes](https://www.lydtechconsulting.com/blog-kafka-kubernetes-demo-pt1.html):  Provides an overview of Kubernetes including the key components that must be understood in order to deploy applications and expose them to external sources.  Explains how minikube enables running and testing Kubernetes deployments locally, and the use of kubectl to interact with the Kubenetes cluster.
- [Kafka on Kubernetes - Part 2: Deploying Kafka](https://www.lydtechconsulting.com/blog-kafka-kubernetes-demo-pt2.html):  Walks through deploying Kafka and Zookeeper to Kubernetes, and explains the kubectl commands used to query the state of the deployment.  Steps through sending and receiving events to the deployed Kafka instance using the Kafka command line tools.
- [Kafka on Kubernetes - Part 3: Spring Boot Demo](https://www.lydtechconsulting.com/blog-kafka-kubernetes-demo-pt3.html):  Walks through deploying a Spring Boot application to Kubernetes.  The application connects to the deployed Kafka to consume and produce events.  It provides a REST API enabling a client to trigger sending events to Kafka, and the steps to expose this to an external source are described.

## Overview

The application provides a REST endpoint that accepts a request to trigger sending events.  The number of events to produce can be specified.

The application also consumes an event that triggers sending outbound events.  The number of events to produce can be specified.

Kafka, Zookeeper, and the demo Spring Boot application are deployed as Docker containers to minikube, which is an implementation of Kubernetes that is great for local testing.  Interacting with the application and Kafka are demonstrated by calling the REST endpoint to trigger emitting events, and by sending events to Kafka and receiving the emitted events from the application via the Kafka commandline tools. 

## Walkthrough

### Install & Run minikube

Select the version of minikube suitable for the OS following step 1 here:

https://minikube.sigs.k8s.io/docs/start/

e.g. for macOS ARM64:
```
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-darwin-arm64
sudo install minikube-darwin-arm64 /usr/local/bin/minikube
```

First start Docker, then start the cluster:
```
minikube start
```

View cluster state in the browser:
```
minikube dashboard
```

At this point there is nothing deployed to view.

Note that minikube can be stopped with the following:
```
minikube stop
```

### Create a Namespace

A namespace can be used rather than using the default one to isolate the resources that are created.

A Kubernetes namespace template is provided in the `/resources` directory in the root of the project.  To create the namespace called `demo` run:
```
kubectl create -f ./resources/demo-namespace.yml
```
The response confirms the namespace is created:
```
namespace/demo created
```

View namespaces:
```
kubectl get namespaces
```

Individual commands can now be directed at the `demo` namespace by adding the option `--namespace=demo`.  Alternatively the namespace can be configured as the default by running:
```
kubectl config set-context $(kubectl config current-context) --namespace=demo
```

To delete the namespace, run:
```
kubectl delete namespace demo
```

### Deploy Kafka and Zookeeper

Kubernetes deployment manifests are provided for Kafka and Zookeeper in the `/resources` directory.  To create the pods for Kafka and Zookeeper run:
```
kubectl create -f ./resources/zookeeper.yml
kubectl create -f ./resources/kafka.yml
```

Kafka and Zookeeper pods can now be viewed in the minikube dashboard.  Alternatively from the command line:
```
kubectl get pods
```
This provides the pod names which can be used for later commands.

To view the deployments:
```
kubectl get deployments
```

To view the services:
```
kubectl get services
```

View the logs (with the pod name obtained via `kubectrl get pod`):
```
kubectl logs kafka-deployment-57f8cd77f6-v2gts
```

To delete all pods:
```
kubectl delete --all pods
```
Note that as the pods are managed by the deployment, as the pod counts drop below the required replica count, they are recreated.

To permanently delete the pods, the deployment must be deleted:
```
kubectl delete --all deployments
```

### Create Kafka Topics

The Spring Boot demo application uses two topics, `demo-inbound-topic` and `demo-outbound-topic`.  These should be created in advance.

First jump onto the Kafka pod: 
```
kubectl exec -it kafka-deployment-57f8cd77f6-v2gts -- /bin/bash
```
Then execute the following commands to create the topics:
```
kafka-topics --bootstrap-server localhost:9092 --create --topic demo-inbound-topic --replication-factor 1 --partitions 3
kafka-topics --bootstrap-server localhost:9092 --create --topic demo-outbound-topic --replication-factor 1 --partitions 3
```
The response should show the topics created:
```
Created topic demo-inbound-topic.
Created topic demo-outbound-topic.
```

### Build and Deploy the Spring Boot application

Ensure the bootstrap-server URL in the Spring Boot application properties, located in `src/main/resources/application.yml`, points to the Kafka service:
```
kafka:
    bootstrap-servers: kafka-service:9092
```
The deployment manifest for the Spring Boot application is located in `resources/demo-application.yml`.

The Spring Boot application is built using maven:
```
mvn clean install
```

Kubernetes uses its own local Docker registry that is not connected to the Docker registry on the local machine.  As the Spring Boot application Docker image will be built locally and is not available in the public Docker registry, the `imagePullPolicy` is set to `Never` in the `resources/demo-application.yml`.  The following minikube command is then required in order to output the environment variables that are needed to point the local Docker daemon to the minikube internal Docker registry. 
```
eval $(minikube -p minikube docker-env)
```
This command must be run in any new terminal window opened to take effect for the next Docker command.

Then the Docker image can be built for the application.  This will installed into the minikube registry, ready for deployment to Kubernetes:
```
docker build -t kube/kafka-kubernetes-demo:latest .
```

Finally the Spring Boot pod can be created and deployed, and is viewable in the minikube dashboard:
```
kubectl create -f ./resources/demo-application.yml
```
Console output shows the deployment and service are created:
```
deployment.apps/kafka-kubernetes-demo created
service/kafka-kubernetes-demo-service created
```

`kafka-kubernetes-demo-service` is the name defined in the service metadata name from the `./resources/demo-application.yml`:

The job can be removed and recreated if necessary, by first deleting it:
```
kubectl delete -f ./resources/demo-application.yml
```

Assign an external port by starting a tunnel (the namespace will be required here):
```
minikube service kafka-kubernetes-demo-service --namespace demo
```
This will open the root page of the Spring Boot application in the browser, which displays `Spring Boot Demo`, verifying the application has started successfully.

Alternatively to get the URL without opening the browser, use the `--url` option:
```
minikube service kafka-kubernetes-demo-service --namespace demo --url
```

### Use Spring Boot Application REST API

With the tunnel open, hit the version endpoint on the Spring Boot application using `curl`.
```
curl -X GET http://localhost:53298/v1/demo/version
```
This should return `v1`

Send a REST request to trigger the application into sending events to Kafka:
```
curl -v -d '{"numberOfEvents":3}' -H "Content-Type: application/json" http://localhost:53298/v1/demo/trigger
```

Check Spring Boot application logs to confirm events have been sent:
```
kubectl logs kafka-kubernetes-demo-745d47966c-tmvqq
```
```
13:23:34.320 INFO  d.k.KafkaDemoApplication - Started KafkaDemoApplication in 1.423 seconds (process running for 1.648)
14:14:43.734 INFO  d.k.s.DemoService - Sending 3 events
14:14:45.318 INFO  d.k.s.DemoService - Total events sent: 3
```

### Consume and Produce Events on the Command Line

Jump onto the Kafka pod:
```
kubectl exec -it kafka-deployment-57f8cd77f6-v2gts -- /bin/bash
```

Consume the events already produced from the beginning of the `demo-outbound-topic`:
```
kafka-console-consumer --bootstrap-server localhost:9092 --topic demo-outbound-topic --from-beginning
```
The events, populated with randomised names, are output:
```
{"firstName":"ZrdOwISesV","middleName":"FXjMEVAyZf","lastName":"WRfyXxHSGB"}
{"firstName":"qTZORIeXpU","middleName":"qCurtqohni","lastName":"mWUIEeTSQk"}
{"firstName":"xvlSzBpKVZ","middleName":"HjNQjirBcu","lastName":"MwzFClWjHV"}
```

Trigger sending new events from the application by submitting a request to the `demo-inbound-topic`, and observe the new events being emitted.  Use a new terminal window in order to leave the console-consumer running.
```
kafka-console-producer --broker-list localhost:9092 --topic demo-inbound-topic 
{"numberOfEvents":2}
```

This should be reflected in the logs:
```
14:25:09.789 INFO  d.k.c.KafkaDemoConsumer - Received message - event: DemoInboundEvent(numberOfEvents=2)
14:25:09.789 INFO  d.k.s.DemoService - Sending 2 events
14:25:09.818 INFO  d.k.s.DemoService - Total events sent: 2
```
### Enable Ingress

https://kubernetes.io/docs/tasks/access-application-cluster/ingress-minikube/

```
minikube addons enable ingress
```
Verify running with:
```
kubectl get pods -n ingress-nginx
```
Create the ingress for the `kafka-kubernetes-demo-service`:
```
kubectl apply -f ./resources/demo-ingress.yml
```
The output on the console should show this as created:
```
ingress.networking.k8s.io/demo-ingress created
```
The ingress has been created with host `k8s.springboot.demo`.  To view the ingress:
```
kubectl get ingress
```

The ingress can be deleted with the following:
```
kubectl delete ingress demo-ingress
```

Update the `/etc/hosts` file, adding the following line (correct for MacOS ARM):
```
127.0.0.1 k8s.springboot.demo
```
For other operating systems add the minikube IP to the `/etc/hosts` file instead:
```
192.168.49.2 k8s.springboot.demo
```
Now start the minikube tunnel:
```
minikube tunnel
```
Open the following address in the browser to see the landing page:
```
http://k8s.springboot.demo
```
Alternatively verify by getting the application version using `curl`:
```
curl -X GET http://k8s.springboot.demo/v1/demo/version
```
This returns the String `v1`.

## References

- [Kubernetes Tutorial for Beginners](https://www.youtube.com/watch?v=X48VuDVv0do)
- [minikube Documentation](https://minikube.sigs.k8s.io/docs/)
- [Setting up Kafka on Kubernetes - an easy way](https://blog.datumo.io/setting-up-kafka-on-kubernetes-an-easy-way-26ae150b9ca8)
- [How to Run Locally Built Docker Images in Kubernetes](https://medium.com/swlh/how-to-run-locally-built-docker-images-in-kubernetes-b28fbc32cc1d)
- [How to Deploy Docker Containers to the Kubernetes Cluster using Kubernetes CLI](https://sweetcode.io/how-to-deploy-docker-containers-to-the-kubernetes-cluster-using-kubernetes-cli/)
- [Set up Ingress on Minikube with the NGINX Ingress Controller](https://kubernetes.io/docs/tasks/access-application-cluster/ingress-minikube/)
