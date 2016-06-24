package querydsl;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.annotation.Nullable;
import javax.inject.Provider;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Constant;
import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.Visitor;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.spatial.PostGISTemplates;

import querydslGenerated.QTesttable;

/**
 *
 * @author scf
 */
public class SubstringTest {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SubstringTest.class);
    private String url;
    private Properties properties;
    private Connection connection;
    private Provider<Connection> connectionProvider;

    public static class ConstantNumberExpression<N extends Number & Comparable<?>> extends NumberExpression<N> {

        private static final long serialVersionUID = 1L;

        public ConstantNumberExpression(final N constant) {
            super(ConstantImpl.create(constant));
        }

        @Override
        @Nullable
        public <R, C> R accept(final Visitor<R, C> v, @Nullable final C context) {
            return v.visit((Constant<N>) mixin, context);
        }

    }

    public SQLQueryFactory createQueryFactory() {
        SQLTemplates templates = PostGISTemplates.builder().quote().build();
        return new SQLQueryFactory(templates, connectionProvider);
    }

    public SubstringTest() {
    }

    @Before
    public void setUp() throws SQLException {
        Properties dbProps = new Properties();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream input = classLoader.getResourceAsStream("db.properties");

            dbProps.load(input);
            Class.forName("org.postgresql.Driver");

            properties = new Properties();
            properties.setProperty("user", dbProps.getProperty("username"));
            properties.setProperty("password", dbProps.getProperty("password"));
            url = dbProps.getProperty("url");

        } catch (IOException | ClassNotFoundException ex) {
            LOGGER.error("Problem reading database connection properties file.", ex);
        }
        connection = DriverManager.getConnection(url, properties);
        connectionProvider = new Provider<Connection>() {
            final Connection c = connection;

            @Override
            public Connection get() {
                return c;
            }
        };
    }

    @Test
    public void testSubstring() {
        QTesttable qt = QTesttable.testtable;

        ConstantNumberExpression<Integer> n1 = new ConstantNumberExpression<>(Integer.valueOf(3));
        ConstantNumberExpression<Integer> n2 = new ConstantNumberExpression<>(Integer.valueOf(6));
        StringExpression s1 = qt.description;
        StringExpression substring = s1.substring(n1, n2);

        SQLQuery<Tuple> query = createQueryFactory()
                .select(qt.id, qt.description)
                .from(qt)
                .where(substring.eq("con"));
        Tuple first;
        try {
            first = query.fetchFirst();
        } catch (Exception e) {
            LOGGER.error("Exception.", e);
            throw e;
        }
        Long id = first.get(qt.id);
        String description = first.get(qt.description);
        LOGGER.info("query: {}", query.getSQL().getSQL());
        LOGGER.info("first result: {}, {}", id, description);
        assert (description.substring(2, 3).equalsIgnoreCase("con"));
    }
}
