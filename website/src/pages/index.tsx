// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

import React, {type ReactNode} from 'react';
import Link from '@docusaurus/Link';
import Layout from '@theme/Layout';
import ThemedImage from '@theme/ThemedImage';
import useBaseUrl from '@docusaurus/useBaseUrl';
import {usePluginData} from '@docusaurus/useGlobalData';
import dashboardLight from '../../../docs/assets/panel-light.png';
import dashboardDark from '../../../docs/assets/panel-dark.png';
import {CATEGORY_NAMES, type Rule} from '@site/src/components/ruleLabels';

const PROMISES = [
  {
    title: 'One dependency',
    text: 'Add the starter to a Spring Boot 4.1 application. There is nothing to configure, and nothing to run next to it.',
  },
  {
    title: 'Findings where you work',
    text: 'A panel beside your operations in Swagger UI shows the score, the grade and every finding, with a Spring fix for each.',
  },
  {
    title: 'Safe by default',
    text: 'Only in the dev and local profiles. No outbound calls, no telemetry, and your SecurityFilterChain is never touched.',
  },
];

function Hero(): ReactNode {
  return (
    <header className="bq-hero container">
      <div className="bq-hero__grid">
        <div>
          <p className="bq-eyebrow">Brekquater · API starter</p>
          <h1 className="bq-hero__title">
            Better API design,
            <br /> right inside Swagger UI.
          </h1>
          <p className="bq-hero__sub">
            Add one dependency to your Spring Boot application, open Swagger UI, and see what to improve in your API
            design, with a maturity score. It warns; it never blocks, unless you ask it to.
          </p>
          <div className="bq-hero__actions">
            <Link className="button button--primary button--lg" to="/getting-started/">
              Get started
            </Link>
            <Link className="button button--outline button--secondary button--lg" to="/rules/">
              Browse the rules
            </Link>
          </div>
          <p className="bq-hero__proof">Apache-2.0 · On Maven Central · Spring Boot 4.1 · Java 17, 21 and 25</p>
        </div>
        <ThemedImage
          className="bq-hero__logo"
          alt=""
          width={720}
          height={513}
          sources={{
            light: useBaseUrl('/img/logo-stacked.webp'),
            dark: useBaseUrl('/img/logo-stacked-ivory.webp'),
          }}
        />
      </div>
    </header>
  );
}

function Promises(): ReactNode {
  return (
    <section className="bq-section container" aria-labelledby="promises">
      <p className="bq-eyebrow" id="promises">
        Why teams use it
      </p>
      <div className="bq-cards">
        {PROMISES.map((promise) => (
          <div className="bq-card" key={promise.title}>
            <h3>{promise.title}</h3>
            <p>{promise.text}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

function Screenshot(): ReactNode {
  return (
    <section className="bq-section container" aria-labelledby="see-it">
      <p className="bq-eyebrow">See it</p>
      <h2 className="bq-section__title" id="see-it">
        Every finding, its fix, and a score
      </h2>
      <ThemedImage
        className="bq-shot"
        sources={{light: dashboardLight, dark: dashboardDark}}
        alt="The Brekquater dashboard: operations with their finding counts, the findings with filters, and a score card with category scores and build evidence"
        loading="lazy"
      />
    </section>
  );
}

function RulesByCategory(): ReactNode {
  const {rules} = usePluginData('rule-catalogue') as {rules: Rule[]};
  const categories = Object.keys(CATEGORY_NAMES).filter((category) => rules.some((rule) => rule.category === category));
  return (
    <section className="bq-section container" aria-labelledby="rules">
      <p className="bq-eyebrow">The rules</p>
      <h2 className="bq-section__title" id="rules">
        {rules.length} rules, each with a page that explains it
      </h2>
      <div className="bq-cards">
        {categories.map((category) => (
          <Link className="bq-card" key={category} to={`/rules/?category=${category}`}>
            <span className="bq-card__count">{rules.filter((rule) => rule.category === category).length}</span>
            <h3>{CATEGORY_NAMES[category]}</h3>
          </Link>
        ))}
      </div>
    </section>
  );
}

export default function Home(): ReactNode {
  return (
    <Layout
      title="API design feedback inside Swagger UI"
      description="A Spring Boot starter that shows API design findings and a maturity score inside Swagger UI.">
      <main>
        <Hero />
        <Promises />
        <Screenshot />
        <RulesByCategory />
      </main>
    </Layout>
  );
}
