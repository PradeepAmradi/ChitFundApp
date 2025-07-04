#!/bin/bash

# ChitFund App - Azure Deployment Verification Script
# This script verifies that all Azure resources are properly configured

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration - Update these values after running deploy-azure.sh
RESOURCE_GROUP_NAME="chitfund-rg"
ACR_NAME=""  # Will be determined automatically
DB_SERVER_NAME=""  # Will be determined automatically
WEB_APP_NAME=""  # Will be determined automatically

echo -e "${YELLOW}🔍 ChitFund Azure Deployment Verification${NC}"
echo "=============================================="

# Check if user is logged in to Azure CLI
echo -e "${YELLOW}🔐 Checking Azure CLI login status...${NC}"
if ! az account show >/dev/null 2>&1; then
    echo -e "${RED}❌ Not logged in to Azure CLI. Please run 'az login' first.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Azure CLI logged in${NC}"

# Check if resource group exists
echo -e "${YELLOW}📦 Checking resource group: $RESOURCE_GROUP_NAME${NC}"
if az group show --name $RESOURCE_GROUP_NAME >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Resource group exists${NC}"
else
    echo -e "${RED}❌ Resource group not found. Run deploy-azure.sh first.${NC}"
    exit 1
fi

# Get resource names automatically
echo -e "${YELLOW}🔍 Discovering deployed resources...${NC}"
ACR_NAME=$(az acr list --resource-group $RESOURCE_GROUP_NAME --query "[0].name" --output tsv 2>/dev/null || echo "")
DB_SERVER_NAME=$(az postgres flexible-server list --resource-group $RESOURCE_GROUP_NAME --query "[0].name" --output tsv 2>/dev/null || echo "")
WEB_APP_NAME=$(az webapp list --resource-group $RESOURCE_GROUP_NAME --query "[0].name" --output tsv 2>/dev/null || echo "")

if [ -z "$ACR_NAME" ] || [ -z "$DB_SERVER_NAME" ] || [ -z "$WEB_APP_NAME" ]; then
    echo -e "${RED}❌ Could not find all required resources. Deploy may not be complete.${NC}"
    echo "Found:"
    echo "  ACR: $ACR_NAME"
    echo "  Database: $DB_SERVER_NAME"
    echo "  Web App: $WEB_APP_NAME"
    exit 1
fi

echo -e "${GREEN}✅ Found all resources:${NC}"
echo "  📦 ACR: $ACR_NAME"
echo "  🗄️  Database: $DB_SERVER_NAME"
echo "  🌐 Web App: $WEB_APP_NAME"

# Check Azure Container Registry
echo -e "${YELLOW}🐳 Checking Azure Container Registry...${NC}"
ACR_STATUS=$(az acr show --name $ACR_NAME --resource-group $RESOURCE_GROUP_NAME --query "provisioningState" --output tsv)
if [ "$ACR_STATUS" = "Succeeded" ]; then
    echo -e "${GREEN}✅ ACR is running${NC}"
    ACR_LOGIN_SERVER=$(az acr show --name $ACR_NAME --resource-group $RESOURCE_GROUP_NAME --query loginServer --output tsv)
    echo "  🔗 Login Server: $ACR_LOGIN_SERVER"
else
    echo -e "${RED}❌ ACR status: $ACR_STATUS${NC}"
fi

# Check PostgreSQL Database
echo -e "${YELLOW}🗄️ Checking PostgreSQL database...${NC}"
DB_STATUS=$(az postgres flexible-server show --name $DB_SERVER_NAME --resource-group $RESOURCE_GROUP_NAME --query "state" --output tsv)
if [ "$DB_STATUS" = "Ready" ]; then
    echo -e "${GREEN}✅ Database is running${NC}"
    DB_FQDN=$(az postgres flexible-server show --name $DB_SERVER_NAME --resource-group $RESOURCE_GROUP_NAME --query "fullyQualifiedDomainName" --output tsv)
    echo "  🔗 FQDN: $DB_FQDN"
else
    echo -e "${YELLOW}⚠️ Database status: $DB_STATUS${NC}"
fi

# Check Web App
echo -e "${YELLOW}🌐 Checking Web App...${NC}"
WEB_APP_STATE=$(az webapp show --name $WEB_APP_NAME --resource-group $RESOURCE_GROUP_NAME --query "state" --output tsv)
if [ "$WEB_APP_STATE" = "Running" ]; then
    echo -e "${GREEN}✅ Web App is running${NC}"
    WEB_APP_URL="https://${WEB_APP_NAME}.azurewebsites.net"
    echo "  🔗 URL: $WEB_APP_URL"
    
    # Test if web app is responding
    echo -e "${YELLOW}🔍 Testing web app connectivity...${NC}"
    if curl -f -s --max-time 10 "$WEB_APP_URL" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Web app is responding${NC}"
    else
        echo -e "${YELLOW}⚠️ Web app is not responding (may still be starting up)${NC}"
    fi
else
    echo -e "${YELLOW}⚠️ Web App state: $WEB_APP_STATE${NC}"
fi

# Check Web App configuration
echo -e "${YELLOW}⚙️ Checking Web App configuration...${NC}"
DB_URL_SET=$(az webapp config appsettings list --name $WEB_APP_NAME --resource-group $RESOURCE_GROUP_NAME --query "[?name=='DATABASE_URL'].value" --output tsv)
if [ -n "$DB_URL_SET" ]; then
    echo -e "${GREEN}✅ DATABASE_URL is configured${NC}"
else
    echo -e "${RED}❌ DATABASE_URL not found in app settings${NC}"
fi

WEBSITES_PORT=$(az webapp config appsettings list --name $WEB_APP_NAME --resource-group $RESOURCE_GROUP_NAME --query "[?name=='WEBSITES_PORT'].value" --output tsv)
if [ "$WEBSITES_PORT" = "8080" ]; then
    echo -e "${GREEN}✅ WEBSITES_PORT is configured correctly${NC}"
else
    echo -e "${RED}❌ WEBSITES_PORT not set to 8080${NC}"
fi

echo ""
echo -e "${GREEN}🎉 Verification Complete!${NC}"
echo ""
echo -e "${YELLOW}📋 Deployment Summary:${NC}"
echo "  🌐 Web App URL: https://${WEB_APP_NAME}.azurewebsites.net"
echo "  🐳 Container Registry: $ACR_LOGIN_SERVER"
echo "  🗄️  Database Server: ${DB_SERVER_NAME}.postgres.database.azure.com"
echo ""
echo -e "${YELLOW}📝 Next Steps:${NC}"
echo "  1. Configure GitHub secrets for CI/CD pipeline"
echo "  2. Push code changes to trigger automated deployment"
echo "  3. Monitor deployment in GitHub Actions"
echo ""
echo -e "${YELLOW}🔧 GitHub Secrets needed:${NC}"
echo "  AZURE_ACR_LOGIN_SERVER: $ACR_LOGIN_SERVER"
echo "  AZURE_ACR_USERNAME: $ACR_NAME"
echo "  AZURE_WEBAPP_NAME: $WEB_APP_NAME"
echo "  AZURE_RESOURCE_GROUP: $RESOURCE_GROUP_NAME"
echo "  AZURE_CREDENTIALS: <service principal JSON>"