locals {
    services = [
        "container.googleapis.com",
        "sourcerepo.googleapis.com"
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