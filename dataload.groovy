@Grab(group='org.apache.commons', module='commons-csv', version='1.8')
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

// Function to load CSV data
def loadCsv(String filePath) {
    def csvFile = new File(filePath)
    def csvRecords = CSVParser.parse(csvFile, CSVFormat.DEFAULT.withFirstRecordAsHeader())
    return csvRecords.getRecords()
}

// Function to load SDL data
def loadSdl(String filePath) {
    return new File(filePath).text
}

// Function to execute Salesforce Bulk API insert
def executeSalesforceInsert(String csvFilePath, String sdlFilePath, String sandboxAlias, String sObjectType, String externalId) {
    println "Authentication successful."

    println "Executing DataLoader insert for $csvFilePath..."
    def dataInsertCmd = "sfdx force:data:bulk:upsert --sobjecttype $sObjectType --csvfile $csvFilePath --externalid $externalId --wait 10"
    def insertProcess = dataInsertCmd.execute()
    insertProcess.waitFor()

    if (insertProcess.exitValue() != 0) {
        println "Data insert failed for $csvFilePath: ${insertProcess.err.text}"
        return
    }

    println "Data insert successful for $csvFilePath."
}

def csvDirectoryPath = '../file1/Account.csv'
def sdlFilePath = '../file/Account.sdl'
def sandboxAlias = 'develop'
def sObjectType = 'Account' // Replace with your Salesforce object API name
def externalId = 'AccountNumber' // Replace with your external ID field

// Load SDL content
def sdlContent = loadSdl(sdlFilePath)

// Process each CSV file in the directory
new File(csvDirectoryPath).eachFile { file ->
    if (file.name.endsWith(".csv")) {
        executeSalesforceInsert(file.absolutePath, sdlFilePath, sandboxAlias, sObjectType, externalId)
    }
}
      
