const fs = require('fs');
            const validationLog = fs.readFileSync('truncated_log.txt', 'utf8');
            github.rest.issues.createComment({
              issue_number: context.payload.pull_request.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: `
              ### ✅ Salesforce Validation Successful

              🚀 **Validation passed successfully!**

              All metadata changes have been validated against Salesforce.

              ---

              **Validation Log (last 30 lines)**:
              \`\`\`
              ${validationLog}
              \`\`\`

              ---

              **Validation Summary**:
              - Branch: \`${context.payload.pull_request.head.ref}\`
              - PR: #${context.payload.pull_request.number}
              - Author: ${context.payload.pull_request.user.login}

              _No issues detected. You're good to go!_
              `
            });
