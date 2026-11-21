// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';
import {CATEGORY_NAMES, CATEGORY_ORDER, readRules} from './plugins/rules';

// Rules are grouped by category from their front matter, so a new rule page needs no sidebar edit.
const rules = readRules();

const ruleCategories = CATEGORY_ORDER.filter((category) => rules.some((rule) => rule.category === category)).map(
  (category) => ({
    type: 'category' as const,
    label: CATEGORY_NAMES[category],
    collapsed: true,
    items: rules
      .filter((rule) => rule.category === category)
      .map((rule) => ({type: 'doc' as const, id: `rules/${rule.id}`, label: `${rule.id} · ${rule.title}`})),
  }),
);

const sidebars: SidebarsConfig = {
  docs: [
    'index',
    'configuration',
    'reports',
    {
      type: 'category',
      label: 'Rules',
      link: {type: 'doc', id: 'rules/index'},
      items: ruleCategories,
    },
  ],
};

export default sidebars;
