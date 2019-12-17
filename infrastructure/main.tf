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

  name               = "my-test-cluster"
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
