// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

// The bundler turns an imported image into its published URL.
declare module '*.png' {
  const url: string;
  export default url;
}
