const fs = require('node:fs');
const path = require('node:path');
const validator = require('gltf-validator');

const directory = path.join(__dirname, '..', 'app', 'src', 'main', 'res', 'raw');
const files = ['home_stats_flame.glb', 'home_stats_books.glb', 'home_stats_trophy.glb'];

(async () => {
  let failed = false;
  for (const name of files) {
    const file = path.join(directory, name);
    if (!fs.existsSync(file)) {
      console.error(`MISSING: ${name}`);
      failed = true;
      continue;
    }
    const bytes = new Uint8Array(fs.readFileSync(file));
    const report = await validator.validateBytes(bytes, { uri: name });
    const errors = report.issues.numErrors;
    const warnings = report.issues.numWarnings;
    console.log(`${name}: ${errors} errors, ${warnings} warnings`);
    if (errors > 0) {
      for (const message of report.issues.messages.filter(item => item.severity === 0)) {
        console.error(`  ${message.code}: ${message.message}`);
      }
      failed = true;
    }
  }
  if (failed) process.exitCode = 1;
})();
