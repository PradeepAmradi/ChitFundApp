#!/bin/bash

# ChitFund App - Azure Deployment Script
# This script sets up Azure infrastructure for the ChitFund backend

set -e  # Exit on any error

# Configuration
RESOURCE_GROUP_NAME="chitfund-rg"
LOCATION="Central India"
ACR_NAME="chitfundacr"  # Fixed name
DB_SERVER_NAME="chitfund-db"  # Fixed name
DB_NAME="chitfund"
DB_ADMIN_USER="chitfundadmin"
DB_ADMIN_PASSWORD="ChitFund123!"
APP_SERVICE_PLAN="chitfund-plan"
WEB_APP_NAME="chitfund-webapp"  # Fixed name

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

# Cleanup old resources with timestamp suffixes
echo "🧹 Cleaning up old timestamped resources..."

# List and delete old ACRs with timestamp suffixes
echo "🧹 Cleaning up old Container Registries..."
OLD_ACRS=$(az acr list --resource-group $RESOURCE_GROUP_NAME --query "[?starts_with(name, 'chitfundacr') && name != 'chitfundacr'].name" -o tsv 2>/dev/null || true)
for acr in $OLD_ACRS; do
    if [ ! -z "$acr" ]; then
        echo "  Deleting old ACR: $acr"
        az acr delete --name $acr --resource-group $RESOURCE_GROUP_NAME --yes 2>/dev/null || true
    fi
done

# List and delete old PostgreSQL servers with timestamp suffixes
echo "🧹 Cleaning up old PostgreSQL servers..."
OLD_DBS=$(az postgres flexible-server list --resource-group $RESOURCE_GROUP_NAME --query "[?starts_with(name, 'chitfund-db-') && name != 'chitfund-db'].name" -o tsv 2>/dev/null || true)
for db in $OLD_DBS; do
    if [ ! -z "$db" ]; then
        echo "  Deleting old PostgreSQL server: $db"
        az postgres flexible-server delete --name $db --resource-group $RESOURCE_GROUP_NAME --yes 2>/dev/null || true
    fi
done

# List and delete old Web Apps with timestamp suffixes
echo "🧹 Cleaning up old Web Apps..."
OLD_WEBAPPS=$(az webapp list --resource-group $RESOURCE_GROUP_NAME --query "[?starts_with(name, 'chitfund-webapp-') && name != 'chitfund-webapp'].name" -o tsv 2>/dev/null || true)
for webapp in $OLD_WEBAPPS; do
    if [ ! -z "$webapp" ]; then
        echo "  Deleting old Web App: $webapp"
        az webapp delete --name $webapp --resource-group $RESOURCE_GROUP_NAME 2>/dev/null || true
    fi
done

echo "✅ Cleanup completed"

# Create resource group
echo "📦 Creating resource group: $RESOURCE_GROUP_NAME"
az group create \
    --name $RESOURCE_GROUP_NAME \
    --location "$LOCATION"



# Register required Azure resource providers if not already registered
echo "📝 Registering Microsoft.ContainerRegistry resource provider..."
az provider register --namespace Microsoft.ContainerRegistry

echo "📝 Registering Microsoft.DBforPostgreSQL resource provider..."
az provider register --namespace Microsoft.DBforPostgreSQL

echo "📝 Registering Microsoft.Web resource provider..."
az provider register --namespace Microsoft.Web

# Create Azure Container Registry
echo "🐳 Creating/Checking Azure Container Registry: $ACR_NAME"
if ! az acr show --name $ACR_NAME --resource-group $RESOURCE_GROUP_NAME >/dev/null 2>&1; then
    echo "  Creating new ACR: $ACR_NAME"
    az acr create \
        --resource-group $RESOURCE_GROUP_NAME \
        --name $ACR_NAME \
        --sku Basic \
        --admin-enabled true
else
    echo "  ACR already exists: $ACR_NAME"
fi

echo "🔑 Getting ACR credentials..."
ACR_LOGIN_SERVER=$(az acr show --name $ACR_NAME --resource-group $RESOURCE_GROUP_NAME --query loginServer --output tsv)
ACR_USERNAME=$(az acr credential show --name $ACR_NAME --resource-group $RESOURCE_GROUP_NAME --query username --output tsv)
ACR_PASSWORD=$(az acr credential show --name $ACR_NAME --resource-group $RESOURCE_GROUP_NAME --query passwords[0].value --output tsv)

# Create PostgreSQL Database
echo "🗄️ Creating/Checking PostgreSQL database server: $DB_SERVER_NAME"
if ! az postgres flexible-server show --name $DB_SERVER_NAME --resource-group $RESOURCE_GROUP_NAME >/dev/null 2>&1; then
    echo "  Creating new PostgreSQL server: $DB_SERVER_NAME"
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
else
    echo "  PostgreSQL server already exists: $DB_SERVER_NAME"
    # Ensure the database exists
    echo "📚 Ensuring database exists: $DB_NAME"
    az postgres flexible-server db create \
        --resource-group $RESOURCE_GROUP_NAME \
        --server-name $DB_SERVER_NAME \
        --database-name $DB_NAME 2>/dev/null || echo "  Database already exists or creation skipped"
fi

# Create App Service Plan
echo "⚙️ Creating/Checking App Service Plan: $APP_SERVICE_PLAN"
if ! az appservice plan show --name $APP_SERVICE_PLAN --resource-group $RESOURCE_GROUP_NAME >/dev/null 2>&1; then
    echo "  Creating new App Service Plan: $APP_SERVICE_PLAN"
    az appservice plan create \
        --resource-group $RESOURCE_GROUP_NAME \
        --name $APP_SERVICE_PLAN \
        --location "$LOCATION" \
        --sku B1 \
        --is-linux
else
    echo "  App Service Plan already exists: $APP_SERVICE_PLAN"
fi

# Create Web App for Containers
echo "🌐 Creating/Checking Web App: $WEB_APP_NAME"
if ! az webapp show --name $WEB_APP_NAME --resource-group $RESOURCE_GROUP_NAME >/dev/null 2>&1; then
    echo "  Creating new Web App: $WEB_APP_NAME"
    az webapp create \
        --resource-group $RESOURCE_GROUP_NAME \
        --plan $APP_SERVICE_PLAN \
        --name $WEB_APP_NAME \
        --deployment-container-image-name "mcr.microsoft.com/appsvc/staticsite:latest"
else
    echo "  Web App already exists: $WEB_APP_NAME"
fi

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