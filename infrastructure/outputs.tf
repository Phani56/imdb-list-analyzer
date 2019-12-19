output "urls" {
  value = {
    repo = google_sourcerepo_repository.repo.url
  }
}
