variable "project_id" {
    description = "The GCP project id"
    type = string
}
 
variable "region" {
    default = "us-central1"
    description = "GCP region"
    type = string
}
 
variable "namespace" {
    description = "The project namespace to use for unique resource naming"
    type = string
}

variable "gke_name" {
    default = "my-test-cluster"
    description = "Goole Kubernetes Engine cluster name"
    type = string
}

variable "gke_namespace" {
    default = "imdb"
    description = "Goole Kubernetes Engine cluster's namespace'"
    type = string
}
