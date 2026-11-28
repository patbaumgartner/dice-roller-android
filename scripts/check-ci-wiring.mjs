#!/usr/bin/env node
// G14: CI runs every invariant, the required check aggregates both test jobs,
// and Dependabot cannot queue a merge without repository-enforced checks.
import { readFileSync } from 'node:fs';

const fail = (msg) => { console.error(`CI WIRING FAIL: ${msg}`); process.exit(1); };
const requireText = (text, needle, msg) => { if (!text.includes(needle)) fail(msg); };
const requireMatch = (text, pattern, msg) => { if (!pattern.test(text)) fail(msg); };
const read = (path) => readFileSync(path, 'utf8');

function jobBlock(workflow, job) {
  const marker = `  ${job}:\n`;
  const markerIndex = workflow.indexOf(`\n${marker}`);
  if (markerIndex < 0) fail(`workflow has no ${job} job`);
  const body = workflow.slice(markerIndex + marker.length + 1);
  const nextJob = body.search(/^  [a-z][a-z0-9-]*:$/m);
  return nextJob < 0 ? body : body.slice(0, nextJob);
}

function stepBlock(workflow, stepName) {
  const lines = workflow.split('\n');
  const start = lines.indexOf(`      - name: ${stepName}`);
  if (start < 0) fail(`workflow has no ${stepName} step`);
  const nextStep = lines.findIndex((line, index) => index > start && line.startsWith('      - name:'));
  return lines.slice(start, nextStep < 0 ? lines.length : nextStep).join('\n');
}

const ci = read('.github/workflows/ci.yml');
const androidSdkExecutable = '"${ANDROID_SDK_ROOT:-$ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager"';
const androidSdkPackage = '"platforms;android-37.1"';
const build = jobBlock(ci, 'build');
const instrumented = jobBlock(ci, 'instrumented');
const verify = jobBlock(ci, 'verify');

const required = [
  'check-security-invariants.mjs',
  'check-font-design.mjs',
  'check-icon-design.mjs',
  'check-theme-design.mjs',
  'check-pip-patterns.mjs',
  'check-display-mode-wiring.mjs',
  'check-backhandler.mjs',
  'check-custom-range-wiring.mjs',
  'check-history-wiring.mjs',
  'check-animation-wiring.mjs',
  'check-feedback-wiring.mjs',
  'check-docs-sync.mjs',
  'check-screenshots.mjs',
  'check-ci-wiring.mjs',
  'check-test-counts.mjs',
];

for (const script of required) {
  requireText(ci, script, `ci.yml does not run ${script}`);
}
requireMatch(ci, /yamllint \./, 'ci.yml does not run yamllint');
requireText(ci, 'shellcheck', 'ci.yml does not run shellcheck');
requireMatch(build, /^    name: Build and unit tests$/m, 'build job must be named Build and unit tests');
requireText(build, 'testDebugUnitTest lintDebug assembleDebug assembleRelease', 'build gate is incomplete');

requireText(instrumented, 'connectedDebugAndroidTest', 'instrumented job does not run connectedDebugAndroidTest');
requireText(instrumented, 'if: failure()', 'instrumented reports must be uploaded on failure');
requireText(instrumented, 'app/build/outputs/androidTest-results/', 'instrumented result artifacts are missing');
requireText(instrumented, androidSdkExecutable, 'instrumented SDK setup must use the installed sdkmanager path');
requireText(instrumented, androidSdkPackage, 'instrumented SDK setup must install platform 37.1');

const releaseWorkflow = read('.github/workflows/release.yml');
for (const job of ['snapshot', 'release']) {
  requireText(jobBlock(releaseWorkflow, job), androidSdkExecutable, `${job} job must use the installed sdkmanager path`);
  requireText(jobBlock(releaseWorkflow, job), androidSdkPackage, `${job} job must install Android SDK 37.1`);
}

requireMatch(verify, /^    name: Test, lint, and package$/m, 'required aggregate check has the wrong name');
requireMatch(verify, /^    if: \$\{\{ always\(\) \}\}$/m, 'aggregate must run after failed dependencies');
requireMatch(verify, /^    needs: \[build, instrumented\]$/m, 'aggregate must need build and instrumented');
requireMatch(verify, /^    timeout-minutes: 5$/m, 'aggregate must time out after five minutes');
requireMatch(
  verify,
  /\[ "\$\{\{ needs\.build\.result \}\}" != "success" \]/,
  'aggregate does not require build success',
);
requireMatch(
  verify,
  /\[ "\$\{\{ needs\.instrumented\.result \}\}" != "success" \]/,
  'aggregate does not require instrumented success',
);
requireText(verify, 'exit 1', 'aggregate must fail when either dependency fails');
requireText(
  verify,
  'echo "Build and unit tests and instrumented tests succeeded."',
  'aggregate must print a success message',
);

