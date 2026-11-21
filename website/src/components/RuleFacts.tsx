// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

import React, {type ReactNode} from 'react';
import Link from '@docusaurus/Link';
import {useDoc} from '@docusaurus/plugin-content-docs/client';
import {CATEGORY_NAMES, READS_NAMES, SEVERITY_NAMES, SEVERITY_WEIGHTS} from './ruleLabels';

/** The facts card under a rule page's title, built from the page's front matter. Renders nothing on other pages. */
export default function RuleFacts(): ReactNode {
  const {frontMatter} = useDoc();
  const facts = frontMatter as Record<string, string>;
  if (!facts.rule_id) {
    return null;
  }
  return (
    <dl className="bq-facts">
      <div>
        <dt>Rule ID</dt>
        <dd>
          <code>{facts.rule_id}</code>
        </dd>
      </div>
      <div>
        <dt>Severity</dt>
        <dd>
          <span className={`bq-chip bq-chip--${facts.severity.toLowerCase()}`}>{SEVERITY_NAMES[facts.severity]}</span>
          <span className="bq-muted"> weight {SEVERITY_WEIGHTS[facts.severity]}</span>
        </dd>
      </div>
      <div>
        <dt>Category</dt>
        <dd>
          <Link to={`/rules/?category=${facts.category}`}>{CATEGORY_NAMES[facts.category] ?? facts.category}</Link>
        </dd>
      </div>
      <div>
        <dt>Reads</dt>
        <dd>{READS_NAMES[facts.reads] ?? facts.reads}</dd>
      </div>
    </dl>
  );
}
