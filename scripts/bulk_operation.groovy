#!/usr/bin/env groovy

if (args.length != 3) {
    println "Usage: groovy bulk_operation.groovy <csv_files> <operation> <sandbox_name>"
    System.exit(1)
}

def csvFilesInput = args[0]
def operation = args[1]
def sandboxName = args[2]

def successLog = new File("logs/success.log")
def errorLog = new File("logs/error.log")

successLog.write('')
errorLog.write('')

def csvFiles = csvFilesInput.split(',')

def authenticateSalesforce() {
    def sfUsername = System.getenv('SF_USERNAME')
    def sfPassword = System.getenv('SF_PASSWORD')
    def sfToken = System.getenv('SF_TOKEN')

    def authCommand = """
        sfdx force:auth:web:login --setalias develop --instanceurl https:login.salesforce.com --setdefaultusername
    """
    println "Authenticating to Salesforce develop..."
    def authProcess = authCommand.execute()
    authProcess.waitFor()

    if (authProcess.exitValue() != 0) {
        println "Salesforce authentication failed. Exit code: ${authProcess.exitValue()}"
        println "Error Output: ${authProcess.err.text}"
        errorLog.append("Salesforce authentication failed. Exit code: ${authProcess.exitValue()}\n")
        errorLog.append("Error Output: ${authProcess.err.text}\n")
        System.exit(1)
    }

    println "Authentication successful."
}

def performBulkOperation(String csvFile, String operation) {
    def objectName = csvFile.replace(".csv", "")
    def operationCommand = ""

    switch (operation) {
        case 'insert':
            operationCommand = "sfdx force:data:bulk:insert --sobjecttype ${objectName} --csvfile data/${csvFile} --wait 10"
            break
        case 'upsert':
            def externalId = 'ExternalId__c'
            operationCommand = "sfdx force:data:bulk:upsert --sobjecttype ${objectName} --csvfile data/${csvFile} --externalid ${externalId} --wait 10"
            break
        case 'delete':
            operationCommand = "sfdx force:data:bulk:delete --sobjecttype ${objectName} --csvfile data/${csvFile} --wait 10"
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
        println "Success: ${operation.capitalize()} operation for ${objectName} completed."
    } else {
        errorLog.append("Error during ${operation} operation for ${objectName}. Exit code: ${process.exitValue()}\n")
        errorLog.append("Error Output: ${process.err.text}\n")
        println "Error: ${operation.capitalize()} operation for ${objectName} failed. Exit code: ${process.exitValue()}"
        println "Error Output: ${process.err.text}"
    }
}

authenticateSalesforce()

csvFiles.each { csvFile ->
    performBulkOperation(csvFile.trim(), operation)
}
