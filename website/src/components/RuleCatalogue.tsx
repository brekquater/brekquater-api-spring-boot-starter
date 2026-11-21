// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

import React, {type ReactNode, useEffect, useMemo, useState} from 'react';
import Link from '@docusaurus/Link';
import {useLocation} from '@docusaurus/router';
import {usePluginData} from '@docusaurus/useGlobalData';
import {CATEGORY_NAMES, READS_NAMES, type Rule, SEVERITIES, SEVERITY_NAMES} from './ruleLabels';

type Filter = {text: string; category: string; severity: string; reads: string};

const NO_FILTER: Filter = {text: '', category: '', severity: '', reads: ''};

function matches(rule: Rule, filter: Filter): boolean {
  const text = `${rule.id} ${rule.title}`.toLowerCase();
  return (
    (!filter.category || rule.category === filter.category) &&
    (!filter.severity || rule.severity === filter.severity) &&
    (!filter.reads || rule.reads === filter.reads) &&
    (!filter.text || text.includes(filter.text.trim().toLowerCase()))
  );
}

function present(rules: Rule[], field: keyof Rule, order: string[]): string[] {
  return order.filter((value) => rules.some((rule) => rule[field] === value));
}

/** Every rule, filterable by text, category, severity and input. Built from the rule pages' front matter. */
export default function RuleCatalogue(): ReactNode {
  const {rules} = usePluginData('rule-catalogue') as {rules: Rule[]};
  const location = useLocation();
  const [filter, setFilter] = useState<Filter>(NO_FILTER);

  // A link such as /rules/?category=SECURITY opens the catalogue already filtered.
  useEffect(() => {
    const category = new URLSearchParams(location.search).get('category') ?? '';
    setFilter((current) => ({...current, category: category in CATEGORY_NAMES ? category : ''}));
  }, [location.search]);

  const shown = useMemo(() => rules.filter((rule) => matches(rule, filter)), [rules, filter]);
  const categories = present(rules, 'category', Object.keys(CATEGORY_NAMES));
  const update = (change: Partial<Filter>) => setFilter((current) => ({...current, ...change}));

  return (
    <section className="bq-catalogue" aria-label="Rules">
      <div className="bq-catalogue__chips" role="group" aria-label="Category">
        {['', ...categories].map((category) => (
          <button
            key={category || 'all'}
            type="button"
            className="bq-pill"
            aria-pressed={filter.category === category}
            onClick={() => update({category})}>
            {category ? CATEGORY_NAMES[category] : 'All'}
            <span className="bq-pill__count">
              {category ? rules.filter((rule) => rule.category === category).length : rules.length}
            </span>
          </button>
        ))}
      </div>
      <div className="bq-catalogue__filters">
        <label className="bq-field bq-field--grow">
          <span className="bq-visually-hidden">Search rules</span>
          <input
            type="search"
            placeholder="Search by ID or title"
            value={filter.text}
            onChange={(event) => update({text: event.target.value})}
          />
        </label>
        <label className="bq-field">
          <span className="bq-visually-hidden">Severity</span>
          <select value={filter.severity} onChange={(event) => update({severity: event.target.value})}>
            <option value="">All severities</option>
            {present(rules, 'severity', [...SEVERITIES]).map((severity) => (
              <option key={severity} value={severity}>
                {SEVERITY_NAMES[severity]}
              </option>
            ))}
          </select>
        </label>
        <label className="bq-field">
          <span className="bq-visually-hidden">Reads</span>
          <select value={filter.reads} onChange={(event) => update({reads: event.target.value})}>
            <option value="">Any input</option>
            {present(rules, 'reads', Object.keys(READS_NAMES)).map((reads) => (
              <option key={reads} value={reads}>
                {READS_NAMES[reads]}
              </option>
            ))}
          </select>
        </label>
      </div>
      <p className="bq-muted bq-catalogue__count" aria-live="polite">
        Showing {shown.length} of {rules.length} rules
      </p>
      {shown.length === 0 ? (
        <p className="bq-muted">No rule matches these filters.</p>
      ) : (
        <ul className="bq-rule-list">
          {shown.map((rule) => (
            <li key={rule.id}>
              <Link className="bq-rule-row" to={`/rules/${rule.id}/`}>
                <code className="bq-rule-row__id">{rule.id}</code>
                <span className="bq-rule-row__title">{rule.title}</span>
                <span className="bq-rule-row__meta">
                  <span className={`bq-chip bq-chip--${rule.severity.toLowerCase()}`}>
                    {SEVERITY_NAMES[rule.severity]}
                  </span>
                  <span className="bq-muted">{CATEGORY_NAMES[rule.category]}</span>
                  <span className="bq-muted">{READS_NAMES[rule.reads]}</span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
