// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater
//
// Ejected from @docusaurus/theme-classic 3.10.2 (MIT, Copyright (c) Facebook, Inc. and its affiliates), with one
// addition: a rule page shows its facts card right under its title. When upgrading Docusaurus, compare this file
// with the new upstream DocItem/Content and keep the RuleFacts line.

import React, {type ReactNode} from 'react';
import clsx from 'clsx';
import {ThemeClassNames} from '@docusaurus/theme-common';
import {useDoc} from '@docusaurus/plugin-content-docs/client';
import Heading from '@theme/Heading';
import MDXContent from '@theme/MDXContent';
import type {Props} from '@theme/DocItem/Content';
import RuleFacts from '@site/src/components/RuleFacts';

function useSyntheticTitle(): string | null {
  const {metadata, frontMatter, contentTitle} = useDoc();
  const shouldRender = !frontMatter.hide_title && typeof contentTitle === 'undefined';
  if (!shouldRender) {
    return null;
  }
  return metadata.title;
}

export default function DocItemContent({children}: Props): ReactNode {
  const syntheticTitle = useSyntheticTitle();
  return (
    <div className={clsx(ThemeClassNames.docs.docMarkdown, 'markdown')}>
      {syntheticTitle && (
        <header>
          <Heading as="h1">{syntheticTitle}</Heading>
        </header>
      )}
      <RuleFacts />
      <MDXContent>{children}</MDXContent>
    </div>
  );
}
