package com.bantanger.jpa.support;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBaseJpaAggregate is a Querydsl query type for BaseJpaAggregate
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QBaseJpaAggregate extends EntityPathBase<BaseJpaAggregate> {

    private static final long serialVersionUID = -821488832L;

    public static final QBaseJpaAggregate baseJpaAggregate = new QBaseJpaAggregate("baseJpaAggregate");

    public final DateTimePath<java.time.Instant> createdAt = createDateTime("createdAt", java.time.Instant.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.Instant> updatedAt = createDateTime("updatedAt", java.time.Instant.class);

    public final NumberPath<Integer> version = createNumber("version", Integer.class);

    public QBaseJpaAggregate(String variable) {
        super(BaseJpaAggregate.class, forVariable(variable));
    }

    public QBaseJpaAggregate(Path<? extends BaseJpaAggregate> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBaseJpaAggregate(PathMetadata metadata) {
        super(BaseJpaAggregate.class, metadata);
    }

}

