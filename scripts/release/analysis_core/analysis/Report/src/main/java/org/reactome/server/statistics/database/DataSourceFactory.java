package org.reactome.server.statistics.database;


import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class DataSourceFactory {
    private static Logger logger = Logger.getLogger(DataSourceFactory.class.getName());

    private static String databaseDriverClass;
    private static String connectionURL;

    public static BasicDataSource getDatabaseConnection() {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(databaseDriverClass);
        basicDataSource.setUrl(connectionURL);
        basicDataSource.setUsername("");
        basicDataSource.setPassword("");
        basicDataSource.setMaxActive(5);
        return basicDataSource;
    }

    public void setDatabaseDriverClass(String databaseDriverClass) {
        DataSourceFactory.databaseDriverClass = databaseDriverClass;
    }

    public void setConnectionURL(String connectionURL) {
        DataSourceFactory.connectionURL = connectionURL;
    }
}