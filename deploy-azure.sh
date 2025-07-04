#!/bin/bash

# ChitFund App - Azure Deployment Script
# This script sets up Azure infrastructure for the ChitFund backend

set -e  # Exit on any error

# Configuration
RESOURCE_GROUP_NAME="chitfund-rg"
LOCATION="East US"
ACR_NAME="chitfundacr$(date +%s)"  # Unique name with timestamp
DB_SERVER_NAME="chitfund-db-$(date +%s)"  # Unique name with timestamp
DB_NAME="chitfund"
DB_ADMIN_USER="chitfundadmin"
DB_ADMIN_PASSWORD="ChitFund123!"
APP_SERVICE_PLAN="chitfund-plan"
WEB_APP_NAME="chitfund-webapp-$(date +%s)"  # Unique name with timestamp

echo "🚀 Starting Azure deployment for ChitFund App"
echo "📍 Resource Group: $RESOURCE_GROUP_NAME"
echo "📍 Location: $LOCATION"
echo "📍 ACR Name: $ACR_NAME"
echo "📍 Database Server: $DB_SERVER_NAME"
echo "📍 Web App: $WEB_APP_NAME"

# Check if user is logged in to Azure CLI
echo "🔐 Checking Azure CLI login status..."
if ! az account show >/dev/null 2>&1; then
    echo "❌ Not logged in to Azure CLI. Please run 'az login' first."
    exit 1
fi

echo "✅ Azure CLI logged in"

# Create resource group
echo "📦 Creating resource group: $RESOURCE_GROUP_NAME"
az group create \
    --name $RESOURCE_GROUP_NAME \
    --location "$LOCATION"

# Create Azure Container Registry
echo "🐳 Creating Azure Container Registry: $ACR_NAME"
az acr create \
    --resource-group $RESOURCE_GROUP_NAME \
    --name $ACR_NAME \
    --sku Basic \
    --admin-enabled true

echo "🔑 Getting ACR credentials..."
ACR_LOGIN_SERVER=$(az acr show --name $ACR_NAME --resource-group $RESOURCE_GROUP_NAME --query loginServer --output tsv)
ACR_USERNAME=$(az acr credential show --name $ACR_NAME --resource-group $RESOURCE_GROUP_NAME --query username --output tsv)
ACR_PASSWORD=$(az acr credential show --name $ACR_NAME --resource-group $RESOURCE_GROUP_NAME --query passwords[0].value --output tsv)

# Create PostgreSQL Database
echo "🗄️ Creating PostgreSQL database server: $DB_SERVER_NAME"
az postgres flexible-server create \
    --resource-group $RESOURCE_GROUP_NAME \
    --name $DB_SERVER_NAME \
    --location "$LOCATION" \
    --admin-user $DB_ADMIN_USER \
    --admin-password $DB_ADMIN_PASSWORD \
    --sku-name Standard_B1ms \
    --tier Burstable \
    --version 15 \
    --storage-size 32

echo "🔐 Configuring database firewall rules..."
# Allow Azure services to access the database
az postgres flexible-server firewall-rule create \
    --resource-group $RESOURCE_GROUP_NAME \
    --name $DB_SERVER_NAME \
    --rule-name "AllowAzureServices" \
    --start-ip-address 0.0.0.0 \
    --end-ip-address 0.0.0.0

# Create the database
echo "📚 Creating database: $DB_NAME"
az postgres flexible-server db create \
    --resource-group $RESOURCE_GROUP_NAME \
    --server-name $DB_SERVER_NAME \
    --database-name $DB_NAME

# Create App Service Plan
echo "⚙️ Creating App Service Plan: $APP_SERVICE_PLAN"
az appservice plan create \
    --resource-group $RESOURCE_GROUP_NAME \
    --name $APP_SERVICE_PLAN \
    --location "$LOCATION" \
    --sku B1 \
    --is-linux

# Create Web App for Containers
echo "🌐 Creating Web App: $WEB_APP_NAME"
az webapp create \
    --resource-group $RESOURCE_GROUP_NAME \
    --plan $APP_SERVICE_PLAN \
    --name $WEB_APP_NAME \
    --deployment-container-image-name "mcr.microsoft.com/appsvc/staticsite:latest"

# Configure Web App settings
echo "⚙️ Configuring Web App settings..."
DATABASE_URL="jdbc:postgresql://${DB_SERVER_NAME}.postgres.database.azure.com:5432/${DB_NAME}?user=${DB_ADMIN_USER}&password=${DB_ADMIN_PASSWORD}&sslmode=require"

az webapp config appsettings set \
    --resource-group $RESOURCE_GROUP_NAME \
    --name $WEB_APP_NAME \
    --settings \
        DATABASE_URL="$DATABASE_URL" \
        WEBSITES_PORT=8080 \
        DOCKER_REGISTRY_SERVER_URL="https://$ACR_LOGIN_SERVER" \
        DOCKER_REGISTRY_SERVER_USERNAME="$ACR_USERNAME" \
        DOCKER_REGISTRY_SERVER_PASSWORD="$ACR_PASSWORD"

echo "✅ Azure infrastructure deployment completed!"
echo ""
echo "📋 Deployment Summary:"
echo "  Resource Group: $RESOURCE_GROUP_NAME"
echo "  Container Registry: $ACR_LOGIN_SERVER"
echo "  Database Server: ${DB_SERVER_NAME}.postgres.database.azure.com"
echo "  Web App URL: https://${WEB_APP_NAME}.azurewebsites.net"
echo ""
echo "🔐 Important Credentials:"
echo "  ACR Username: $ACR_USERNAME"
echo "  ACR Password: $ACR_PASSWORD (stored in Web App settings)"
echo "  Database URL: $DATABASE_URL"
echo ""
echo "📝 Next Steps:"
echo "  1. Build and push your Docker image to: $ACR_LOGIN_SERVER/chitfund-backend:latest"
echo "  2. Update Web App to use your container image"
echo "  3. The GitHub Actions workflow can now deploy to these resources"
echo ""
echo "🔧 GitHub Secrets to configure:"
echo "  AZURE_ACR_LOGIN_SERVER: $ACR_LOGIN_SERVER"
echo "  AZURE_ACR_USERNAME: $ACR_USERNAME"
echo "  AZURE_ACR_PASSWORD: $ACR_PASSWORD"
echo "  AZURE_WEBAPP_NAME: $WEB_APP_NAME"
echo "  AZURE_RESOURCE_GROUP: $RESOURCE_GROUP_NAME"