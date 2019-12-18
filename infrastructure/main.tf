locals {
    services = [
        "container.googleapis.com",
        "sourcerepo.googleapis.com",
        "cloudbuild.googleapis.com"
    ]
}

resource "google_project_service" "enabled_service" {
  for_each = toset(local.services)
  project  = var.project_id
  service  = each.key
 
  provisioner "local-exec" {
    command = "sleep 60"
  }
 
  provisioner "local-exec" {
    when    = destroy
    command = "sleep 15"
  }
}

resource "google_container_cluster" "gke-cluster" {
  depends_on = [
    google_project_service.enabled_service["container.googleapis.com"]
  ]

  name               = var.gke_name
  location           = var.region
  initial_node_count = 1

  node_config {
    oauth_scopes = [
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
    ]

    metadata = {
      disable-legacy-endpoints = "true"
    }
  }

  timeouts {
    create = "30m"
    update = "40m"
  }
}

resource "google_sourcerepo_repository" "repo" {
  depends_on = [
    google_project_service.enabled_service["sourcerepo.googleapis.com"]
  ]
 
  name = "${var.namespace}-repo"
}

resource "local_file" "kube_deployment_manifest" {
  content = templatefile("./manifests/templates/deployment.yaml",
    {
        project_id=var.project_id
        gke_name=var.gke_name
        gke_namespace=var.gke_namespace
        repo_name=local.image
    })
    filename = "./manifests/out/deployment.yaml"
}

resource "local_file" "kube_service_manifest" {
  content = templatefile("./manifests/templates/service.yaml",
    {
        project_id=var.project_id
        gke_name=var.gke_name
        gke_namespace=var.gke_namespace
    })
    filename = "./manifests/out/service.yaml"
}

locals {
  image = "gcr.io/${var.project_id}/${var.namespace}"
  steps = [
    {
      name = "gcr.io/cloud-builders/docker"
      args = ["build", "-t", local.image, "-f", "Dockerfile.slim", "."]
    },
    {
      name = "gcr.io/cloud-builders/docker"
      args = ["push", local.image]
    },
    {
      name = "gcr.io/cloud-builders/kubectl"
      args = ["apply", "-f", "./infrastructure/manifests/out/service.yaml", "-f", "./infrastructure/manifests/out/deployment.yaml"]
      env = ["CLOUDSDK_COMPUTE_ZONE=${var.region}", "CLOUDSDK_CONTAINER_CLUSTER=${var.gke_name}"]
    },
    {
     name = "gcr.io/cloud-builders/kubectl"
      args = ["expose", "deployment", "-n", "${var.gke_namespace}", "${var.project_id}", "--type=LoadBalancer", "--name=${var.project_id}-lb", "--port=8080"]
      env = ["CLOUDSDK_COMPUTE_ZONE=${var.region}", "CLOUDSDK_CONTAINER_CLUSTER=${var.gke_name}"]
    }
  ]
}

resource "google_cloudbuild_trigger" "trigger" {
  depends_on = [
    google_project_service.enabled_service["cloudbuild.googleapis.com"]
  ]
 
  trigger_template {
    branch_name = "master"
    repo_name   = google_sourcerepo_repository.repo.name
  }
 
  build {
    dynamic "step" {
      for_each = local.steps
      content {
        name = step.value.name
        args = step.value.args
        env  = lookup(step.value, "env", null)
      }
    }
  }
}

data "google_project" "project" {}

resource "google_project_iam_member" "cloudbuild_roles" {
  depends_on = [google_cloudbuild_trigger.trigger]
  for_each   = toset(["roles/container.developer", "roles/iam.serviceAccountUser"])
  project    = var.project_id
  role       = each.key
  member     = "serviceAccount:${data.google_project.project.number}@cloudbuild.gserviceaccount.com"
}
