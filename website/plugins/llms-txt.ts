// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater
//
// Writes llms.txt (a list of every page, as https://llmstxt.org describes) and llms-full.txt (every page's Markdown
// in one file) into the built site, so AI agents can read the docs without parsing HTML. Both are rebuilt from docs/
// on every build, so a new rule page is listed without editing anything here.

import fs from 'node:fs';
import path from 'node:path';
import matter from 'gray-matter';
import type {DocusaurusConfig, Plugin} from '@docusaurus/types';
import {ruleFiles} from './rules';

const DOCS_DIR = path.join(__dirname, '..', '..', 'docs');
const INTRO = path.join(__dirname, '..', 'llms-intro.md');

// The guides in reading order. The rule catalogue page is left out: it is a filter UI, and the rules are listed anyway.
const GUIDES = ['index.md', 'configuration.md', 'reports.md'];

type Page = {file: string; title: string; description: string; url: string; body: string};

export default function llmsTxtPlugin(): Plugin {
  return {
    name: 'llms-txt',
    async postBuild({siteConfig, outDir}) {
      const pages = readPages(siteConfig.url + siteConfig.baseUrl);
      fs.writeFileSync(path.join(outDir, 'llms.txt'), index(pages, siteConfig));
      fs.writeFileSync(path.join(outDir, 'llms-full.txt'), fullText(pages));
    },
  };
}

function readPages(siteUrl: string): Page[] {
  const files = [...GUIDES, ...ruleFiles().map((file) => `rules/${file}`)];
  return files.map((file) => readPage(file, siteUrl));
}

function readPage(file: string, siteUrl: string): Page {
  const {data, content} = matter(fs.readFileSync(path.join(DOCS_DIR, file), 'utf8'));
  const body = content.replace(/\r\n/g, '\n').trim();
  const route = String(data.slug ?? `/${file.replace(/\.md$/, '')}`).replace(/^\//, '');
  return {
    file,
    title: String(data.title ?? body.match(/^# (.+)$/m)?.[1] ?? file),
    description: String(data.description ?? firstParagraph(body)),
    url: `${siteUrl}${route}/`,
    body,
  };
}

function firstParagraph(body: string): string {
  return body.split('\n\n').find((block) => !block.startsWith('#')) ?? '';
}

function index(pages: Page[], siteConfig: DocusaurusConfig): string {
  const guides = pages.filter((page) => GUIDES.includes(page.file));
  const rules = pages.filter((page) => !GUIDES.includes(page.file));
  const repository = `https://github.com/${siteConfig.organizationName}/${siteConfig.projectName}`;
  return [
    fs.readFileSync(INTRO, 'utf8').replace(/\r\n/g, '\n').trim(),
    `## Docs\n\n${links(guides)}`,
    `## Rules\n\n${links(rules)}`,
    '## Optional\n\n' +
      `- [Every page in one file](${siteConfig.url}${siteConfig.baseUrl}llms-full.txt): the guides and all rule pages as Markdown\n` +
      `- [Source code](${repository})`,
  ].join('\n\n') + '\n';
}

function links(pages: Page[]): string {
  return pages.map((page) => `- [${page.title}](${page.url}): ${page.description}`).join('\n');
}

function fullText(pages: Page[]): string {
  const urls = new Map(pages.map((page) => [page.file, page.url]));
  return pages.map((page) => section(page, urls)).join('\n\n---\n\n') + '\n';
}

function section(page: Page, urls: Map<string, string>): string {
  const body = page.body
    .replace(/^# .+\n+/, '')
    // Screenshots mean nothing in plain text.
    .replace(/^!\[[^\]]*\]\([^)]*\)\n?/gm, '')
    .replace(/\]\(([^)#\s]+\.md)(#[^)\s]*)?\)/g, (link, target: string, anchor = '') => {
      const url = urls.get(path.posix.normalize(path.posix.join(path.posix.dirname(page.file), target)));
      return url ? `](${url}${anchor})` : link;
    });
  return `# ${page.title}\n\nSource: ${page.url}\n\n${body.trim()}`;
}
