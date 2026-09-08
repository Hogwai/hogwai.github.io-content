package com.hogwai.rowtransformers.transformer;

/**
 * Projection interface - Spring Data generates a proxy class that
 * extracts title and pages from the ResultSet. The underlying RowTransformer
 * is SingularReturnImpl.
 */
public interface BookTitleProjection {
    String getTitle();
    int getPages();
}
