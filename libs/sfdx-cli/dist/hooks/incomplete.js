/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license.
 * For full license text, see LICENSE.txt file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
import os from 'node:os';
import { toConfiguredId, toStandardizedId } from '@oclif/core/util/ids';
function buildChoices(matches, config) {
    const configuredIds = matches.map((p) => toConfiguredId(p.id, config));
    const maxCommandLength = configuredIds.reduce((max, id) => Math.max(max, id.length), 0);
    return matches.map((p, i) => {
        const summary = p.summary ?? p.description?.split(os.EOL)[0] ?? '';
        return {
            name: `${configuredIds[i].padEnd(maxCommandLength + 5, ' ')}${summary}`,
            value: p.id,
            short: configuredIds[i],
        };
    });
}
async function determineCommand(config, matches) {
    const [{ setTimeout }, { SfError }, select] = await Promise.all([
        import('node:timers/promises'),
        import('@salesforce/core/sfError'),
        import('@inquirer/select'),
    ]);
    const choices = buildChoices(matches, config);
    const answer = select.default({
        message: 'Which of these commands do you mean',
        choices,
    });
    const timeout = setTimeout(60_000, undefined, { ref: false }).then(() => {
        answer.cancel();
        throw new SfError('Prompt timed out.');
    });
    return Promise.race([answer, timeout]);
}
const hook = async function ({ config, matches, argv }) {
    const command = await determineCommand(config, matches.filter((m) => !m.hidden));
    if (argv.includes('--help') || argv.includes('-h')) {
        const { loadHelpClass } = await import('@oclif/core/help');
        const Help = await loadHelpClass(config);
        // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
        const help = new Help(config, config.pjson.helpOptions);
        return help.showHelp([toStandardizedId(command, config), ...argv]);
    }
    return config.runCommand(toStandardizedId(command, config), argv);
};
export default hook;
//# sourceMappingURL=incomplete.js.map