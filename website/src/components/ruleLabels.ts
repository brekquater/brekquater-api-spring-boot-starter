// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater
//
// Display names for the values in rule front matter. Shared by the catalogue and the facts card in the browser.

export const SEVERITIES = ['BLOCKER', 'MAJOR', 'MINOR', 'INFO'] as const;

export const SEVERITY_NAMES: Record<string, string> = {
  BLOCKER: 'Blocker',
  MAJOR: 'Major',
  MINOR: 'Minor',
  INFO: 'Info',
};

// Default weights in the score; configuration can change them.
export const SEVERITY_WEIGHTS: Record<string, string> = {
  BLOCKER: '10',
  MAJOR: '7.5',
  MINOR: '5',
  INFO: '0, listed only',
};

export const CATEGORY_NAMES: Record<string, string> = {
  DESIGN: 'Design',
  DOCUMENTATION: 'Documentation',
  SECURITY: 'Security',
  ERRORS: 'Errors',
  NAMING: 'Naming',
  TESTING: 'Testing',
};

export const READS_NAMES: Record<string, string> = {
  OPENAPI: 'OpenAPI document',
  COVERAGE: 'Coverage report',
  TEST_RESULTS: 'Test reports',
  VULNERABILITIES: 'Vulnerability report',
};

export type Rule = {
  id: string;
  title: string;
  severity: string;
  category: string;
  reads: string;
};
