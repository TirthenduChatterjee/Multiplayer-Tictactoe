terraform {
  backend "azurerm" {
    resource_group_name  = "Neon-Toe-Dev"
    storage_account_name = "neontoestorage"
    container_name       = "tfstate"
    key                  = "build/terraform.tfstate"
  }
}