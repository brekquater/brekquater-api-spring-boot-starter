// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater
//
// Reads the rule pages' front matter. Runs in Node at build time only (the sidebar and the catalogue plugin), never
// in the browser. The front matter is the single source: the Java test checks it against each rule's code.

import fs from 'node:fs';
import path from 'node:path';
import matter from 'gray-matter';
import type {Rule} from '../src/components/ruleLabels';

export {CATEGORY_NAMES} from '../src/components/ruleLabels';

export const RULES_DIR = path.join(__dirname, '..', '..', 'docs', 'rules');

// The order categories appear in the sidebar.
export const CATEGORY_ORDER = ['DESIGN', 'DOCUMENTATION', 'SECURITY', 'ERRORS', 'NAMING', 'TESTING'];

/** The rule pages' file names, sorted by rule ID. */
export function ruleFiles(): string[] {
  return fs
    .readdirSync(RULES_DIR)
    .filter((file) => /^BQ-[A-Z]{3}-\d{3}\.md$/.test(file))
    .sort();
}

export function readRules(): Rule[] {
  return ruleFiles().map((file) => {
    const {data} = matter(fs.readFileSync(path.join(RULES_DIR, file), 'utf8'));
    return {
      id: data.rule_id,
      // The title is "<RULE-ID>: <what it checks>"; the catalogue shows the second part next to the ID.
      title: String(data.title).replace(/^BQ-[A-Z]{3}-\d{3}: /, ''),
      severity: data.severity,
      category: data.category,
      reads: data.reads,
    };
  });
}
