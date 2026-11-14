// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.scoring;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The maturity score of one assessment. It is for visibility only and never fails a build or a startup.
 *
 * @param overall the mean of the category scores, from 0 to 100; empty when no category has a score
 * @param grade the letter for the overall score, or F while any blocker finding remains; empty when there is no
 *     overall score
 * @param categories one entry per category, in declaration order, including the ones that were not assessed
 */
public record Score(OptionalInt overall, Optional<Grade> grade, List<CategoryScore> categories) {

    public Score {
        categories = List.copyOf(categories);
    }

    public int assessedCategoryCount() {
        return (int) categories.stream().filter(CategoryScore::assessed).count();
    }
}
