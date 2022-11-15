package com.wugui.datax.admin.tool.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Date;

/**
 * @author junlaile
 * @version 1.0
 * @date 2022/11/14 13:10
 */
public class GainLinkWrite {

    private static DataSource beeDataSource(String dbDriver, String dbUrl, String dbUsername,
                                            String dbPassword) {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName(dbDriver);
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setIdleTimeout(10000);
        config.setMaximumPoolSize(1);
        return new HikariDataSource(config);
    }

    private static Connection connection(String dbDriver, String dbUrl, String dbUsername,
                                         String dbPassword) {
        DataSource dataSource = beeDataSource(dbDriver, dbUrl, dbUsername, dbPassword);
        try {
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(true);
            return connection;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取最大值
     */
    public static <T> T max(String dbDriver, String dbUrl, String dbUsername,
                            String dbPassword, String filedName, String tableName, Class<T> clazz) {
        try {
            Connection connection = connection(dbDriver, dbUrl, dbUsername, dbPassword);
            final String sql = "select max(%s) from %s ";
            String sqLq = String.format(sql, filedName, tableName);
            PreparedStatement ps = connection.prepareStatement(sqLq);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                return resultSet.getObject(1, clazz);
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }/**
     * 获取最大值
     */
    public static Timestamp maxTimestamp(String dbDriver, String dbUrl, String dbUsername,
                            String dbPassword, String filedName, String tableName) {
        try {
            Connection connection = connection(dbDriver, dbUrl, dbUsername, dbPassword);
            final String sql = "select max(%s) from %s ";
            String sqLq = String.format(sql, filedName, tableName);
            PreparedStatement ps = connection.prepareStatement(sqLq);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                return resultSet.getTimestamp(1);
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
