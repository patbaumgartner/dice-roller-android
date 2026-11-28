#!/usr/bin/env node
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { spawnSync } from 'node:child_process';
import {
  chmodSync,
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const workflow = readFileSync(join(root, '.github/workflows/dependabot-auto-merge.yml'), 'utf8');
const requiredContext = 'Test, lint, and package';

function workflowStep(stepName) {
  const lines = workflow.split('\n');
  const step = lines.indexOf(`      - name: ${stepName}`);
  assert.notEqual(step, -1, `${stepName} step is missing`);
  const nextStep = lines.findIndex((line, index) => index > step && line.startsWith('      - name:'));
  return lines.slice(step, nextStep === -1 ? lines.length : nextStep).join('\n');
}

function stepRunScript(stepName) {
  const lines = workflowStep(stepName).split('\n');
  const run = lines.findIndex((line) => line.startsWith('        run: '));
  assert.notEqual(run, -1, `${stepName} run command is missing`);
  if (lines[run] !== '        run: |') return lines[run].slice('        run: '.length);
  const script = [];
  for (let index = run + 1; index < lines.length; index += 1) {
    const line = lines[index];
    if (line && !line.startsWith('          ')) break;
    script.push(line ? line.slice(10) : '');
  }
  return script.join('\n');
}

const script = stepRunScript('Verify main requires the aggregate CI check');
const mergeScript = stepRunScript('Enable auto-merge for patch and minor updates');

function stepCondition(stepName) {
  const lines = workflow.split('\n');
  const step = lines.indexOf(`      - name: ${stepName}`);
  assert.notEqual(step, -1, `${stepName} step is missing`);
  for (let index = step + 1; index < lines.length; index += 1) {
    const line = lines[index];
    if (line.startsWith('      - name:')) break;
    if (line === '        if: >-') {
      const parts = [];
      for (let part = index + 1; lines[part]?.startsWith('          '); part += 1) {
        parts.push(lines[part].trim());
      }
      return parts.join(' ');
    }
    if (line.startsWith('        if: ')) return line.slice('        if: '.length);
  }
  assert.fail(`${stepName} condition is missing`);
}

function updateTypeAllowed(condition, updateType) {
  const expression = condition.replaceAll('steps.metadata.outputs.update-type', JSON.stringify(updateType));
  return Function(`"use strict"; return (${expression});`)();
}

function requiredStatusRule(context = requiredContext) {
  return {
    type: 'required_status_checks',
    parameters: { required_status_checks: [{ context }] },
  };
}

function runPreflight({ pages = [[]], failRulesRead = false }) {
  const temporaryRoot = join(root, 'build');
  mkdirSync(temporaryRoot, { recursive: true });
  const directory = mkdtempSync(join(temporaryRoot, 'auto-merge-preflight-'));
  try {
    const bin = join(directory, 'bin');
    const fixtures = join(directory, 'fixtures');
    mkdirSync(bin);
    mkdirSync(fixtures);
    if (failRulesRead) writeFileSync(join(fixtures, 'fail-rules-read'), '');
    writeFileSync(join(fixtures, 'pages.json'), JSON.stringify(pages));
    const log = join(directory, 'gh.log');
    const gh = join(bin, 'gh');
    writeFileSync(
      gh,
      `#!/usr/bin/env bash
set -euo pipefail
printf '%s\\n' "$*" >> "$MOCK_GH_LOG"
if [ "$#" -lt 2 ] || [ "$1" != api ]; then
  echo 'only read-only gh api calls are allowed' >&2
  exit 64
fi
case "$*" in
  *"/rules/branches/main?per_page=100"*)
    if [ -f "$MOCK_FIXTURES/fail-rules-read" ]; then exit 1; fi
    cat "$MOCK_FIXTURES/pages.json"
    ;;
  *)
    echo "unexpected gh invocation: $*" >&2
    exit 65
    ;;
esac
`,
    );
    chmodSync(gh, 0o755);
    const result = spawnSync('bash', ['-c', script], {
      cwd: root,
      env: {
        ...process.env,
        PATH: `${bin}:${process.env.PATH}`,
        GH_TOKEN: 'test-only-token',
        GITHUB_REPOSITORY: 'owner/repo',
        REQUIRED_CONTEXT: requiredContext,
        MOCK_FIXTURES: fixtures,
        MOCK_GH_LOG: log,
      },
      encoding: 'utf8',
      timeout: 10_000,
    });
    return {
      ...result,
      output: `${result.stdout}${result.stderr}`,
      calls: existsSync(log) ? readFileSync(log, 'utf8') : '',
    };
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
}

function runMerge(token) {
  const temporaryRoot = join(root, 'build');
  mkdirSync(temporaryRoot, { recursive: true });
  const directory = mkdtempSync(join(temporaryRoot, 'auto-merge-token-'));
  try {
    const bin = join(directory, 'bin');
    mkdirSync(bin);
    const log = join(directory, 'gh.log');
    const gh = join(bin, 'gh');
    writeFileSync(
      gh,
      `#!/usr/bin/env bash
set -euo pipefail
printf '%s\\n' "$*" >> "$MOCK_GH_LOG"
if [ "$#" -lt 2 ] || [ "$1" != pr ]; then
  echo 'only the mocked merge command is allowed' >&2
  exit 64
fi
printf 'MERGE_QUEUED\\n'
`,
    );
    chmodSync(gh, 0o755);
    const result = spawnSync('bash', ['-c', mergeScript], {
      cwd: root,
      env: {
        ...process.env,
        PATH: `${bin}:${process.env.PATH}`,
        GH_TOKEN: token,
        PR_URL: 'https://example.invalid/pull/1',
        MOCK_GH_LOG: log,
      },
      encoding: 'utf8',
      timeout: 10_000,
    });
    return {
      ...result,
      output: `${result.stdout}${result.stderr}`,
      calls: existsSync(log) ? readFileSync(log, 'utf8') : '',
    };
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
}

test('pull_request_target stays no-checkout and preflight precedes merge', () => {
  assert.ok(!workflow.includes('actions/checkout@'));
  assert.ok(workflow.indexOf('- name: Verify main requires the aggregate CI check') < workflow.indexOf('gh pr merge --auto'));
});

test('merge uses AUTO_MERGE_TOKEN while metadata and preflight keep GITHUB_TOKEN', () => {
  assert.ok(workflowStep('Fetch Dependabot metadata').includes('github-token: ${{ secrets.GITHUB_TOKEN }}'));
  assert.ok(workflowStep('Verify main requires the aggregate CI check').includes('GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}'));
  const merge = workflowStep('Enable auto-merge for patch and minor updates');
  assert.ok(merge.includes('GH_TOKEN: ${{ secrets.AUTO_MERGE_TOKEN }}'));
  assert.ok(!merge.includes('secrets.GITHUB_TOKEN'));
});

test('missing automation token fails before invoking merge', () => {
  const result = runMerge('');
  assert.notEqual(result.status, 0, result.output);
  assert.match(result.output, /repository owner must configure AUTO_MERGE_TOKEN/);
  assert.match(result.output, /Contents write, Pull requests write, and Workflows write/);
  assert.equal(result.calls, '', 'merge must not be attempted without the token');
});

test('dummy automation token permits the mocked merge without printing its value', () => {
  const dummyToken = 'test-only-token';
  const result = runMerge(dummyToken);
  assert.equal(result.status, 0, result.output);
  assert.match(result.output, /MERGE_QUEUED/);
  assert.ok(result.calls.includes('pr merge --auto --rebase https://example.invalid/pull/1'));
  assert.ok(!result.output.includes(dummyToken));
});

test('patch and minor update types enable preflight and merge', () => {
  for (const stepName of ['Verify main requires the aggregate CI check', 'Enable auto-merge for patch and minor updates']) {
    const condition = stepCondition(stepName);
    for (const updateType of ['version-update:semver-patch', 'version-update:semver-minor']) {
      assert.equal(updateTypeAllowed(condition, updateType), true, `${stepName}: ${updateType}`);
    }
  }
});

for (const [label, updateType] of [
  ['empty', ''],
  ['unknown', 'unknown'],
  ['major', 'version-update:semver-major'],
]) {
  test(`${label} update type disables preflight and merge`, () => {
    for (const stepName of ['Verify main requires the aggregate CI check', 'Enable auto-merge for patch and minor updates']) {
      assert.equal(updateTypeAllowed(stepCondition(stepName), updateType), false, stepName);
    }
  });
}

test('required context in effective main rules allows auto-merge using only the paginated endpoint', () => {
  const result = runPreflight({
    pages: [[requiredStatusRule('CodeQL')], [requiredStatusRule()]],
  });
  assert.equal(result.status, 0, result.output);
  assert.match(result.output, /active main ruleset/);
  assert.ok(result.calls.includes('api --paginate --slurp repos/owner/repo/rules/branches/main?per_page=100'));
  assert.ok(!result.calls.includes('/branches/main/protection'));
  assert.ok(!result.calls.includes('/rulesets'));
});

test('wrong context does not allow auto-merge', () => {
  const result = runPreflight({ pages: [[requiredStatusRule('Build and unit tests')]] });
  assert.notEqual(result.status, 0, result.output);
  assert.match(result.output, /active main ruleset/);
});

test('context must match exactly', () => {
  const result = runPreflight({ pages: [[requiredStatusRule(`${requiredContext} extra`)]] });
  assert.notEqual(result.status, 0, result.output);
});

test('wrong rule type does not allow auto-merge', () => {
  const wrongType = { ...requiredStatusRule(), type: 'branch_name_pattern' };
  const result = runPreflight({ pages: [[wrongType]] });
  assert.notEqual(result.status, 0, result.output);
});

test('missing required-check parameters fail closed', () => {
  const result = runPreflight({ pages: [[{ type: 'required_status_checks' }]] });
  assert.notEqual(result.status, 0, result.output);
});

test('missing active main rules fail with an actionable owner error', () => {
  const result = runPreflight({});
  assert.notEqual(result.status, 0);
  assert.match(result.output, /repository owner must configure an active main ruleset/);
  assert.ok(!result.calls.includes('gh pr merge'));
});

test('main-rules API failure prevents auto-merge', () => {
  const result = runPreflight({ failRulesRead: true });
  assert.notEqual(result.status, 0);
  assert.match(result.output, /Could not read active main rules/);
});
