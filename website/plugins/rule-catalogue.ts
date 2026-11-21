// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

import path from 'node:path';
import type {Plugin} from '@docusaurus/types';
import type {Rule} from '../src/components/ruleLabels';
import {readRules, RULES_DIR} from './rules';

/** Hands every rule's front matter to the catalogue page, which filters it in the browser. */
export default function ruleCataloguePlugin(): Plugin {
  return {
    name: 'rule-catalogue',
    async loadContent() {
      return readRules();
    },
    async contentLoaded({content, actions}) {
      actions.setGlobalData({rules: content as Rule[]});
    },
    getPathsToWatch() {
      return [path.join(RULES_DIR, '*.md')];
    },
  };
}
