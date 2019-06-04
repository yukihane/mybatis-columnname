package com.github.yukihane.mybatis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.StringTypeHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ColumnNameTest {

    private static SqlSessionFactory sqlSessionFactory;

    @BeforeAll
    public static void init() throws Exception {
        final Reader config = Resources.getResourceAsReader("mybatis-config.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(config);
        config.close();

        final SqlSession session = sqlSessionFactory.openSession();
        final Connection conn = session.getConnection();
        final Reader createtable = Resources.getResourceAsReader("createtable.sql");
        final ScriptRunner runner = new ScriptRunner(conn);
        runner.setLogWriter(null);
        runner.runScript(createtable);
        conn.close();
        createtable.close();
        session.close();
    }

    @Test
    public void fetchMetadata() {
        try (final SqlSession sqlSession = sqlSessionFactory.openSession()) {
            sqlSession.getMapper(TestMapper.class).select();
        }
    }

    @MappedJdbcTypes({ JdbcType.VARCHAR })
    public static class TestStringTypeHandler extends StringTypeHandler {
        @Override
        public String getNullableResult(final ResultSet rs, final String columnName)
            throws SQLException {

            final ResultSetMetaData md = rs.getMetaData();
            final String actualLabel = md.getColumnLabel(1);
            final String actualName = md.getColumnName(1);

            assertThat(actualName).isEqualToIgnoringCase("column_name");
            assertThat(actualLabel).isEqualToIgnoringCase("alias_label");

            // Argument columnName is not equal to actual column name.
            assertThat(columnName).isEqualToIgnoringCase(actualName);

            return super.getNullableResult(rs, columnName);
        }
    }

    @Mapper
    interface TestMapper {
        @Select("select column_name as alias_label from mytable")
        public Collection<String> select();
    }
}
