# How to spin up Github -> CircleCI -> GCP Kubernetes Pipeline

* Create your own project to Google Cloude Platform (GCP) 
* Create container registery
    * Image name: imdb-list-analyzer
* Create IAM account
    * Name: ci-user
    * Add roles: Kubernetes Engine Admin, Storage Admin
* Spin up Kubernetes cluster in GCP 
    * Add imdb namespace using kubectl
    
```
kubectl create namespace imdb
```
    
* Add following environment variables to CircleCI

```
CI_ACCOUNT_TOKEN_BASE64
CI_USER
CLUSTER	
CLUSTER_ZON	
GCP_PROJECT
```

* Fix the errors I forgot to mention
* Happy K8ing!