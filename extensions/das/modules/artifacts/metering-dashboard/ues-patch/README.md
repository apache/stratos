This folder (ues-patch) contains fix for DAS-3.0.0 analytics dashboard to  support reading request parameters from 
dashboard url and publish the values to gadgets.

You can find the related UES product jira here: https://wso2.org/jira/browse/UES-582
Please follow below steps to apply the patch locally:

1. Copy 'ues-dashboard.js' and 'ues-pubsub.js' files to <DAS-HOME>/repository/deployment/server/jaggeryapps/portal/js/ folder.
2. Copy 'dashboard.jag' file to <DAS-HOME>/repository/deployment/server/jaggeryapps/portal/theme/templates/ folder.