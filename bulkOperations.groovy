// Import necessary libraries for handling file operations and JSON parsing
import groovy.json.JsonSlurper

// Read command-line arguments from GitHub Actions (CSV files and operation type)
def csvFiles = args[0].split(",")
def operation = args[1].toLowerCase()

// Salesforce CLI Bulk Data operation function
def runSfdxBulkOperation(csvFile, operation) {
    // Derive object name from CSV filename (e.g., Account.csv -> Account)
    def objectName = csvFile.replace(".csv", "")
    
    // Set the appropriate Salesforce bulk command based on operation type
    def command
    switch (operation) {
        case "insert":
            command = "sfdx force:data:bulk:insert -s ${objectName} -f ${csvFile} -i --json"
            break
        case "upsert":
            // Assuming we have an external ID column named "Id" for upsert operations
            //command = "sfdx force:data:bulk:upsert -s ${objectName} -f /home/runner/work/Demo1/Demo1/file/${csvFile} -i --json -w 10 -i Id"
            command = "sfdx force:data:bulk:upsert -s ${objectName} -f /home/runner/work/Demo1/Demo1/file/${csvFile} --json -w 10 -i Id"
            //command = "sfdx force:data:bulk:upsert --sobjecttype ${objectName} --csvfile file/${csvFile} --externalid Id --wait 10"
            break
        case "delete":
            command = "sfdx force:data:bulk:delete -s ${objectName} -f ${csvFile} -i --json"
            break
        default:
            println "Error: Unsupported operation ${operation}. Use insert, upsert, or delete."
            return
    }
    
    // Execute the constructed command
    println "Executing: ${command}"
    def process = command.execute()
    process.waitFor()

    // Capture and display output
    def output = process.text
    println output
    
    // Parse JSON output and display the status
    def jsonSlurper = new JsonSlurper()
    def result = jsonSlurper.parseText(output)
    println "Operation Result for ${csvFile}: ${result}"
}

// Loop through each CSV file and perform the specified operation
csvFiles.each { csvFile ->
    if (operation in ["insert", "upsert", "delete"]) {
        runSfdxBulkOperation(csvFile, operation)
    } else {
        println "Invalid operation: ${operation}. Only insert, upsert, and delete are supported."
    }
}