requireText(build, 'node --test scripts/auto-merge-preflight.test.mjs', 'CI does not run auto-merge controls');

const autoMerge = read('.github/workflows/dependabot-auto-merge.yml');
requireText(autoMerge, 'pull_request_target:', 'Dependabot workflow trigger changed unexpectedly');
if (autoMerge.includes('actions/checkout@')) fail('pull_request_target must never check out PR code');
requireText(autoMerge, 'REQUIRED_CONTEXT: Test, lint, and package', 'Dependabot protects the wrong check context');
for (const updateType of ['version-update:semver-patch', 'version-update:semver-minor']) {
  const condition = `steps.metadata.outputs.update-type == '${updateType}'`;
  if (autoMerge.split(condition).length - 1 !== 2) fail(`preflight and merge must both explicitly allow ${updateType}`);
}
if (autoMerge.includes("steps.metadata.outputs.update-type !=")) fail('update types must be allow-listed, not major-excluded');

const metadataStep = stepBlock(autoMerge, 'Fetch Dependabot metadata');
const preflightStep = stepBlock(autoMerge, 'Verify main requires the aggregate CI check');
const mergeStep = stepBlock(autoMerge, 'Enable auto-merge for patch and minor updates');
requireText(metadataStep, 'github-token: ${{ secrets.GITHUB_TOKEN }}', 'metadata must keep GITHUB_TOKEN');
requireText(preflightStep, 'GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}', 'preflight must keep GITHUB_TOKEN');
requireText(mergeStep, 'GH_TOKEN: ${{ secrets.AUTO_MERGE_TOKEN }}', 'merge must use AUTO_MERGE_TOKEN');
if (mergeStep.includes('secrets.GITHUB_TOKEN')) fail('merge must not use GITHUB_TOKEN');
requireText(mergeStep, '[ -z "$GH_TOKEN" ]', 'merge must reject an empty automation token');
for (const permission of ['Contents write', 'Pull requests write', 'Workflows write']) {
  requireText(mergeStep, permission, `missing-token message must require ${permission}`);
}
requireText(mergeStep, 'merged pushes trigger CI and releases', 'missing-token message must explain push triggering');

requireText(autoMerge, '--paginate --slurp', 'main-rules pagination must include every page');
requireText(
  autoMerge,
  'repos/$GITHUB_REPOSITORY/rules/branches/main?per_page=100',
  'preflight must read effective rules for main',
);
for (const forbidden of ['branches/main/protection', 'repos/$GITHUB_REPOSITORY/rulesets']) {
  if (autoMerge.includes(forbidden)) fail(`preflight must not call ${forbidden}`);
}
requireText(autoMerge, '.type == "required_status_checks"', 'preflight must select required status-check rules');
requireText(autoMerge, '.parameters.required_status_checks[]?', 'ruleset required contexts are not selected');
requireText(autoMerge, '.context == $required', 'required context must match exactly');
requireText(autoMerge, 'repository owner must configure an active main ruleset', 'failure is not actionable');
requireText(autoMerge, 'exit 1', 'Dependabot protection verification must fail closed');
if (/gh api[^\n]*--method/.test(autoMerge)) fail('Dependabot protection verification must use read-only GET requests');

const preflightTests = read('scripts/auto-merge-preflight.test.mjs');
for (const control of [
  'merge uses AUTO_MERGE_TOKEN while metadata and preflight keep GITHUB_TOKEN',
  'missing automation token fails before invoking merge',
  'required context in effective main rules allows auto-merge using only the paginated endpoint',
  'context must match exactly',
  'wrong rule type does not allow auto-merge',
  'main-rules API failure prevents auto-merge',
]) {
  requireText(preflightTests, control, `missing executable control: ${control}`);
}

const mergeCommand = 'gh pr merge --auto --rebase "$PR_URL"';
const mergeIndex = autoMerge.indexOf(mergeCommand);
if (mergeIndex < 0) fail('Dependabot auto-merge command is missing');
const mainRulesApi = 'rules/branches/main?per_page=100';
if (autoMerge.indexOf(mainRulesApi) > mergeIndex) fail('effective main rules must be checked before auto-merge');
const tokenGuardIndex = mergeStep.indexOf('[ -z "$GH_TOKEN" ]');
const mergeStepCommandIndex = mergeStep.indexOf(mergeCommand);
if (tokenGuardIndex < 0 || tokenGuardIndex > mergeStepCommandIndex) fail('token guard must precede merge');

console.log('CI WIRING OK');
