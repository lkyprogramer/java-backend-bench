package benchjudge;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

/**
 * Judge-owned JDBC instrumentation. Counts statement round trips and write-transaction commit
 * attempts, and injects one commit failure at the N-th write commit (BENCH_FAIL_APPEND_AT=N).
 * A "write commit" is a commit of a transaction that executed DML, or a DML statement executed
 * in auto-commit mode. Counting of commits starts once the application is ready, so schema
 * migration at startup does not shift N. The candidate application never sees the variable.
 */
public final class BenchJdbc {

    static final AtomicLong STATEMENTS = new AtomicLong();
    static final AtomicLong WRITE_COMMITS = new AtomicLong();
    static final AtomicBoolean ARMED = new AtomicBoolean();
    static final AtomicBoolean INJECTED = new AtomicBoolean();
    static final long FAIL_AT = parse(System.getenv("BENCH_FAIL_APPEND_AT"));

    private BenchJdbc() {
    }

    private static long parse(String value) {
        try {
            return value == null ? -1 : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static boolean isDml(String sql) {
        if (sql == null) {
            return false;
        }
        String s = sql.stripLeading().toLowerCase(Locale.ROOT);
        return s.startsWith("insert") || s.startsWith("update") || s.startsWith("delete") || s.startsWith("merge");
    }

    /** Called before a write commit attempt; throws on the configured attempt, once. */
    static void writeCommitAttempt() throws SQLException {
        if (!ARMED.get()) {
            return;
        }
        long n = WRITE_COMMITS.incrementAndGet();
        if (n == FAIL_AT && INJECTED.compareAndSet(false, true)) {
            throw new SQLException("bench: injected storage failure at write commit " + n, "08006");
        }
    }

    public static DataSource wrap(DataSource target) {
        return (DataSource) Proxy.newProxyInstance(BenchJdbc.class.getClassLoader(),
                new Class<?>[] {DataSource.class, Closeable.class}, (proxy, method, args) -> {
                    if (method.getName().equals("close") && method.getParameterCount() == 0) {
                        if (target instanceof AutoCloseable c) {
                            c.close();
                        }
                        return null;
                    }
                    Object result = invoke(target, method, args);
                    return result instanceof Connection c ? connection(c) : result;
                });
    }

    private static Connection connection(Connection target) {
        boolean[] dirty = {false};
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "commit" -> {
                    if (dirty[0]) {
                        writeCommitAttempt();
                    }
                    Object r = invoke(target, method, args);
                    dirty[0] = false;
                    return r;
                }
                case "rollback" -> {
                    Object r = invoke(target, method, args);
                    if (args == null || args.length == 0) {
                        dirty[0] = false;
                    }
                    return r;
                }
                case "createStatement", "prepareStatement", "prepareCall" -> {
                    Object r = invoke(target, method, args);
                    String sql = args != null && args.length > 0 && args[0] instanceof String s ? s : null;
                    return statement((Statement) r, target, dirty, sql);
                }
                default -> {
                    return invoke(target, method, args);
                }
            }
        };
        return (Connection) Proxy.newProxyInstance(BenchJdbc.class.getClassLoader(), new Class<?>[] {Connection.class}, handler);
    }

    private static Object statement(Statement target, Connection connection, boolean[] dirty, String preparedSql) {
        Class<?> type = target instanceof CallableStatement ? CallableStatement.class
                : target instanceof PreparedStatement ? PreparedStatement.class : Statement.class;
        boolean[] batchDml = {false};
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if (name.equals("addBatch")) {
                String sql = args != null && args.length == 1 && args[0] instanceof String s ? s : preparedSql;
                batchDml[0] |= isDml(sql);
                return invoke(target, method, args);
            }
            if (name.startsWith("execute")) {
                String sql = args != null && args.length > 0 && args[0] instanceof String s ? s : preparedSql;
                boolean dml = name.contains("Batch") ? batchDml[0] : isDml(sql);
                STATEMENTS.incrementAndGet();
                if (dml) {
                    if (connection.getAutoCommit()) {
                        writeCommitAttempt();
                    } else {
                        dirty[0] = true;
                    }
                }
                if (name.contains("Batch")) {
                    batchDml[0] = false;
                }
            }
            return invoke(target, method, args);
        };
        return Proxy.newProxyInstance(BenchJdbc.class.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
