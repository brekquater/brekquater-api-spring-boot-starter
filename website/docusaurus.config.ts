// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import ruleCataloguePlugin from './plugins/rule-catalogue';

const REPOSITORY = 'https://github.com/brekquater/brekquater-api-spring-boot-starter';

const config: Config = {
  title: 'Brekquater API Starter',
  tagline: 'API design feedback and a maturity score, inside Swagger UI.',
  favicon: 'img/favicon.ico',

  future: {
    v4: true,
  },

  // Every finding links to https://starter.brekquater.com/api/rules/<RULE-ID>, and released jars keep those links
  // forever. Never change url, baseUrl, trailingSlash or a rule page's slug.
  url: 'https://starter.brekquater.com',
  baseUrl: '/api/',
  trailingSlash: true,

  organizationName: 'brekquater',
  projectName: 'brekquater-api-spring-boot-starter',

  onBrokenLinks: 'throw',
  onBrokenAnchors: 'throw',

  // .md files are plain Markdown, so a rule page cannot break the build with a stray { or <. Only .mdx files
  // (the rule catalogue) can use components.
  markdown: {
    format: 'detect',
    hooks: {
      onBrokenMarkdownLinks: 'throw',
    },
  },

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          path: '../docs',
          routeBasePath: '/',
          sidebarPath: './sidebars.ts',
          editUrl: ({docPath}) => `${REPOSITORY}/edit/master/docs/${docPath}`,
          // The default exclusions, plus planning notes that are kept locally and never published.
          exclude: [
            '**/_*.{js,jsx,ts,tsx,md,mdx}',
            '**/_*/**',
            '**/*.test.{js,jsx,ts,tsx}',
            '**/__tests__/**',
            'PRD.md',
            'DESIGN.md',
          ],
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  plugins: [ruleCataloguePlugin],

  themes: [
    // Search runs in the browser on an index built with the site: no external service, nothing sent anywhere.
    [
      '@easyops-cn/docusaurus-search-local',
      {
        hashed: true,
        indexBlog: false,
        indexPages: true,
        docsRouteBasePath: '/',
        docsDir: '../docs',
        language: ['en'],
        highlightSearchTermsOnTargetPage: true,
        searchResultLimits: 10,
        searchBarShortcutHint: true,
      },
    ],
  ],

  themeConfig: {
    image: 'img/og-image.png',
    colorMode: {
      respectPrefersColorScheme: true,
    },
    docs: {
      sidebar: {
        hideable: true,
        autoCollapseCategories: true,
      },
    },
    navbar: {
      title: 'API Starter',
      logo: {
        alt: 'Brekquater',
        src: 'img/logo-symbol.webp',
        srcDark: 'img/logo-symbol-ivory.webp',
        width: 32,
        height: 28,
      },
      items: [
        {type: 'doc', docId: 'index', label: 'Docs', position: 'left'},
        {to: '/rules/', label: 'Rules', position: 'left'},
        {to: '/configuration/', label: 'Configuration', position: 'left'},
        {href: REPOSITORY, label: 'GitHub', position: 'right'},
      ],
    },
    footer: {
      links: [
        {
          title: 'Docs',
          items: [
            {label: 'Getting started', to: '/getting-started/'},
            {label: 'Rule catalogue', to: '/rules/'},
            {label: 'Configuration', to: '/configuration/'},
            {label: 'Build reports', to: '/reports/'},
          ],
        },
        {
          title: 'Project',
          items: [
            {label: 'GitHub', href: REPOSITORY},
            {label: 'Changelog', href: `${REPOSITORY}/blob/master/CHANGELOG.md`},
            {label: 'Contributing', href: `${REPOSITORY}/blob/master/CONTRIBUTING.md`},
            {label: 'Security', href: `${REPOSITORY}/blob/master/SECURITY.md`},
          ],
        },
        {
          title: 'Brekquater',
          items: [{label: 'brekquater.com', href: 'https://brekquater.com'}],
        },
      ],
      copyright: `Code licensed under Apache-2.0. The Brekquater name and logo are trademarks of Brekquater.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.vsDark,
      additionalLanguages: ['java', 'bash', 'properties'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
