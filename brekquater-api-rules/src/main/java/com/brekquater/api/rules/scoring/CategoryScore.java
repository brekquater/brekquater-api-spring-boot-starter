// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.scoring;

import com.brekquater.api.rules.Category;
import java.util.OptionalInt;

/**
 * How one category did.
 *
 * @param category the category
 * @param assessed whether at least one rule of the category was evaluated
 * @param score the weighted pass rate from 0 to 100; empty when no scored rule had anything to check
 */
public record CategoryScore(Category category, boolean assessed, OptionalInt score) {}
