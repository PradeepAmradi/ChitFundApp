# Azure Deployment Setup

This document provides instructions for setting up Azure deployment for the ChitFund backend application.

## Overview

The deployment setup includes:
- **Azure Container Registry (ACR)** for storing Docker images
- **Azure Database for PostgreSQL** for the application database
- **Azure Web App for Containers** for hosting the backend API
- **GitHub Actions workflow** for automated CI/CD

## Prerequisites

1. **Azure CLI** installed and configured
2. **Azure subscription** with appropriate permissions
3. **GitHub repository** with required secrets configured

## Manual Deployment Setup

### 1. Install Azure CLI

```bash
# Install Azure CLI (if not already installed)
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
```

### 2. Login to Azure

```bash
az login
```

### 3. Run the Deployment Script

```bash
# Make the script executable
chmod +x deploy-azure.sh

# Run the deployment script
./deploy-azure.sh
```

The script will create:
- Resource group
- Azure Container Registry
- PostgreSQL database server
- Web App for Containers
- All necessary configurations

### 4. Configure GitHub Secrets

After running the deployment script, configure these secrets in your GitHub repository:

1. Go to your GitHub repository
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Add the following secrets:

| Secret Name | Description | Example Value |
|-------------|-------------|---------------|
| `AZURE_ACR_LOGIN_SERVER` | ACR login server URL | `chitfundacr123.azurecr.io` |
| `AZURE_ACR_USERNAME` | ACR username | `chitfundacr123` |
| `AZURE_ACR_PASSWORD` | ACR password | `<password from script output>` |
| `AZURE_WEBAPP_NAME` | Web app name | `chitfund-webapp-123` |
| `AZURE_RESOURCE_GROUP` | Resource group name | `chitfund-rg` |
| `AZURE_CREDENTIALS` | Azure service principal credentials | `{"clientId":"...","clientSecret":"...","subscriptionId":"...","tenantId":"..."}` |

### 5. Create Azure Service Principal

For GitHub Actions to deploy to Azure, create a service principal:

```bash
# Get your subscription ID
SUBSCRIPTION_ID=$(az account show --query id --output tsv)

# Create service principal
az ad sp create-for-rbac \
  --name "chitfund-github-actions" \
  --role contributor \
  --scopes /subscriptions/$SUBSCRIPTION_ID/resourceGroups/chitfund-rg \
  --sdk-auth
```

Copy the JSON output and add it as the `AZURE_CREDENTIALS` secret in GitHub.

## Automated CI/CD Pipeline

The GitHub Actions workflow (`.github/workflows/azure-deploy.yml`) automatically:

1. **Builds and tests** the backend when code changes
2. **Creates Docker image** and pushes to Azure Container Registry
3. **Deploys to Azure Web App** when changes are pushed to main branch

### Workflow Triggers

- **Push to main branch** with changes in backend, shared, or Dockerfile
- **Pull request to main branch** (build and test only)
- **Manual dispatch** from GitHub Actions tab

### Deployment Process

1. Build backend JAR with Gradle
2. Create Docker image using the Dockerfile
3. Push image to Azure Container Registry
4. Deploy image to Azure Web App for Containers
5. Restart the web app and verify deployment

## Environment Variables

The web app is configured with these environment variables:

- `DATABASE_URL`: PostgreSQL connection string
- `WEBSITES_PORT`: Port 8080 for the web app
- `DOCKER_REGISTRY_SERVER_*`: ACR credentials for pulling images

## Accessing the Deployed Application

After successful deployment:

- **API Base URL**: `https://<your-webapp-name>.azurewebsites.net`
- **Health Check**: `https://<your-webapp-name>.azurewebsites.net/`
- **API Endpoints**: `https://<your-webapp-name>.azurewebsites.net/api/v1/...`

## Monitoring and Logs

1. **Azure Portal**: Monitor app performance and health
2. **Application Insights**: Detailed telemetry (if enabled)
3. **Log Stream**: Real-time application logs
4. **GitHub Actions**: Build and deployment logs

## Troubleshooting

### Common Issues

1. **Container fails to start**
   - Check application logs in Azure Portal
   - Verify environment variables are correctly set
   - Ensure database connectivity

2. **GitHub Actions deployment fails**
   - Verify all secrets are correctly configured
   - Check Azure service principal permissions
   - Review workflow logs for specific errors

3. **Database connection issues**
   - Confirm firewall rules allow Azure services
   - Verify database credentials and connection string
   - Check if database server is running

### Useful Commands

```bash
# Check web app status
az webapp show --name <webapp-name> --resource-group chitfund-rg

# View web app logs
az webapp log tail --name <webapp-name> --resource-group chitfund-rg

# Restart web app
az webapp restart --name <webapp-name> --resource-group chitfund-rg

# Test database connection
az postgres flexible-server connect --name <db-server-name> --admin-user chitfundadmin
```

## Cleanup

To remove all Azure resources:

```bash
az group delete --name chitfund-rg --yes --no-wait
```

## Security Considerations

1. **Database**: Uses SSL connections and Azure firewall rules
2. **Container Registry**: Admin access enabled for deployment automation
3. **Secrets**: Stored securely in GitHub repository secrets
4. **Service Principal**: Limited scope to resource group only

## Cost Optimization

The current setup uses:
- **Basic SKU** for Container Registry
- **B1 SKU** for App Service Plan (suitable for development/testing)
- **Burstable B1ms SKU** for PostgreSQL (cost-effective for small workloads)

For production, consider upgrading to higher SKUs for better performance and availability.