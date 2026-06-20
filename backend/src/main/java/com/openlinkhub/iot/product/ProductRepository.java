package com.openlinkhub.iot.product;

import com.openlinkhub.iot.common.PageResult;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepository {

    private final JdbcClient jdbc;

    public ProductRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Product> findAll() {
        return jdbc.sql("""
                SELECT id, name, code, category, description, created_at, updated_at
                FROM olh_product
                ORDER BY id DESC
                """)
                .query(this::mapProduct)
                .list();
    }

    public PageResult<Product> findPage(String keyword, String category, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = (safePage - 1) * safeSize;
        String keywordFilter = keyword == null || keyword.isBlank()
                ? ""
                : " AND (name ILIKE :keyword OR code ILIKE :keyword OR description ILIKE :keyword)\n";
        String categoryFilter = category == null || category.isBlank() ? "" : " AND category = :category\n";

        JdbcClient.StatementSpec countSpec = jdbc.sql("""
                SELECT COUNT(*)
                FROM olh_product
                WHERE TRUE
                """ + keywordFilter + categoryFilter);
        JdbcClient.StatementSpec dataSpec = jdbc.sql("""
                SELECT id, name, code, category, description, created_at, updated_at
                FROM olh_product
                WHERE TRUE
                """ + keywordFilter + categoryFilter + """
                ORDER BY id DESC
                LIMIT :limit OFFSET :offset
                """)
                .param("limit", safeSize)
                .param("offset", offset);

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword + "%";
            countSpec = countSpec.param("keyword", like);
            dataSpec = dataSpec.param("keyword", like);
        }
        if (category != null && !category.isBlank()) {
            countSpec = countSpec.param("category", category);
            dataSpec = dataSpec.param("category", category);
        }

        return new PageResult<>(
                dataSpec.query(this::mapProduct).list(),
                countSpec.query(Long.class).single(),
                safePage,
                safeSize
        );
    }

    public Optional<Product> findById(Long id) {
        return jdbc.sql("""
                SELECT id, name, code, category, description, created_at, updated_at
                FROM olh_product
                WHERE id = :id
                """)
                .param("id", id)
                .query(this::mapProduct)
                .optional();
    }

    public Product create(ProductRequest request) {
        return jdbc.sql("""
                INSERT INTO olh_product (name, code, category, description)
                VALUES (:name, :code, COALESCE(:category, 'general'), :description)
                RETURNING id, name, code, category, description, created_at, updated_at
                """)
                .param("name", request.name())
                .param("code", request.code())
                .param("category", request.category())
                .param("description", request.description())
                .query(this::mapProduct)
                .single();
    }

    public Product update(Long id, ProductRequest request) {
        return jdbc.sql("""
                UPDATE olh_product
                SET name = :name,
                    code = :code,
                    category = COALESCE(:category, 'general'),
                    description = :description,
                    updated_at = NOW()
                WHERE id = :id
                RETURNING id, name, code, category, description, created_at, updated_at
                """)
                .param("id", id)
                .param("name", request.name())
                .param("code", request.code())
                .param("category", request.category())
                .param("description", request.description())
                .query(this::mapProduct)
                .single();
    }

    private Product mapProduct(ResultSet rs, int rowNum) throws SQLException {
        return new Product(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("code"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }
}
