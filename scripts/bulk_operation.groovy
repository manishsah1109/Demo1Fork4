#!/usr/bin/env groovy

import groovy.io.FileType

// Load arguments passed from GitHub workflow
def csvFilesInput = args[0]  // CSV file names (comma-separated)
def operation = args[1]      // Operation (insert, upsert, delete)
def sandboxName = args[2]     // Salesforce sandbox name

// Paths for logging
def successLog = new File("logs/success.log")
def errorLog = new File("logs/error.log")

successLog.write('')  // Clear logs before starting
errorLog.write('')

// Convert CSV file input to a list
def csvFiles = csvFilesInput.split(',')

// Salesforce authentication using credentials stored in GitHub Secrets
def authenticateSalesforce() {
    def sfUsername = System.getenv('SF_USERNAME')
    def sfPassword = System.getenv('SF_PASSWORD')
    def sfToken = System.getenv('SF_TOKEN')

    def authCommand = """
        sf org login password --username ${sfUsername} --password ${sfPassword}${sfToken} --instance-url https://${sandboxName}.my.salesforce.com --set-default
    """
    println "Authenticating to Salesforce ${sandboxName}..."
    def authProcess = authCommand.execute()
    authProcess.waitFor()

    if (authProcess.exitValue() != 0) {
        println "Salesforce authentication failed."
        errorLog.append("Salesforce authentication failed.\n")
        System.exit(1)
    }

    println "Authentication successful."
}

// Perform bulk operation for each CSV file
def performBulkOperation(String csvFile, String operation) {
    def objectName = csvFile.replace(".csv", "")
    def operationCommand = ""

    switch (operation) {
        case 'insert':
            operationCommand = "sf data create bulk --sobject ${objectName} --file file/${csvFile} --wait 10"
            break
        case 'upsert':
            def externalId = 'id'  // Assuming ExternalId field, adjust as needed
            operationCommand = "sf data upsert bulk --sobject ${objectName} --file file/${csvFile} --external-id ${externalId} --wait 10"
            break
        case 'delete':
            operationCommand = "sf data delete bulk --sobject ${objectName} --file file/${csvFile} --wait 10"
            break
        default:
            println "Invalid operation: ${operation}"
            errorLog.append("Invalid operation: ${operation}\n")
            System.exit(1)
    }

    println "Running ${operation} operation on ${objectName}..."
    def process = operationCommand.execute()
    process.waitFor()

    if (process.exitValue() == 0) {
        successLog.append("${operation.capitalize()} operation for ${objectName} completed successfully.\n")
    } else {
        errorLog.append("Error during ${operation} operation for ${objectName}.\n")
    }
}

// Authenticate and run operations
authenticateSalesforce()

csvFiles.each { csvFile ->
    performBulkOperation(csvFile.trim(), operation)
}
