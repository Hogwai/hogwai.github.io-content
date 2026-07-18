package com.hogwai.jpaprojections.projection;

import org.springframework.beans.factory.annotation.Value;

/**
 * Open projection using {@code @Value} with a Spring bean reference.
 * <p>
 * The {@code genreLabel} field is computed by {@code ProjectionHelper.formatGenreLabel(target)}
 * where {@code target} is the backing Movie entity. This demonstrates the legitimate use case
 * for {@code @Value}: when the computation requires a Spring bean.
 * <p>
 * <strong>Caveat:</strong> This is an <em>open</em> projection. Spring Data cannot optimise the
 * SELECT because the SpEL expression could reference any property. The full Movie entity is
 * loaded. For simple property-derived computations, prefer a {@code default} method on a
 * closed interface (see {@link ActorNameView}).
 *
 * @see com.hogwai.jpaprojections.helper.ProjectionHelper
 */
public interface MovieWithLabelView {

    String getTitle();

    @Value("#{@projectionHelper.formatGenreLabel(target)}")
    String getGenreLabel();
}
